package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.util.AtomicFile
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DhizukuSessionJournal
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/** Debug-only, same-UID bound probe. Its caller must kill this process and recover its exact journal. */
class DhizukuSessionDeathProbeService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var case: String? = null
    private var heldSession: PackageInstaller.Session? = null
    private var heldLease: DhizukuSessionJournal.Lease? = null
    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == IBinder.FIRST_CALL_TRANSACTION) {
                check(Binder.getCallingUid() == Process.myUid())
                data.enforceInterface(DESCRIPTOR)
                requireNotNull(reply).writeNoException()
                reply.writeInt(Process.myPid())
                reply.writeString(requireNotNull(case))
                return true
            }
            return super.onTransact(code, data, reply, flags)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        check(intent.getBooleanExtra("confirmDhizukuFixture", false))
        val token = requireNotNull(intent.getStringExtra("case"))
        check(UUID.fromString(token).toString() == token && case == null)
        case = token
        val directory = directory(this, token)
        check(!directory.exists() && directory.mkdirs())
        worker.execute {
            val status = JsonObject().apply { addProperty("case", token); addProperty("pid", Process.myPid()) }
            try {
                val testPackage = "$packageName.test"
                check(packageManager.checkSignatures(packageName, testPackage) == PackageManager.SIGNATURE_MATCH)
                val fixture = File(directory, "fixture.apk")
                createPackageContext(testPackage, 0).assets.open("fixture-v1.apk").use { input -> fixture.outputStream().use(input::copyTo) }
                @Suppress("DEPRECATION")
                val info = requireNotNull(packageManager.getPackageArchiveInfo(fixture.absolutePath, 0))
                check(info.packageName == FIXTURE && requireNotNull(info.applicationInfo).flags and ApplicationInfo.FLAG_HAS_CODE == 0)
                @Suppress("DEPRECATION") check(info.versionCode == 1)
                val framework = DhizukuFramework(this)
                val journal = DhizukuSessionJournal(this)
                val parameters = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(FIXTURE); setSize(fixture.length())
                }
                val lease = journal.create(framework, parameters, FIXTURE, fixture.length()).also { heldLease = it }
                val session = framework.openSession(lease.record.id).also { heldSession = it }
                val bytes = fixture.inputStream().use { input -> ByteArray(4096).also { check(input.read(it) == it.size) } }
                framework.checked { session.openWrite("base.apk", 0, fixture.length()) }.use { output ->
                    output.write(bytes)
                    framework.checked { session.fsync(output) }
                }
                status.addProperty("ready", true)
                status.addProperty("session", lease.record.id)
                status.addProperty("sessionToken", lease.record.token)
                status.addProperty("bytesWritten", bytes.size)
            } catch (failure: Exception) {
                status.addProperty("ready", false)
                status.addProperty("failure", failure.javaClass.name)
            }
            val atomic = AtomicFile(File(directory, "status.json"))
            val stream = atomic.startWrite()
            try { stream.write(status.toString().toByteArray(Charsets.UTF_8)); stream.fd.sync(); atomic.finishWrite(stream) }
            catch (failure: Exception) { atomic.failWrite(stream); throw failure }
        }
        return binder
    }

    // Deliberately retain the partial platform session until the instrumentation caller kills
    // this independent process. Unbinding first prevents Android from restarting a bound service.
    override fun onDestroy() { super.onDestroy() }

    companion object {
        const val DESCRIPTOR = "three.setup.installer.DhizukuDeathProbe"
        const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture"
        fun directory(context: Context, token: String): File {
            require(UUID.fromString(token).toString() == token)
            return File(context.cacheDir, "p8-dhizuku-probes/$token")
        }
    }
}
