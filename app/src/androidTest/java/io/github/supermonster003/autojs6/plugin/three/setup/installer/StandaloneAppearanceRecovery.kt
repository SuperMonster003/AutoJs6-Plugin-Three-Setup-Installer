package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Parcel
import android.os.Process
import android.util.AtomicFile
import android.util.Base64
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIconStatePolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIcons
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Durable test-owned recovery journal, written and fsynced before any preference or alias can change. */
internal object StandaloneAppearanceRecovery {
    private const val RELATIVE_PATH = "p5-standalone-appearance/restore-plan.json"
    private const val PREFERENCE_FILE = "app-appearance"
    private const val MAX_BYTES = 8 * 1024 * 1024

    data class Plan(val runId: String, val internalPath: String, val readableCopy: String?)

    fun begin(context: Context): Plan {
        val file = File(context.filesDir, RELATIVE_PATH)
        if (file.exists() || File(file.path + ".bak").exists()) {
            val prior = read(context)
            check(prior.get("status").asString == "restored") {
                "An earlier display test has an unresolved restore-plan.json. Run standaloneAppearanceRestoreOnly first; the saved baseline will not be overwritten."
            }
        }
        val shortcuts = mutableShortcuts(context)
        if (Build.VERSION.SDK_INT >= 28) {
            val affectedActivities = LauncherIconMode.entries.map { it.component(context) }.toSet() +
                android.content.ComponentName(context.packageName, "${context.packageName}.ui.HomeActivity")
            check(shortcuts.none { !it.isEnabled && it.disabledReason == ShortcutInfo.DISABLED_REASON_APP_CHANGED && it.activity in affectedActivities }) {
                "Resolve pending APP_CHANGED shortcut migration before the display audit; its system disabled reason cannot be restored by an app."
            }
        }
        val runId = UUID.randomUUID().toString()
        val plan = JsonObject().apply {
            addProperty("format", 1)
            addProperty("runId", runId)
            addProperty("status", "pending")
            addProperty("packageName", context.packageName)
            addProperty("uid", Process.myUid())
            addProperty("sdk", Build.VERSION.SDK_INT)
            addProperty("fingerprint", Build.FINGERPRINT)
            addProperty("createdAtMillis", System.currentTimeMillis())
            addProperty("preferenceFile", PREFERENCE_FILE)
            add("appearance", encodeValues(context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE).all))
            add("aliases", JsonObject().apply {
                LauncherIconMode.entries.forEach { mode -> addProperty(mode.name, context.packageManager.getComponentEnabledSetting(mode.component(context))) }
            })
            add("mutableShortcuts", JsonArray().apply {
                if (Build.VERSION.SDK_INT >= 25) shortcuts.forEach { shortcut ->
                    val parcel = Parcel.obtain()
                    try {
                        shortcut.writeToParcel(parcel, 0)
                        check(!parcel.hasFileDescriptors()) { "A shortcut contains a non-persistable descriptor" }
                        add(Base64.encodeToString(parcel.marshall(), Base64.NO_WRAP))
                    } finally { parcel.recycle() }
                }
            })
        }
        write(context, plan)
        return location(context, runId)
    }

    /** Safe after a native process crash. Missing or inconsistent plans fail without inventing defaults. */
    fun restore(context: Context, expectedRunId: String? = null, drainPendingLauncherWork: Boolean = false): Plan {
        val plan = read(context)
        val runId = plan.get("runId").asString
        check(expectedRunId == null || expectedRunId == runId) { "The saved recovery baseline belongs to another test run" }
        check(plan.get("status").asString in setOf("pending", "restored"))
        // A completed journal must not overwrite preferences the user changed after the audit.
        if (plan.get("status").asString == "restored") {
            check(expectedRunId == null) { "This test's recovery journal was already completed by another operation" }
            return location(context, runId)
        }
        val values = decodeValues(plan.getAsJsonObject("appearance"))
        val aliases = plan.getAsJsonObject("aliases")
        check(aliases.keySet() == LauncherIconMode.entries.map { it.name }.toSet())
        val states = LauncherIconMode.entries.associateWith { mode -> aliases.get(mode.name).asInt.also { check(it in 0..4) } }
        val shortcuts = if (Build.VERSION.SDK_INT >= 25) plan.getAsJsonArray("mutableShortcuts").map { encoded ->
            val bytes = Base64.decode(encoded.asString, Base64.NO_WRAP)
            val parcel = Parcel.obtain()
            try {
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                ShortcutInfo.CREATOR.createFromParcel(parcel).also { check(it.`package` == context.packageName) }
            } finally { parcel.recycle() }
        } else emptyList()

        if (drainPendingLauncherWork) {
            val complete = CountDownLatch(1)
            LauncherIcons.normalizeAsync(context) { complete.countDown() }
            check(complete.await(10, TimeUnit.SECONDS)) { "Launcher repair did not drain; the recovery plan remains pending" }
        }
        val preferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
        val editor = preferences.edit().clear()
        for ((key, value) in values) when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> error("Unsupported saved preference type")
        }
        check(editor.commit()) { "Unable to persist restored appearance; the plan remains pending" }
        states.entries.sortedBy { if (LauncherIconStatePolicy.enabled(it.key, it.value)) 0 else 1 }.forEach { (mode, state) ->
            context.packageManager.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
        }
        if (Build.VERSION.SDK_INT >= 25) {
            val manager = context.getSystemService(ShortcutManager::class.java)
            val current = (manager.dynamicShortcuts + manager.pinnedShortcuts).associateBy { it.id }
            val changed = shortcuts.filter { old -> current[old.id]?.let { !sameOwnership(old, it) } == true }
            if (changed.isNotEmpty()) check(manager.updateShortcuts(changed)) { "Shortcut restoration failed; the plan remains pending" }
            val after = (manager.dynamicShortcuts + manager.pinnedShortcuts).associateBy { it.id }
            // updateShortcuts cannot recreate entries removed by Android before recovery runs.
            check(shortcuts.all { old -> after[old.id]?.let { sameOwnership(old, it) && old.isEnabled == it.isEnabled } == true }) {
                "A saved shortcut is missing or its state differs. The plan remains pending for explicit review."
            }
        }
        check(values == preferences.all) { "The restored preference keys or types differ from the saved plan" }
        check(states.all { (mode, state) -> context.packageManager.getComponentEnabledSetting(mode.component(context)) == state }) {
            "The restored launcher component states differ from the saved plan"
        }
        plan.addProperty("status", "restored")
        plan.addProperty("restoredAtMillis", System.currentTimeMillis())
        write(context, plan)
        return location(context, runId)
    }

    private fun read(context: Context): JsonObject {
        val file = File(context.filesDir, RELATIVE_PATH)
        check(file.exists() || File(file.path + ".bak").exists()) {
            "No saved appearance recovery plan exists. The prior values cannot be inferred from test screenshots or current preferences."
        }
        val bytes = AtomicFile(file).openRead().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size() + count <= MAX_BYTES)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        return JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject.also {
            check(it.get("format").asInt == 1 && it.get("packageName").asString == context.packageName)
            check(it.get("uid").asInt == Process.myUid() && it.get("sdk").asInt == Build.VERSION.SDK_INT)
            check(it.get("fingerprint").asString == Build.FINGERPRINT)
            check(it.get("preferenceFile").asString == PREFERENCE_FILE)
        }
    }

    private fun write(context: Context, plan: JsonObject) {
        val bytes = GsonBuilder().setPrettyPrinting().create().toJson(plan).toByteArray(Charsets.UTF_8)
        check(bytes.size <= MAX_BYTES)
        fun atomicWrite(file: File) {
            check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try { output.write(bytes); output.fd.sync(); atomic.finishWrite(output) }
            catch (failure: Throwable) { atomic.failWrite(output); throw failure }
        }
        atomicWrite(File(context.filesDir, RELATIVE_PATH))
        // Readable evidence copy for adb pull. Restoration trusts only the private app-files copy.
        context.getExternalFilesDir(null)?.let { atomicWrite(File(it, RELATIVE_PATH)) }
    }

    private fun location(context: Context, runId: String) = Plan(runId, File(context.filesDir, RELATIVE_PATH).absolutePath,
        context.getExternalFilesDir(null)?.let { File(it, RELATIVE_PATH).absolutePath })

    private fun mutableShortcuts(context: Context): List<ShortcutInfo> = if (Build.VERSION.SDK_INT >= 25) {
        context.getSystemService(ShortcutManager::class.java).let { manager ->
            (manager.dynamicShortcuts + manager.pinnedShortcuts).distinctBy { it.id }
                .filterNot { it.isDeclaredInManifest || Build.VERSION.SDK_INT >= 30 && it.isImmutable }
        }
    } else emptyList()

    @androidx.annotation.RequiresApi(25)
    private fun sameOwnership(first: ShortcutInfo, second: ShortcutInfo): Boolean {
        val originals = first.intents.orEmpty()
        val current = second.intents.orEmpty()
        return first.activity == second.activity && originals.size == current.size && originals.zip(current).all { (a, b) -> a.filterEquals(b) }
    }

    private fun encodeValues(values: Map<String, *>): JsonObject = JsonObject().apply {
        for ((key, value) in values) add(key, JsonObject().apply {
            when (value) {
                is String -> { addProperty("type", "string"); addProperty("value", value) }
                is Boolean -> { addProperty("type", "boolean"); addProperty("value", value) }
                is Int -> { addProperty("type", "int"); addProperty("value", value.toString()) }
                is Long -> { addProperty("type", "long"); addProperty("value", value.toString()) }
                is Float -> { addProperty("type", "float"); addProperty("value", value.toString()) }
                is Set<*> -> {
                    check(value.all { it is String })
                    addProperty("type", "stringSet")
                    add("value", JsonArray().apply { value.filterIsInstance<String>().sorted().forEach(::add) })
                }
                else -> error("Unsupported preference type for $key")
            }
        })
    }

    private fun decodeValues(values: JsonObject): Map<String, Any> = values.entrySet().associate { (key, entry) ->
        val saved = entry.asJsonObject
        val value = saved.get("value")
        key to when (saved.get("type").asString) {
            "string" -> value.asString
            "boolean" -> value.asBoolean
            "int" -> value.asString.toInt()
            "long" -> value.asString.toLong()
            "float" -> value.asString.toFloat()
            "stringSet" -> value.asJsonArray.map { it.asString }.toSet()
            else -> error("Unsupported preference type in recovery plan")
        }
    }
}
