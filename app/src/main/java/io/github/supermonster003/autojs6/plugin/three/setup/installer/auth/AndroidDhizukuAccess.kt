package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.dhizuku.shared.DhizukuVariables
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/** No provisioning or permission prompts during state reads, and no API access below Android 8. */
internal class AndroidDhizukuAccess(context: Context) : DhizukuAccess {
    private val context = context.applicationContext
    override fun state(): AuthorizerState {
        if (Build.VERSION.SDK_INT < 26) return AuthorizerState(Authorizer.DHIZUKU, false, false, false, "Dhizuku requires Android API 26 or later")
        val owner = runCatching { Dhizuku.getOwnerComponent(context) }.getOrNull()
        val provider = owner?.let { context.packageManager.resolveContentProvider(DhizukuVariables.getProviderAuthorityName(it.packageName), 0) }
        val installed = provider != null || runCatching { context.packageManager.getPackageInfo("com.rosan.dhizuku", 0) }.isSuccess
        if (!installed) return AuthorizerState(Authorizer.DHIZUKU, false, false, false, "Dhizuku is not installed")
        if (provider == null) return AuthorizerState(Authorizer.DHIZUKU, true, false, false, "Activate a Dhizuku device/profile owner first")
        val identity = runCatching { DhizukuFramework.initializeOwner(context) }.getOrElse {
            return AuthorizerState(Authorizer.DHIZUKU, true, false, false, it.message ?: "The active owner has no trusted Dhizuku provider")
        }
        val granted = runCatching { DhizukuFramework.permissionGranted(context, identity) }.getOrElse {
            return AuthorizerState(Authorizer.DHIZUKU, true, false, false, "The Dhizuku service is unavailable")
        }
        return AuthorizerState(Authorizer.DHIZUKU, true, true, granted, if (granted) null else "Dhizuku permission is not granted")
    }

    override fun requestPermission(result: (Boolean) -> Unit): Closeable {
        val active = AtomicBoolean(true)
        val main = Handler(Looper.getMainLooper())
        val prompt = Runnable {
            if (active.get()) runCatching {
                if (Build.VERSION.SDK_INT < 26) result(false)
                else {
                    DhizukuFramework.initializeOwner(context)
                    Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                    override fun onRequestPermission(grantResult: Int) {
                        if (active.get()) result(grantResult == PackageManager.PERMISSION_GRANTED)
                    }
                    })
                }
            }.onFailure { if (active.get()) result(false) }
        }
        if (!main.post(prompt)) result(false)
        return Closeable { active.set(false); main.removeCallbacks(prompt) }
    }
}
