package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Only shell startup uses libsu; actual installation calls use the cached RootService Binder. */
internal object RootShellAccess {
    private val queue = RootShellQueue<Shell>(Executors.newSingleThreadExecutor { task ->
        Thread(task, "installer-root-shell").apply { isDaemon = true }
    })

    fun <T> submit(context: Context, action: (Shell) -> T, failed: (Exception) -> Unit = {}): Future<T> =
        queue.submit(open = {
            // A previous refusal must not make every later explicit request reuse a non-root shell.
            Shell.getCachedShell()?.takeIf { !it.isRoot }?.close()
            if (Shell.getCachedShell() == null) {
                Shell.setDefaultBuilder(Shell.Builder.create().setContext(context.applicationContext)
                    .setTimeout(RootAuthorizer.TIMEOUT_MILLIS / 1000))
            }
            Shell.getShell()
        }, action = action, failed = failed)
}
