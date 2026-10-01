package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/** Serial ownership covers both authorization probes and RootService launch tasks. */
internal class RootShellQueue<S : Closeable>(private val worker: ExecutorService) {
    fun <T> submit(open: () -> S, action: (S) -> T, failed: (Exception) -> Unit = {}): Future<T> =
        worker.submit<T> {
            try {
                open().use(action)
            } catch (failure: Exception) {
                failed(failure)
                throw failure
            }
        }
}
