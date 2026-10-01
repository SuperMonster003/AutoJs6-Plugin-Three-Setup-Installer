package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.system.ErrnoException
import android.system.OsConstants
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.util.Collections
import java.util.IdentityHashMap

/** Errno survives local IOException/reflection/Future wrappers; source text is never an errno. */
internal object StorageErrors {
    fun isInsufficientStorage(failure: Throwable): Boolean {
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = failure
        while (current != null && visited.add(current)) {
            if (current is ErrnoException && (current.errno == OsConstants.ENOSPC || current.errno == OsConstants.EDQUOT)) return true
            current = current.cause
        }
        return false
    }

    fun failure(cause: Throwable, packageName: String? = null) = InstallFailure(
        InstallerErrorCodes.INSUFFICIENT_STORAGE, "Not enough storage space to prepare or install the package",
        packageName = packageName, systemMessage = cause.message, cause = cause,
    )
}
