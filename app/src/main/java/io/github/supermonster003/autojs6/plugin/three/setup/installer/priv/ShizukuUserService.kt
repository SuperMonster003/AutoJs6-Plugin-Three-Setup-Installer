package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import androidx.annotation.Keep
import android.content.Context
import kotlin.system.exitProcess

@Keep
internal class ShizukuUserService @JvmOverloads constructor(context: Context? = null) :
    PrivilegedInstallerImpl(context?.applicationInfo?.uid) {
    init {
        // Shizuku's bootstrap calls System.exit when its server Binder dies, without invoking
        // destroy(). A normal VM exit still runs this hook; SIGKILL cannot and does not.
        Runtime.getRuntime().addShutdownHook(Thread({ close() }, "installer-privileged-shutdown"))
    }

    override fun destroy() {
        super.destroy()
        exitProcess(0)
    }
}
