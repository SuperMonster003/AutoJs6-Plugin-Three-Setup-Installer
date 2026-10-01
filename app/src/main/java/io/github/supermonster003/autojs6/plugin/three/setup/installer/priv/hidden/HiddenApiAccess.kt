package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.os.Build
import android.os.IBinder
import android.system.Os
import android.system.OsConstants
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.FileDescriptor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/** Uses device framework Stubs, including their actual transaction numbers. No android.* classes ship. */
internal object HiddenApiAccess {
    @Synchronized
    fun initialize() {
        if (initialized) return
        if (Build.VERSION.SDK_INT >= 28) {
            check(HiddenApiBypass.addHiddenApiExemptions("")) { "Hidden API access is unavailable" }
        }
        initialized = true
    }

    private var initialized = false

    fun service(name: String, descriptor: String, wrap: (IBinder) -> IBinder = { it }): Any {
        initialize()
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java).invoke(null, name) as IBinder
        return Class.forName("$descriptor\$Stub").getMethod("asInterface", IBinder::class.java)
            .invoke(null, wrap(binder))!!
    }

    fun invoke(target: Any?, method: Method, vararg args: Any?): Any? = try {
        method.invoke(target, *args)
    } catch (failure: InvocationTargetException) {
        throw failure.targetException
    }

    fun method(descriptor: String, name: String, vararg types: Class<*>): Method =
        Class.forName(descriptor).getMethod(name, *types)

    /** Os.fcntlInt exists on API 24 but only became part of the public SDK on API 30. */
    fun accessMode(descriptor: FileDescriptor): Int {
        val flags = if (Build.VERSION.SDK_INT >= 30) Os.fcntlInt(descriptor, OsConstants.F_GETFL, 0) else {
            initialize()
            invoke(null, method("android.system.Os", "fcntlInt", FileDescriptor::class.java, intType, intType),
                descriptor, OsConstants.F_GETFL, 0) as Int
        }
        return flags and OsConstants.O_ACCMODE
    }

    fun setNonBlocking(descriptor: FileDescriptor) {
        if (Build.VERSION.SDK_INT >= 30) {
            val flags = Os.fcntlInt(descriptor, OsConstants.F_GETFL, 0)
            Os.fcntlInt(descriptor, OsConstants.F_SETFL, flags or OsConstants.O_NONBLOCK)
        } else {
            initialize()
            val fcntl = method("android.system.Os", "fcntlInt", FileDescriptor::class.java, intType, intType)
            val flags = invoke(null, fcntl, descriptor, OsConstants.F_GETFL, 0) as Int
            invoke(null, fcntl, descriptor, OsConstants.F_SETFL, flags or OsConstants.O_NONBLOCK)
        }
    }

    val intType: Class<*> = Integer.TYPE
    val booleanType: Class<*> = java.lang.Boolean.TYPE
    val flagsType: Class<*> get() = if (Build.VERSION.SDK_INT >= 33) java.lang.Long.TYPE else intType
    fun flags(value: Int): Any = if (Build.VERSION.SDK_INT >= 33) value.toLong() else value
}
