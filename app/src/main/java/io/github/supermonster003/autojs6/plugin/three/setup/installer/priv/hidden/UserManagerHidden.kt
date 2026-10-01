package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess.booleanType

internal object UserManagerHidden {
    fun users(): Bundle {
        val descriptor = "android.os.IUserManager"
        val remote = HiddenApiAccess.service("user", descriptor)
        val types = if (Build.VERSION.SDK_INT >= 30) arrayOf(booleanType, booleanType, booleanType) else arrayOf(booleanType)
        val args = if (Build.VERSION.SDK_INT >= 30) arrayOf(true, true, true) else arrayOf(true)
        val users = HiddenApiAccess.invoke(remote, HiddenApiAccess.method(descriptor, "getUsers", *types), *args) as List<*>
        val userClass = Class.forName("android.content.pm.UserInfo")
        // IUserManager gained this query in Android 8. Android 7 uses IActivityManager instead.
        val runningTarget = if (Build.VERSION.SDK_INT >= 26) remote else
            Class.forName("android.app.ActivityManagerNative").getMethod("getDefault").invoke(null)!!
        val running = if (Build.VERSION.SDK_INT >= 26) HiddenApiAccess.method(descriptor, "isUserRunning", HiddenApiAccess.intType)
            else HiddenApiAccess.method("android.app.IActivityManager", "isUserRunning", HiddenApiAccess.intType, HiddenApiAccess.intType)
        return Bundle().apply {
            putParcelableArrayList("users", ArrayList(users.map { user ->
                Bundle().apply {
                    val id = userClass.getField("id").getInt(user)
                    putInt("id", id)
                    putString("name", userClass.getField("name").get(user) as String?)
                    putBoolean("isPrimary", userClass.getMethod("isPrimary").invoke(user) as Boolean)
                    putBoolean("isRunning", if (Build.VERSION.SDK_INT >= 26) HiddenApiAccess.invoke(runningTarget, running, id) as Boolean
                        else HiddenApiAccess.invoke(runningTarget, running, id, 0) as Boolean)
                }
            }))
        }
    }
}
