package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import java.util.concurrent.atomic.AtomicReference

/** Publish the actual destination failure before EOF/EPIPE becomes visible to the client. */
internal class PrivilegedWriteState {
    private val failure = AtomicReference<Throwable>()

    fun write(closeInput: () -> Unit, onFailure: (Throwable) -> Unit, copy: () -> Unit) {
        try {
            copy()
        } catch (error: Throwable) {
            failure.compareAndSet(null, error)
            runCatching { onFailure(error) }
            throw error
        } finally {
            runCatching { closeInput() }
        }
    }

    fun checkFailure() {
        failure.get()?.let { throw IllegalStateException("Cannot write APK stream", it) }
    }
}
