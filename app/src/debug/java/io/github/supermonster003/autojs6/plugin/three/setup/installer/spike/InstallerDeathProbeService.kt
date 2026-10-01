package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import org.autojs.plugin.installer.api.IInstallerSessionCallback

/** A disposable callback process for testing a client's actual Binder death. Never shipped. */
class InstallerDeathProbeService : Service() {
    private val callback = object : IInstallerSessionCallback.Stub() {
        override fun onStage(id: String?, stage: String?, detail: Bundle?) {
            if (Binder.getCallingUid() == Process.myUid() && stage == "die") Process.killProcess(Process.myPid())
        }
        override fun onProgress(id: String?, progress: Float, detail: Bundle?) = Unit
        override fun onCompleted(id: String?, result: Bundle?) = Unit
        override fun onFailed(id: String?, error: Bundle?) = Unit
    }
    override fun onBind(intent: Intent?): IBinder = callback
}
