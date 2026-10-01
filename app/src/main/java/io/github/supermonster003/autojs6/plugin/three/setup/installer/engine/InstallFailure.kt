package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import com.google.gson.JsonObject
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

/**
 * One failure of the installer with its contract code (`InstallerErrorCodes`), the platform
 * `PackageInstaller.STATUS_*` value and message when the platform produced it, and the package
 * the failure belongs to. Serialized as the error document of the Binder contract.
 */
internal class InstallFailure(
    val code: String,
    message: String,
    val status: Int? = null,
    val systemMessage: String? = null,
    val packageName: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    val retryable: Boolean get() = InstallerErrorCodes.isRetryableByDefault(code)

    fun withPackage(packageName: String?): InstallFailure =
        if (packageName == null || this.packageName != null) this else InstallFailure(code, message ?: code, status, systemMessage, packageName, cause)

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_ERROR_CODE, code)
        addProperty(InstallerContract.FIELD_ERROR_MESSAGE, message ?: code)
        status?.let { addProperty(InstallerContract.FIELD_ERROR_STATUS, it) }
        systemMessage?.let { addProperty(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE, it) }
        packageName?.let { addProperty(InstallerContract.FIELD_PACKAGE_NAME, it) }
        addProperty(InstallerContract.FIELD_ERROR_RETRYABLE, retryable)
    }

    companion object {
        /** Maps any throwable raised inside the engine to a failure; known failures pass through. */
        fun from(throwable: Throwable, packageName: String? = null): InstallFailure = when (throwable) {
            is InstallFailure -> throwable.withPackage(packageName)
            is InterruptedException -> InstallFailure(InstallerErrorCodes.CANCELLED, "Installation interrupted", packageName = packageName, cause = throwable)
            is SecurityException -> InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, throwable.message ?: "Privileged operation refused", packageName = packageName, cause = throwable)
            is IllegalArgumentException -> InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, throwable.message ?: "Invalid argument", packageName = packageName, cause = throwable)
            is java.io.FileNotFoundException -> InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, throwable.message ?: "Package source not found", packageName = packageName, cause = throwable)
            is java.io.IOException -> InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, throwable.message ?: "Package source cannot be read", packageName = packageName, cause = throwable)
            is android.os.DeadObjectException -> InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, "Privileged process died", packageName = packageName, cause = throwable)
            is android.os.RemoteException -> InstallFailure(InstallerErrorCodes.INTERNAL, "Privileged call failed: ${throwable.message}", packageName = packageName, cause = throwable)
            else -> InstallFailure(InstallerErrorCodes.INTERNAL, throwable.message ?: throwable.javaClass.simpleName, packageName = packageName, cause = throwable)
        }
    }
}
