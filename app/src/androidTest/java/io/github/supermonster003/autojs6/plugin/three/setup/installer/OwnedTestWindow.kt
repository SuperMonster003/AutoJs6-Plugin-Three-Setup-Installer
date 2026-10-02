package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Finish only the Activity owned by this scenario before AndroidX can launch its EmptyActivity. */
internal fun <A : Activity, T> ActivityScenario<A>.useOwnedWindow(
    label: String,
    beforeFinish: (A) -> Unit = {},
    action: (ActivityScenario<A>) -> T,
): T {
    var failure: Throwable? = null
    try {
        return action(this)
    } catch (error: Throwable) {
        failure = error
        UiTestFailures.capture(label, error)
        throw error
    } finally {
        var cleanupFailure: Throwable? = null
        fun clean(block: () -> Unit) {
            try { block() } catch (error: Throwable) {
                UiTestFailures.capture("$label-cleanup", error)
                val primary = failure ?: cleanupFailure
                if (primary == null) cleanupFailure = error else if (primary !== error) primary.addSuppressed(error)
            }
        }
        clean {
            if (state != Lifecycle.State.DESTROYED) {
                var entered = false
                try {
                    onActivity { activity ->
                        entered = true
                        try { beforeFinish(activity) } finally { activity.finish() }
                    }
                } catch (error: IllegalStateException) {
                    // A completion callback can finish the same Activity between the state read
                    // and onActivity. It does not require another synthetic transition.
                    if (entered || state != Lifecycle.State.DESTROYED) throw error
                }
            }
        }
        clean {
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (state != Lifecycle.State.DESTROYED) {
                check(SystemClock.elapsedRealtime() < deadline) { "The owned test Activity was not destroyed: $label" }
                SystemClock.sleep(25)
            }
        }
        clean { close() }
        if (failure == null) cleanupFailure?.let { throw it }
    }
}

/** Restoration must still run after an assertion, without replacing the assertion's stack trace. */
internal fun preserveTestFailure(failure: Throwable?, cleanup: () -> Unit) {
    try { cleanup() } catch (error: Throwable) {
        if (failure == null) throw error
        if (failure !== error) failure.addSuppressed(error)
    }
}

internal fun View.readyForWindowInput(): Boolean = isAttachedToWindow && isLaidOut &&
    width > 0 && height > 0 && !isLayoutRequested && hasWindowFocus()

/** Best-effort failure evidence only. Accessibility metadata deliberately excludes user text. */
internal object UiTestFailures {
    private val captured = WeakHashMap<Throwable, Boolean>()
    private val sequence = AtomicInteger()

    fun capture(label: String, failure: Throwable) {
        // Nothing in diagnostics, including directory creation or status reporting, can hide the
        // original assertion. Each independent artifact also survives failures in its neighbours.
        runCatching {
            if (synchronized(captured) { captured.put(failure, true) != null }) return
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val directory = File(context.cacheDir, "ui-test-failures").apply { check(isDirectory || mkdirs()) }
            val safe = label.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100).ifEmpty { "window" }
            val prefix = "$safe-${SystemClock.elapsedRealtime()}-${sequence.incrementAndGet()}"
            val automation = instrumentation.uiAutomation
            runCatching {
                val metadata = JsonObject().apply {
                    addProperty("failureClass", failure.javaClass.name)
                    failure.stackTrace.firstOrNull { it.className.contains("DeviceTest") }?.let { frame ->
                        addProperty("testClass", frame.className); addProperty("testMethod", frame.methodName)
                        addProperty("testLine", frame.lineNumber)
                    }
                    add("windows", JsonArray().apply {
                        automation.windows.forEach { window ->
                            add(JsonObject().apply {
                                addProperty("id", window.id); addProperty("type", window.type); addProperty("layer", window.layer)
                                addProperty("active", window.isActive); addProperty("focused", window.isFocused)
                                addProperty("bounds", Rect().also(window::getBoundsInScreen).toShortString())
                                runCatching { window.root?.let { root ->
                                    try {
                                        addProperty("rootPackage", root.packageName?.toString())
                                        addProperty("rootClass", root.className?.toString())
                                        addProperty("rootFocused", root.isFocused)
                                    } finally { @Suppress("DEPRECATION") root.recycle() }
                                } }
                            })
                            @Suppress("DEPRECATION") window.recycle()
                        }
                    })
                    automation.rootInActiveWindow?.let { root ->
                        try {
                            add("activeRoot", JsonObject().apply {
                                addProperty("package", root.packageName?.toString())
                                addProperty("class", root.className?.toString())
                                addProperty("windowId", root.windowId); addProperty("focused", root.isFocused)
                                addProperty("accessibilityFocused", root.isAccessibilityFocused)
                            })
                        } finally { @Suppress("DEPRECATION") root.recycle() }
                    }
                }
                File(directory, "$prefix-windows.json").writeText(metadata.toString())
            }
            runCatching {
                automation.takeScreenshot()?.let { screenshot ->
                    try { File(directory, "$prefix-screen.png").outputStream().use {
                        check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                    } } finally { screenshot.recycle() }
                }
            }
            for ((name, command) in listOf(
                "window" to "dumpsys -t 2 window displays",
                "activity" to "dumpsys -t 2 activity activities",
                "input-method" to "dumpsys -t 2 input_method",
            )) runCatching {
                ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { input ->
                    File(directory, "$prefix-$name.txt").outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var remaining = 512 * 1024
                        while (remaining > 0) {
                            val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                            if (count < 0) break
                            output.write(buffer, 0, count); remaining -= count
                        }
                    }
                }
            }
            runCatching { android.util.Log.e("UiTestFailures", "Failure artifacts: ${directory.absolutePath}/$prefix-*") }
        }
    }
}
