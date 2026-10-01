package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

/** AOSP PackageManager flags. Keep the private transport independent of the future host contract. */
internal object PrivilegedOptions {
    const val INSTALL_REPLACE_EXISTING = 0x2
    const val INSTALL_ALLOW_TEST = 0x4
    const val INSTALL_ALL_USERS = 0x40
    const val INSTALL_REQUEST_DOWNGRADE = 0x80
    const val INSTALL_ALLOW_DOWNGRADE = 0x100000
    const val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK = 0x1000000
    const val DELETE_KEEP_DATA = 0x1
    const val DELETE_ALL_USERS = 0x2
    const val MAX_SESSIONS = 4
    const val MAX_SPLITS = 64
    const val FLAGS = "flags"
    const val SIZE = "size"
    const val DEFAULT_REQUIRES_CLEAR = -1
    // Exact private Binder marker. Only a verified ENOSPC/EDQUOT cause may produce it.
    const val ERROR_INSUFFICIENT_STORAGE = "THREE_SETUP_INSTALLER:INSUFFICIENT_STORAGE"

    fun validateFlags(flags: Int, sdk: Int) {
        val allowed = INSTALL_REPLACE_EXISTING or INSTALL_ALLOW_TEST or INSTALL_ALL_USERS or
            INSTALL_REQUEST_DOWNGRADE or INSTALL_ALLOW_DOWNGRADE or
            (if (sdk >= 34) INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK else 0)
        require(flags and allowed.inv() == 0) { "Unsupported install flags" }
    }

    fun validateName(name: String) {
        require(name.length in 5..255 && Regex("[A-Za-z0-9][A-Za-z0-9_.-]*\\.apk").matches(name)) { "Invalid split name" }
    }

    fun validatePackage(name: String) {
        require(name.length <= 255 && Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+").matches(name)) { "Invalid package name" }
    }
}
