package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.os.Process
import android.os.RemoteException
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.shared.DhizukuVariables
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.reflect.InvocationTargetException
import java.security.MessageDigest

/** System owner/provider identity, including signer identity to reject a recycled package UID. */
internal data class DhizukuOwnerIdentity(
    val component: String, val packageName: String, val uid: Int, val userId: Int,
    val provider: String, val signingSha256: String,
)

/** Isolated framework objects; never mutate the application's cached managers or global services. */
internal class DhizukuFramework(private val context: Context) {
    val owner: ComponentName
    val identity: DhizukuOwnerIdentity
    val installer: PackageInstaller
    init {
        identity = checkReady(context)
        HiddenApiAccess.initialize()
        owner = requireNotNull(ComponentName.unflattenFromString(identity.component))
        val base = context.packageManager.packageInstaller
        val field = PackageInstaller::class.java.getDeclaredField("mInstaller").apply { isAccessible = true }
        val wrapped = proxy("android.content.pm.IPackageInstaller", field.get(base) as IInterface)
        val api = Class.forName("android.content.pm.IPackageInstaller")
        val constructor = if (Build.VERSION.SDK_INT >= 31) PackageInstaller::class.java.getDeclaredConstructor(api, String::class.java, String::class.java, Int::class.javaPrimitiveType)
            else PackageInstaller::class.java.getDeclaredConstructor(api, String::class.java, Int::class.javaPrimitiveType)
        constructor.isAccessible = true
        installer = if (Build.VERSION.SDK_INT >= 31) constructor.newInstance(wrapped, owner.packageName, null, Process.myUid() / 100000)
            else constructor.newInstance(wrapped, owner.packageName, Process.myUid() / 100000)
    }

    fun openSession(id: Int): PackageInstaller.Session {
        return checked {
            val raw = installer.openSession(id)
            var wrapped: Any? = null
            try {
                val field = PackageInstaller.Session::class.java.getDeclaredField("mSession").apply { isAccessible = true }
                wrapped = proxy("android.content.pm.IPackageInstallerSession", field.get(raw) as IInterface)
                // This Session was just created for this handle. Reuse it instead of retaining
                // two Java wrappers for the same single platform open/close reference.
                field.set(raw, wrapped)
                raw
            } catch (failure: Exception) {
                runCatching {
                    if (wrapped != null) HiddenApiAccess.invoke(wrapped,
                        HiddenApiAccess.method("android.content.pm.IPackageInstallerSession", "close"))
                    else raw.close()
                }.onFailure(failure::addSuppressed)
                throw failure
            }
        }
    }

    fun policyManager(): DevicePolicyManager {
        val ownerContext = context.createPackageContext(owner.packageName, Context.CONTEXT_IGNORE_SECURITY)
        val manager = ownerContext.getSystemService(DevicePolicyManager::class.java)
        val field = DevicePolicyManager::class.java.getDeclaredField("mService").apply { isAccessible = true }
        val wrapped = proxy("android.app.admin.IDevicePolicyManager", field.get(manager) as IInterface)
        field.set(manager, wrapped)
        return manager
    }

    /** Pin each transaction to the currently authorized owner, without replaying failed writes. */
    fun <T> checked(action: () -> T): T = synchronized(transportLock) {
        val current = checkReady(context)
        if (current != identity) throw unavailable("The Dhizuku owner/provider identity changed")
        call(action)
    }

    private fun proxy(descriptor: String, original: IInterface): Any = Class.forName("$descriptor\$Stub")
        .getMethod("asInterface", IBinder::class.java).invoke(null, Dhizuku.binderWrapper(original.asBinder()))!!

    companion object {
        private val transportLock = Any()

        /** No library initialization or remote calls until the provider is proved to be the owner. */
        fun currentOwner(context: Context): DhizukuOwnerIdentity {
            if (Build.VERSION.SDK_INT < 26) throw unavailable("Dhizuku requires Android API 26 or later")
            val component = call { Dhizuku.getOwnerComponent(context) }
                ?: throw unavailable("An active Dhizuku device/profile owner is required")
            val packages = context.packageManager
            val provider = packages.resolveContentProvider(DhizukuVariables.getProviderAuthorityName(component.packageName), 0)
                ?: throw unavailable("The active owner does not expose a Dhizuku provider")
            val application = packages.getApplicationInfo(component.packageName, 0)
            val user = Process.myUid() / 100000
            if (!providerMatchesOwner(component.packageName, application.uid, user, provider.packageName,
                    provider.applicationInfo.uid, provider.enabled && application.enabled)) {
                throw unavailable("The Dhizuku provider does not belong to the active owner in this user")
            }
            @Suppress("DEPRECATION")
            val info = packages.getPackageInfo(component.packageName,
                if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            val certificates = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            if (certificates.isNullOrEmpty()) throw unavailable("Cannot verify the Dhizuku owner signing identity")
            val signers = certificates.map { certificate -> sha256(certificate.toByteArray()) }.sorted().joinToString("\n")
            return DhizukuOwnerIdentity(component.flattenToString(), component.packageName, application.uid, user,
                ComponentName(provider.packageName, provider.name).flattenToString(), sha256(signers.toByteArray(Charsets.UTF_8)))
        }

        internal fun providerMatchesOwner(ownerPackage: String, ownerUid: Int, userId: Int,
            providerPackage: String, providerUid: Int, enabled: Boolean): Boolean =
            enabled && ownerUid >= 0 && ownerUid / 100000 == userId &&
                providerPackage == ownerPackage && providerUid == ownerUid

        fun initializeOwner(context: Context): DhizukuOwnerIdentity = synchronized(transportLock) {
            val expected = currentOwner(context)
            if (!call { Dhizuku.init(context) }) throw unavailable("Dhizuku device/profile owner is unavailable")
            // The API can reuse a live cached owner. Do not authorize a changed admin/provider
            // merely because the old owner's package still has another admin component.
            if (currentOwner(context) != expected || call { Dhizuku.getOwnerComponent().flattenToString() } != expected.component) {
                throw unavailable("Dhizuku cached ownership differs from the current system owner")
            }
            expected
        }

        fun permissionGranted(context: Context, expected: DhizukuOwnerIdentity): Boolean = synchronized(transportLock) {
            if (initializeOwner(context) != expected) throw unavailable("The Dhizuku owner/provider identity changed")
            call { Dhizuku.isPermissionGranted() }
        }

        fun checkReady(context: Context): DhizukuOwnerIdentity = synchronized(transportLock) {
            val owner = initializeOwner(context)
            if (!call { Dhizuku.isPermissionGranted() }) throw InstallFailure(InstallerErrorCodes.AUTHORIZER_DENIED, "Dhizuku permission is not granted")
            owner
        }

        /** The API wraps RemoteException; reflection can wrap it once more. Preserve its cause. */
        internal fun unwrap(failure: Throwable): Throwable {
            var actual = failure
            repeat(8) {
                actual = when {
                    actual is InvocationTargetException -> (actual as InvocationTargetException).targetException ?: return actual
                    actual.javaClass == RuntimeException::class.java && actual.cause is RemoteException -> actual.cause!!
                    else -> return actual
                }
            }
            return actual
        }

        fun <T> call(action: () -> T): T = try { action() } catch (failure: Exception) {
            val actual = unwrap(failure)
            if (actual is IllegalStateException && actual.message == "binder haven't been received") {
                throw unavailable("The Dhizuku service is unavailable")
            }
            throw InstallFailure.from(actual)
        }

        private fun unavailable(message: String) = InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, message)
        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
