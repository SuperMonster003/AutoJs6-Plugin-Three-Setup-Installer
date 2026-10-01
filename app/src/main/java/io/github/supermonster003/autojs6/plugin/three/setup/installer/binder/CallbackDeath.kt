package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import android.os.IBinder
import android.os.RemoteException
import java.io.Closeable

internal class CallbackDeath(private val binder: IBinder, private val onDeath: () -> Unit) : IBinder.DeathRecipient, Closeable {
    private val lock = Any()
    private var linked = false
    private var closed = false
    fun link() {
        try {
            synchronized(lock) {
                if (closed) return
                binder.linkToDeath(this, 0)
                linked = true
            }
        } catch (_: RemoteException) { binderDied() }
    }
    override fun binderDied() { close(); onDeath() }
    override fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            if (linked) runCatching { binder.unlinkToDeath(this, 0) }
        }
        Unit
    }
}
