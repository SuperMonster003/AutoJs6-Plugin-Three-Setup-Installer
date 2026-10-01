package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.VersionedPackage
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess.intType

/** Signatures from AOSP IPackageInstaller (Android 7, 9, 12 and 15). */
internal class PackageInstallerHidden(private val remote: Any) {
    fun createSession(params: PackageInstaller.SessionParams, installer: String, user: Int): Int {
        val types = mutableListOf<Class<*>>(PackageInstaller.SessionParams::class.java, String::class.java)
        val args = mutableListOf<Any?>(params, installer)
        if (Build.VERSION.SDK_INT >= 31) {
            types += String::class.java
            args += null
        }
        types += intType
        args += user
        return call("createSession", types.toTypedArray(), *args.toTypedArray()) as Int
    }

    fun openSession(id: Int): PackageInstaller.Session {
        val session = call("openSession", arrayOf(intType), id)
        // The framework Session handles both FileBridge (old Android) and revocable file descriptors.
        return PackageInstaller.Session::class.java
            .getConstructor(Class.forName("android.content.pm.IPackageInstallerSession"))
            .newInstance(session)
    }

    fun abandonSession(id: Int) {
        call("abandonSession", arrayOf(intType), id)
    }

    fun sessionInfo(id: Int): PackageInstaller.SessionInfo? =
        call("getSessionInfo", arrayOf(intType), id) as PackageInstaller.SessionInfo?

    fun uninstall(name: String, caller: String, flags: Int, user: Int, sender: IntentSender) {
        val packageType: Class<*>
        val packageValue: Any
        if (Build.VERSION.SDK_INT >= 26) {
            packageType = VersionedPackage::class.java
            packageValue = VersionedPackage(name, -1)
        } else {
            packageType = String::class.java
            packageValue = name
        }
        call("uninstall", arrayOf(packageType, String::class.java, intType, IntentSender::class.java, intType),
            packageValue, caller, flags, sender, user)
    }

    private fun call(name: String, types: Array<Class<*>>, vararg args: Any?): Any? =
        HiddenApiAccess.invoke(remote, HiddenApiAccess.method(DESCRIPTOR, name, *types), *args)

    companion object {
        private const val DESCRIPTOR = "android.content.pm.IPackageInstaller"

        fun setFlags(params: PackageInstaller.SessionParams, flags: Int) {
            PackageInstaller.SessionParams::class.java.getField("installFlags").setInt(params, flags)
        }
    }
}
