package io.github.supermonster003.autojs6.plugin.three.setup.installer.queue

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDefaults
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import java.io.Closeable

/**
 * Home and sharing use the existing InstallSession batch engine: one source is prepared,
 * confirmed, installed and released before the next is opened. continueOnError and cancellation
 * therefore have exactly the same meaning as host/script batches. Nothing resumes after death.
 */
internal object InstallQueue {
    data class Snapshot(val token: String, val origin: String, val createdAt: Long, val state: InstallPresentation.Snapshot)

    fun start(context: Context, sources: ExternalSources, origin: String = InstallerContract.SOURCE_HOME,
        options: InstallOptions? = null, isBatch: Boolean = sources.uris.size > 1): String =
        ExternalInstaller.start(context, sources, options, origin, isBatch)

    /** Includes every live installation source, including silent Binder sessions. No IO. */
    fun snapshots(includeTerminal: Boolean = false): List<Snapshot> = InstallPresentation.snapshots(includeTerminal).map {
        Snapshot(it.token, it.origin, it.createdAt, it.state)
    }

    /** Notifications run on the main thread. The Activity closes its subscription in onStop. */
    fun observe(observer: () -> Unit): Closeable = InstallPresentation.observe(observer)

    fun cancel(token: String): Boolean {
        val record = InstallPresentation.find(token) ?: return false
        if (record.snapshot().terminal) return false
        record.cancel()
        return true
    }

    fun open(context: Context, token: String): Boolean {
        val record = InstallPresentation.find(token) ?: return false
        return runCatching { context.startActivity(record.activityIntent()); true }.getOrDefault(false)
    }
}
