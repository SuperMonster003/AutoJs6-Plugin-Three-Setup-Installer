package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import androidx.annotation.Keep
import android.content.Context
import kotlin.system.exitProcess

@Keep
internal class ShizukuUserService @JvmOverloads constructor(context: Context? = null) :
    PrivilegedInstallerImpl(context?.applicationInfo?.uid) {
    override fun destroy() {
        super.destroy()
        exitProcess(0)
    }
}
