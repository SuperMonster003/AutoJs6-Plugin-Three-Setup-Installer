package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

internal class RootInstallerService : RootService() {
    private val installer by lazy { PrivilegedInstallerImpl(applicationInfo.uid) }
    override fun onBind(intent: Intent): IBinder = installer
    override fun onDestroy() {
        installer.close()
        super.onDestroy()
    }
}
