package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

internal class DeviceUsers(context: Context) {
    private val context = context.applicationContext
    val currentId: Int get() = Process.myUid() / 100000

    fun list(authorizer: Authorizer, timeoutMillis: Long = PrivilegedClient.BIND_TIMEOUT_MILLIS): List<DeviceUser> {
        if (!authorizer.privileged || authorizer == Authorizer.DHIZUKU) {
            val manager = context.getSystemService(UserManager::class.java)
            return listOf(DeviceUser(currentId, null, manager.isSystemUser, true))
        }
        val reply = PrivilegedClient.get(context).acquire(authorizer, timeoutMillis).users
            ?: throw InstallFailure(InstallerErrorCodes.INTERNAL, "Privileged service returned no user list")
        val users = if (Build.VERSION.SDK_INT >= 33) reply.getParcelableArrayList("users", Bundle::class.java)
            else @Suppress("DEPRECATION") reply.getParcelableArrayList<Bundle>("users")
        return users.orEmpty().map { DeviceUser(it.getInt("id", -1), it.getString("name"), it.getBoolean("isPrimary"), it.getBoolean("isRunning")) }
            .also { values ->
                if (values.isEmpty() || values.any { it.id < 0 } || values.map { it.id }.distinct().size != values.size) {
                    throw InstallFailure(InstallerErrorCodes.INTERNAL, "Privileged service returned an invalid user list")
                }
            }
    }

    fun resolve(user: String, authorizer: Authorizer, timeoutMillis: Long): Int =
        resolve(user, authorizer, currentId, list(authorizer, timeoutMillis))

    companion object {
        fun resolve(user: String, authorizer: Authorizer, currentId: Int, users: List<DeviceUser>): Int {
            RequestDocuments.userOf(user, "target user")
            if (authorizer == Authorizer.DHIZUKU) DhizukuPolicy.user(user, currentId)
            if (!authorizer.privileged && user != InstallerContract.USER_CURRENT) {
                throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Selecting users requires Shizuku or Root")
            }
            val id = if (user == InstallerContract.USER_CURRENT || user == InstallerContract.USER_ALL) currentId else user.toInt()
            if (users.none { it.id == id }) throw RequestDocuments.invalid("Target user $id does not exist")
            return id
        }
    }
}
