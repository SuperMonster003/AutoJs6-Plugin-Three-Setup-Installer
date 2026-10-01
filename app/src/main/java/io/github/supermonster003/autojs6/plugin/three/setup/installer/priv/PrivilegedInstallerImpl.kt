package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import android.content.ComponentName
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageInstallerHidden
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.UserManagerHidden
import java.io.Closeable
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Shared by the Shizuku and libsu processes. Each client owns at most four live sessions. */
internal open class PrivilegedInstallerImpl(expectedOwnerUid: Int? = null) : IPrivilegedInstaller.Stub(), Closeable {
    private val packages = PackageManagerHidden()
    private val installer = packages.installer()
    private val ownerUid = expectedOwnerUid ?: packages.packageUid(ThreeSetupInstallerPlugin.PACKAGE_NAME, Process.myUid() / 100000)
    private val userId = ownerUid / 100000
    private val sessions = mutableMapOf<Int, Record>()
    private val writers = Executors.newFixedThreadPool(PrivilegedOptions.MAX_SESSIONS)
    private var client: IBinder? = null
    private var closed = false
    private val death = IBinder.DeathRecipient { close() }

    override fun attachClient(token: IBinder) = synchronized(this) {
        checkCaller()
        check(!closed) { "Service is closed" }
        check(client == null || client == token) { "A client is already attached" }
        if (client == null) {
            token.linkToDeath(death, 0)
            client = token
        }
    }

    override fun getUid(): Int {
        checkCaller()
        return Process.myUid()
    }

    override fun getInstalledVersion(packageName: String, userId: Int): Bundle = privileged {
        PrivilegedOptions.validatePackage(packageName)
        require(userId >= 0) { "Invalid user" }
        val info = packages.packageInfo(packageName, userId)
        Bundle().apply {
            if (info != null) {
                putString("versionName", info.versionName)
                @Suppress("DEPRECATION")
                putLong("versionCode", if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
            }
        }
    }

    override fun createSession(params: Bundle, installerPackageName: String, userId: Int): Int = privileged {
        require(params.keySet().all { it == PrivilegedOptions.FLAGS || it == PrivilegedOptions.SIZE }) { "Unknown session option" }
        PrivilegedOptions.validatePackage(installerPackageName)
        require(userId >= 0) { "Invalid user" }
        val flags = params.getInt(PrivilegedOptions.FLAGS, PrivilegedOptions.INSTALL_REPLACE_EXISTING)
        PrivilegedOptions.validateFlags(flags, Build.VERSION.SDK_INT)
        val size = params.getLong(PrivilegedOptions.SIZE, -1)
        require(size >= -1) { "Invalid size" }
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            if (size >= 0) setSize(size)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            if (Build.VERSION.SDK_INT >= 33) setPackageSource(PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE)
        }
        PackageInstallerHidden.setFlags(sessionParams, flags)
        synchronized(this) {
            checkActive()
            check(sessions.size < PrivilegedOptions.MAX_SESSIONS) { "Too many sessions" }
            val id = installer.createSession(sessionParams, installerPackageName, userId)
            try {
                sessions[id] = Record(installer.openSession(id))
            } catch (failure: Throwable) {
                runCatching { installer.abandonSession(id) }
                throw failure
            }
            id
        }
    }

    override fun openWrite(sessionId: Int, name: String, length: Long): ParcelFileDescriptor = privileged {
        PrivilegedOptions.validateName(name)
        require(length > 0) { "APK length must be positive" }
        val record = record(sessionId)
        synchronized(record) {
            checkActive()
            check(!record.committing) { "Session is committing" }
            require(record.writes.size < PrivilegedOptions.MAX_SPLITS && name !in record.writes) { "Too many or duplicate splits" }
            val output = record.session.openWrite(name, 0, length)
            // Reliable pipes also send a Unix socket, rejected across Magisk/app SELinux domains.
            // Length checks plus the writer Future carry failures without that extra descriptor.
            val pipe = try { ParcelFileDescriptor.createPipe() } catch (failure: Throwable) {
                output.close()
                throw failure
            }
            record.inputs += pipe[0]
            record.outputs += output
            // API 24 returns a FileBridge socket, not an APK stream. The framework handles its protocol.
            val work = Runnable {
                try {
                    output.use { destination ->
                        ParcelFileDescriptor.AutoCloseInputStream(pipe[0]).use { source ->
                            val buffer = ByteArray(256 * 1024)
                            val poll = StructPollfd().apply {
                                fd = pipe[0].fileDescriptor
                                events = OsConstants.POLLIN.toShort()
                            }
                            val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(30)
                            var copied = 0L
                            while (true) {
                                check(!record.cancelled && !Thread.currentThread().isInterrupted) { "Session was abandoned" }
                                check(System.nanoTime() < deadline) { "APK stream timed out" }
                                if (Os.poll(arrayOf(poll), 1000) == 0) continue
                                val count = source.read(buffer)
                                if (count < 0) break
                                copied += count
                                check(copied <= length) { "APK exceeded declared length" }
                                destination.write(buffer, 0, count)
                            }
                            check(copied == length) { "Incomplete APK stream" }
                            record.session.fsync(destination)
                        }
                    }
                } catch (failure: Throwable) {
                    if (!record.cancelled && record.writeFailureLogged.compareAndSet(false, true)) {
                        Log.w("PrivilegedInstaller", "APK write failed (declaredBytes=$length)", failure)
                    }
                    runCatching { pipe[0].close() }
                    throw failure
                }
            }
            try {
                record.writes[name] = writers.submit(work)
            } catch (failure: Exception) {
                output.close()
                pipe.forEach { it.close() }
                record.inputs.remove(pipe[0])
                record.outputs.remove(output)
                throw failure
            }
            pipe[1]
        }
    }

    override fun commit(sessionId: Int, sender: IntentSender) = privileged {
        val record = record(sessionId)
        val pending = synchronized(record) {
            check(!record.committing && record.writes.isNotEmpty()) { "Session is empty or committing" }
            record.committing = true
            record.writes.values.toList()
        }
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            pending.forEach { it.get((deadline - System.nanoTime()).coerceAtLeast(0), TimeUnit.NANOSECONDS) }
            record.session.commit(sender)
            // Keep ownership until the caller receives a terminal result and abandons/releases it.
            // Pending user action must still be cancellable after commit has returned.
            record.closeSession()
        } catch (failure: Exception) {
            remove(sessionId, abandon = true)
            throw IllegalStateException("Cannot commit APK stream", failure)
        }
    }

    override fun abandon(sessionId: Int) = privileged { remove(sessionId, abandon = true) }

    override fun release(sessionId: Int) = privileged { remove(sessionId, abandon = false) }

    override fun uninstall(packageName: String, flags: Int, userId: Int, sender: IntentSender) = privileged {
        PrivilegedOptions.validatePackage(packageName)
        require(flags and 3.inv() == 0 && userId >= 0) { "Invalid uninstall flags or user" }
        installer.uninstall(packageName, callerPackage(), flags, userId, sender)
    }

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean): Int = privileged {
        require(component.packageName == ThreeSetupInstallerPlugin.PACKAGE_NAME) { "Only this plugin may be selected" }
        if (!enable) {
            packages.clearPreferred(component.packageName)
            return@privileged 0
        }
        val requests = PackageManagerHidden.INSTALL_ACTIONS.flatMap { action ->
            listOf("content", "file").map { scheme ->
                val intent = PackageManagerHidden.intent(action, scheme)
                val matches = packages.query(intent, userId)
                require(matches.any { ComponentName(it.activityInfo.packageName, it.activityInfo.name) == component }) { "Installer entry is unavailable" }
                Triple(intent, PackageManagerHidden.filter(action, scheme), matches)
            }
        }
        if (!packages.canReplacePreferred) {
            val others = requests.flatMap { it.third }.map { it.activityInfo.packageName }
                .distinct().filter { it != component.packageName }
            val competing = others.flatMap(packages::preferredActivities).any { filter ->
                requests.any { (intent, _, _) ->
                    filter.match(intent.action, intent.type, intent.scheme, intent.data, intent.categories, "ThreeSetup") >= 0
                }
            }
            if (competing) return@privileged PrivilegedOptions.DEFAULT_REQUIRES_CLEAR
            // Legacy replacePreferredActivity rejects MIME filters. Clear only our own preferences.
            packages.clearPreferred(component.packageName)
        }
        // The six-argument API replaces exact filters, preserving other packages' unrelated defaults.
        requests.forEach { (_, filter, matches) -> packages.addPreferred(filter, matches, component, userId) }
        requests.count { (intent, _, _) -> packages.resolve(intent, userId) == component }
    }

    override fun getUsers(): Bundle = privileged { UserManagerHidden.users() }

    override fun destroy() {
        val caller = Binder.getCallingUid()
        check(caller == ownerUid || caller == 0 || caller == 2000) { "Unauthorized service shutdown" }
        close()
    }

    override fun close() {
        val ids = synchronized(this) {
            if (closed) return
            closed = true
            client?.let { runCatching { it.unlinkToDeath(death, 0) } }
            client = null
            sessions.keys.toList()
        }
        val identity = Binder.clearCallingIdentity()
        try {
            ids.forEach { remove(it, abandon = true) }
            writers.shutdownNow()
        } finally {
            Binder.restoreCallingIdentity(identity)
        }
    }

    private fun record(id: Int): Record = synchronized(this) { requireNotNull(sessions[id]) { "Unknown session" } }

    private fun remove(id: Int, abandon: Boolean) {
        val record = synchronized(this) { sessions.remove(id) } ?: return
        synchronized(record) {
            record.committing = true
            record.cancelled = true
            record.inputs.forEach { runCatching { it.close() } }
            if (abandon) runCatching { installer.abandonSession(id) }
            record.outputs.forEach { runCatching { it.close() } }
            record.writes.values.forEach { it.cancel(true) }
            runCatching { record.closeSession() }
        }
    }

    private fun checkCaller() {
        if (Binder.getCallingUid() != ownerUid) throw SecurityException("Only the plugin may use this service")
    }

    private inline fun <T> privileged(block: () -> T): T {
        checkCaller()
        checkActive()
        val identity = Binder.clearCallingIdentity()
        return try { block() } catch (failure: ReflectiveOperationException) {
            // Binder cannot marshal checked reflection exceptions on older Android versions.
            // Report an explicit supported exception instead of a successful reply with null data.
            throw IllegalStateException("Privileged framework API is unavailable: ${failure.message}", failure)
        } finally { Binder.restoreCallingIdentity(identity) }
    }

    private fun checkActive() = synchronized(this) {
        check(!closed && client?.isBinderAlive == true) { "No live client" }
    }

    private fun callerPackage() = if (Process.myUid() == 2000) "com.android.shell" else ThreeSetupInstallerPlugin.PACKAGE_NAME

    private class Record(val session: PackageInstaller.Session) {
        var committing = false
        private var sessionClosed = false
        @Volatile var cancelled = false
        val writeFailureLogged = AtomicBoolean(false)
        val inputs = mutableListOf<ParcelFileDescriptor>()
        val outputs = mutableListOf<OutputStream>()
        val writes = linkedMapOf<String, Future<*>>()

        @Synchronized
        fun closeSession() {
            if (sessionClosed) return
            // A Binder failure may occur after the framework processed close; never retry it.
            sessionClosed = true
            session.close()
        }
    }
}
