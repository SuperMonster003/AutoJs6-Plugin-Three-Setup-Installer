package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.autojs.plugin.installer.api.InstallerContract

/** Device-owner sessions are not shell/root sessions. Reject unsupported options before any write. */
internal object DhizukuPolicy {
    fun user(user: String, currentId: Int) {
        if (user != InstallerContract.USER_CURRENT && user != currentId.toString()) {
            throw RequestDocuments.invalid("Dhizuku can operate only in its current device/profile-owner user")
        }
    }
    fun install(options: InstallOptions, currentId: Int, ownerPackage: String) {
        user(options.user, currentId)
        if (options.allowDowngrade || options.allowTestOnly || options.bypassLowTargetSdk) {
            throw RequestDocuments.invalid("Dhizuku does not grant shell/root installation flags; use Shizuku or Root for these options")
        }
        if (options.installer != null && options.installer != ownerPackage) {
            throw RequestDocuments.invalid("Dhizuku installation attribution must use its device/profile-owner package")
        }
    }
    fun uninstall(request: UninstallRequest, currentId: Int) {
        user(request.user, currentId)
        if (request.keepData) throw RequestDocuments.invalid("Dhizuku uninstallation does not support the shell/root keepData flag")
    }
}
