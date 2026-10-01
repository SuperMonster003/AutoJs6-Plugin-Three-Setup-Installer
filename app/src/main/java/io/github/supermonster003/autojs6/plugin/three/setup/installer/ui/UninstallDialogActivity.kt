package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusBridge
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusMapper
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Non-exported, token-bound owner of ACTION_UNINSTALL_PACKAGE's activity result. */
class UninstallDialogActivity : Activity() {
    private var token: String? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN)
        val ticket = id?.let { UninstallDialogBridge.attach(it, this) }
        if (ticket == null) { finish(); return }
        token = id
        if (savedInstanceState != null) return
        try {
            startActivityForResult(Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:${ticket.packageName}"))
                .putExtra(Intent.EXTRA_RETURN_RESULT, true), REQUEST)
        } catch (failure: Exception) {
            ticket.results.offer(InstallFailure(
                if (failure is SecurityException) InstallerErrorCodes.BLOCKED_BY_POLICY else InstallerErrorCodes.UNINSTALL_FAILED,
                "The system uninstall confirmation could not be opened", systemMessage = failure.message, cause = failure))
            finish()
        }
    }

    @Deprecated("Platform activity result required for API 24 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST) return
        token?.let { UninstallDialogBridge.complete(it, resultCode, data) }
        finish()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) token?.let { UninstallDialogBridge.cancelIfPending(it) }
        super.onDestroy()
    }

    internal fun dismissConfirmation() {
        finishActivity(REQUEST)
        finish()
    }

    companion object { private const val REQUEST = 1 }
}

internal object UninstallDialogBridge {
    const val EXTRA_TOKEN = "uninstallToken"
    private val main = Handler(Looper.getMainLooper())
    private val tickets = ConcurrentHashMap<String, Ticket>()

    class Ticket(val packageName: String) {
        val results = LinkedBlockingQueue<Any>(1)
        var activity = WeakReference<UninstallDialogActivity>(null)
    }

    fun attach(token: String, activity: UninstallDialogActivity): Ticket? = tickets[token]?.also { it.activity = WeakReference(activity) }

    fun complete(token: String, resultCode: Int, data: Intent?) {
        val ticket = tickets[token] ?: return
        val status = when (resultCode) {
            Activity.RESULT_OK -> InstallStatusMapper.STATUS_SUCCESS
            Activity.RESULT_CANCELED -> InstallStatusMapper.STATUS_FAILURE_ABORTED
            else -> InstallStatusMapper.STATUS_FAILURE
        }
        // OEMs may include a legacy DELETE_* code. It is not a PackageInstaller STATUS_*.
        val message = if (data?.hasExtra("android.intent.extra.INSTALL_RESULT") == true)
            "System uninstall result: ${data.getIntExtra("android.intent.extra.INSTALL_RESULT", 0)}" else null
        ticket.results.offer(InstallStatusBridge.Status(status, message, ticket.packageName, null))
    }

    fun cancelIfPending(token: String) {
        tickets[token]?.results?.offer(InstallFailure(InstallerErrorCodes.USER_CANCELLED, "Uninstall confirmation closed"))
    }

    fun await(context: Context, packageName: String, deadline: Long, checkActive: () -> Unit,
        launch: (Intent) -> Unit): InstallStatusBridge.Status {
        val token = UUID.randomUUID().toString()
        val ticket = Ticket(packageName)
        tickets[token] = ticket
        try {
            checkActive()
            launch(Intent(context, UninstallDialogActivity::class.java).putExtra(EXTRA_TOKEN, token)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
            val confirmationDeadline = SystemClock.elapsedRealtime() + InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS
            while (true) {
                checkActive()
                if (SystemClock.elapsedRealtime() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The uninstallation timed out")
                if (SystemClock.elapsedRealtime() >= confirmationDeadline) throw InstallFailure(InstallerErrorCodes.USER_ACTION_TIMEOUT, "The uninstall confirmation timed out")
                when (val result = ticket.results.poll(100, TimeUnit.MILLISECONDS)) {
                    is InstallStatusBridge.Status -> return result
                    is InstallFailure -> throw result
                }
            }
        } finally {
            tickets.remove(token)
            main.post { ticket.activity.get()?.dismissConfirmation() }
        }
    }
}
