package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/** Answers `org.autojs.plugin.INFO` (category `installer`) for the AutoJs6 plugin center. */
class ThreeSetupInstallerPluginInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo {
            return threeSetupInstallerPluginRuntimeInfo().toPluginInfo().apply {
                // Explicit and auditable: no ABI restriction (see AGENTS.md, PluginInfo rules).
                supportedAbis = emptyArray()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
