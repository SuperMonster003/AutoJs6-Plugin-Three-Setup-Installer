package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.AtomicFile
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallProfilesActivity
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Real controls; no installation. The dedicated fixture run owns and journals one preferences document. */
@RunWith(AndroidJUnit4::class)
class InstallProfilesDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun fieldProfileAndListDraftsCancelSafelyAndOnlyListSavePersists() = fixture { owner ->
        val profile = InstallProfile(name = "Draft fixture")
        owner.seed(listOf(profile))
        val before = owner.document()
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                click(activity, "profile-edit-${profile.id}")
                choose(activity, "profile-option-${C.FIELD_ALLOW_TEST_ONLY}", 1, confirm = false)
                assertEquals(before, owner.document())
                click(activity, "profile-option-${C.FIELD_ALLOW_TEST_ONLY}")
                assertTrue(dialog(activity).findViewWithTag<MaterialRadioButton>("choice-0").isChecked)
                dialog(activity).findViewWithTag<View>("settings-cancel").performClick()
                choose(activity, "profile-option-${C.FIELD_ALLOW_TEST_ONLY}", 1)
                click(activity, "profile-edit-confirm")
                assertEquals(before, owner.document())
                click(activity, "profiles-cancel")
            }
        }
        assertEquals(before, owner.document())
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                click(activity, "profile-edit-${profile.id}")
                choose(activity, "profile-option-${C.FIELD_ALLOW_TEST_ONLY}", 1)
                choose(activity, "profile-option-${C.FIELD_AUTHORIZER}", 1)
                choose(activity, "profile-option-${C.FIELD_INSTALLER}", 1)
                choose(activity, "profile-option-${C.FIELD_USER}", 1)
                assertEquals(before, owner.document())
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                click(activity, "profile-option-${C.FIELD_ALLOW_TEST_ONLY}")
                assertTrue(dialog(activity).findViewWithTag<MaterialRadioButton>("choice-1").isChecked)
                dialog(activity).findViewWithTag<View>("settings-cancel").performClick()
                click(activity, "profile-edit-confirm")
                assertEquals(before, owner.document())
                val changes = AtomicInteger()
                val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == DOCUMENT) changes.incrementAndGet() }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                try {
                    owner.acceptUiWrite { click(activity, "profiles-save") }
                    assertEquals(1, changes.get())
                    assertFalse(root(activity).findViewWithTag<View>("profiles-save").isEnabled)
                } finally { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
        }
        val saved = InstallProfilePreferences.read(context).profiles.single().overrides
        assertEquals(setOf(C.FIELD_ALLOW_TEST_ONLY, C.FIELD_AUTHORIZER, C.FIELD_INSTALLER, C.FIELD_USER), saved.keys)
        assertFalse(saved[C.FIELD_ALLOW_TEST_ONLY]!!.asBoolean)
        assertEquals(C.AUTHORIZER_AUTO, saved[C.FIELD_AUTHORIZER]!!.asString)
        assertEquals(JsonNull.INSTANCE, saved[C.FIELD_INSTALLER])
        assertEquals(C.USER_CURRENT, saved[C.FIELD_USER]!!.asString)
        evidence("threeDraftLevels=true cancelledUnchanged=true recreatedDraftRetained=true explicitFalseNullAutoCurrent=true writes=1")
    }

    @Test fun creationEnableOrderAndDeletionRemainDraftsUntilSaving() = fixture { owner ->
        val first = InstallProfile(name = "First fixture", source = C.SOURCE_HOST)
        val second = InstallProfile(name = "Second fixture", source = C.SOURCE_SCRIPT, packagePrefix = "com.example.")
        owner.seed(listOf(first, second))
        val before = owner.document()
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                click(activity, "profile-enabled-${first.id}")
                click(activity, "profile-up-${second.id}")
                click(activity, "profile-delete-${first.id}")
                assertEquals(before, owner.document())
                click(activity, "profiles-cancel")
            }
        }
        assertEquals(before, owner.document())
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                click(activity, "profile-enabled-${second.id}")
                click(activity, "profile-up-${second.id}")
                click(activity, "profile-delete-${first.id}")
                click(activity, "profiles-add")
                input(activity, "profile-name", "Created fixture")
                choose(activity, "profile-source", 3)
                input(activity, "profile-prefix", "com.example.new.")
                click(activity, "profile-edit-confirm")
                assertEquals(before, owner.document())
                owner.acceptUiWrite { click(activity, "profiles-save") }
            }
        }
        val values = InstallProfilePreferences.read(context).profiles
        assertEquals(2, values.size)
        assertEquals(second.id, values[0].id)
        assertFalse(values[0].enabled)
        assertEquals("Created fixture", values[1].name)
        assertEquals(C.SOURCE_EXTERNAL, values[1].source)
        assertEquals("com.example.new.", values[1].packagePrefix)
        assertTrue(values[1].overrides.keys.isEmpty())
        assertNotEquals(first.id, values[1].id)
        evidence("createEnableMoveDelete=true cancelUnchanged=true emptyOverrideProfilePreserved=true")
    }

    @Test fun concurrentSaveCannotOverwriteAndReloadExplicitlyDiscardsTheDraft() = fixture { owner ->
        val profile = InstallProfile(name = "Original fixture")
        owner.seed(listOf(profile))
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                click(activity, "profile-enabled-${profile.id}")
                owner.seed(listOf(profile.copy(name = "Concurrent fixture")))
                val concurrent = owner.document()
                owner.acceptUiWrite { click(activity, "profiles-save") }
                assertEquals(concurrent, owner.document())
                assertFalse(root(activity).findViewWithTag<CompoundButton>("profile-enabled-${profile.id}").isChecked)
                assertTrue(texts(root(activity)).any { it == activity.getString(R.string.profile_conflict) })
                choose(activity, "profiles-reload", 0, confirm = false)
                assertFalse(root(activity).findViewWithTag<CompoundButton>("profile-enabled-${profile.id}").isChecked)
                choose(activity, "profiles-reload", 0)
                assertTrue(root(activity).findViewWithTag<CompoundButton>("profile-enabled-${profile.id}").isChecked)
                assertTrue(texts(root(activity)).any { it == "Concurrent fixture" })
                assertFalse(root(activity).findViewWithTag<View>("profiles-save").isEnabled)
                assertEquals(concurrent, owner.document())
            }
        }
        evidence("concurrentSaveRejected=true draftRetained=true reloadCancelRetained=true confirmedReloadUsesConcurrentValue=true")
    }

    @Test fun unreadableProfilesRequireResetConfirmationAndFinalSave() = fixture { owner ->
        owner.corrupt()
        val before = owner.document()
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(root(activity).findViewWithTag<View>("profiles-add").isEnabled)
                assertFalse(root(activity).findViewWithTag<View>("profiles-save").isEnabled)
                choose(activity, "profiles-reset", 0, confirm = false)
                assertFalse(root(activity).findViewWithTag<View>("profiles-save").isEnabled)
                assertEquals(before, owner.document())
                choose(activity, "profiles-reset", 0)
                assertTrue(root(activity).findViewWithTag<View>("profiles-save").isEnabled)
                assertEquals(before, owner.document())
                click(activity, "profiles-cancel")
            }
        }
        assertEquals(before, owner.document())
        ActivityScenario.launch(InstallProfilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                choose(activity, "profiles-reset", 0)
                owner.acceptUiWrite { click(activity, "profiles-save") }
            }
        }
        assertTrue(InstallProfilePreferences.read(context).readable)
        assertTrue(InstallProfilePreferences.read(context).profiles.isEmpty())
        evidence("unreadableAddBlocked=true resetCancelUnchanged=true resetDraftCancelUnchanged=true finalSaveResets=true")
    }

    private fun fixture(action: (Profiles) -> Unit) {
        assumeTrue(InstrumentationRegistry.getArguments().getString("profileUiFixtures") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish") && Process.myUid() / 100000 == 0) {
            "These settings fixtures are restricted to a task-owned emulator in user 0"
        }
        check(InstallPresentation.snapshots().none { !it.state.terminal })
        Profiles().use(action)
    }

    private fun root(activity: InstallProfilesActivity): View = activity.findViewById(android.R.id.content)
    private fun dialog(activity: InstallProfilesActivity): View = requireNotNull(
        InstallProfilesActivity::class.java.getDeclaredField("prompt").apply { isAccessible = true }.get(activity) as? Dialog
    ).window!!.decorView
    private fun click(activity: InstallProfilesActivity, tag: String) {
        val view = requireNotNull(root(activity).findViewWithTag<View>(tag)) { "Missing control $tag" }
        assertTrue("Disabled control $tag", view.isEnabled)
        val handled = view.performClick()
        assertTrue("No click handler for $tag", handled || view is CompoundButton)
    }
    private fun choose(activity: InstallProfilesActivity, tag: String, index: Int, confirm: Boolean = true) {
        click(activity, tag)
        dialog(activity).findViewWithTag<View>("choice-$index").performClick()
        dialog(activity).findViewWithTag<View>(if (confirm) "settings-confirm" else "settings-cancel").performClick()
    }
    private fun input(activity: InstallProfilesActivity, tag: String, value: String) {
        click(activity, tag)
        dialog(activity).findViewWithTag<EditText>("settings-input").setText(value)
        val confirm = dialog(activity).findViewWithTag<View>("settings-confirm")
        assertTrue(confirm.isEnabled)
        confirm.performClick()
    }
    private fun texts(view: View): Sequence<String> = sequence {
        if (view is TextView) yield(view.text.toString())
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(texts(view.getChildAt(index)))
    }
    private fun evidence(value: String) = instrumentation.sendStatus(0, Bundle().apply { putString("profileUiEvidence", value) })

    private inner class Profiles : Closeable {
        private val baseline = InstallProfilePreferences.read(context)
        private val original = document()
        private val existed = File(context.applicationInfo.dataDir, "shared_prefs/$FILE.xml").exists()
        private var owned = original
        private val runId = UUID.randomUUID().toString()
        private val journal = AtomicFile(File(context.filesDir, "p9-profile-ui/$runId.json"))

        init {
            check(baseline.readable) { "Do not replace a pre-existing unreadable profile document" }
            check(journal.baseFile.parentFile!!.isDirectory || journal.baseFile.parentFile!!.mkdirs())
            writeJournal("pending")
        }

        fun document(): String? {
            val values = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).all
            check(values.keys.all { it == DOCUMENT }) { "Unexpected profile preference keys" }
            return values[DOCUMENT]?.let { check(it is String); it }
        }

        fun seed(profiles: List<InstallProfile>) {
            check(document() == owned) { "Profiles changed outside this fixture" }
            assertEquals(InstallProfileSaveResult.SAVED, InstallProfilePreferences.save(context, InstallProfilePreferences.read(context), profiles))
            owned = document(); writeJournal("pending")
        }

        fun corrupt() {
            check(document() == owned)
            owned = "{fixture:$runId"
            writeJournal("pending")
            check(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(DOCUMENT, owned).commit())
            check(document() == owned)
        }

        fun acceptUiWrite(action: () -> Unit) {
            check(document() == owned) { "Profiles changed before the UI action" }
            action()
            owned = document(); writeJournal("pending")
        }

        override fun close() {
            check(document() == owned) { "Profiles changed after the fixture action; retain the journal for explicit recovery" }
            if (!existed) check(context.deleteSharedPreferences(FILE))
            else check(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().apply {
                if (original == null) remove(DOCUMENT) else putString(DOCUMENT, original)
            }.commit())
            check(document() == original)
            check(InstallProfilePreferences.read(context) == baseline)
            writeJournal("restored")
            evidence("profilesBaselineRestored=true journal=${journal.baseFile.name}")
        }

        private fun writeJournal(status: String) {
            val json = JsonObject().apply {
                addProperty("format", 1); addProperty("runId", runId); addProperty("uid", Process.myUid())
                addProperty("fingerprint", Build.FINGERPRINT); addProperty("status", status)
                addProperty("preferenceFileExisted", existed)
                addProperty("originalDocument", original); addProperty("ownedDocument", owned)
            }
            val stream = journal.startWrite()
            try { stream.write(json.toString().toByteArray(Charsets.UTF_8)); journal.finishWrite(stream) }
            catch (failure: Throwable) { journal.failWrite(stream); throw failure }
        }
    }

    companion object {
        private const val FILE = "installation_profiles"
        private const val DOCUMENT = "document"
    }
}
