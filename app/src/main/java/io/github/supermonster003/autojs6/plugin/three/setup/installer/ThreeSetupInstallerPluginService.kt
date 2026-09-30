package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Host-facing installer service answering `org.autojs.plugin.INSTALLER` (category `installer`).
 *
 * P0 returns a placeholder Binder that only carries the future `IInstallerPlugin` descriptor:
 * the host can discover and bind the service, but every transaction is rejected until roadmap
 * P1.2 replaces this object with the `IInstallerPlugin.Stub` of the host `installer-api` module.
 */
class ThreeSetupInstallerPluginService : Service() {

    private val binder: IBinder = Binder().apply {
        attachInterface(null, ThreeSetupInstallerPlugin.SERVICE_DESCRIPTOR)
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
