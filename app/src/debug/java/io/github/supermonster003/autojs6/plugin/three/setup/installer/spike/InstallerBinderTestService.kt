package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.InstallerBinder

/** Debug-only remote endpoint. The production service always uses HostCallerGuard. */
class InstallerBinderTestService : Service() {
    private lateinit var installer: InstallerBinder
    override fun onCreate() {
        super.onCreate()
        installer = InstallerBinder(this, object : CallerGuard {
            override fun enforceHost(): Int = Binder.getCallingUid().also {
                if (it != Process.myUid()) throw SecurityException("Debug test endpoint requires the plugin UID")
            }
        })
    }
    override fun onBind(intent: Intent?): IBinder = installer
    override fun onDestroy() {
        installer.close()
        io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient.get(this).releaseAll()
        super.onDestroy()
    }
}
