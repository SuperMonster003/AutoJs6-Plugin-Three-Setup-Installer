package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.InstallerBinder

/**
 * Host-facing installer service answering `org.autojs.plugin.INSTALLER` (category `installer`).
 *
 * All package operations run on bounded workers behind caller and session ownership checks.
 */
class ThreeSetupInstallerPluginService : Service() {

    private lateinit var binder: InstallerBinder

    override fun onCreate() {
        super.onCreate()
        binder = InstallerBinder(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        binder.close()
        super.onDestroy()
    }
}
