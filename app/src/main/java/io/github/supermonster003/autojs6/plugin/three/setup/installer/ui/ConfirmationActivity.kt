package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Minimal explicit confirmation; P3 expands this surface with icons, split choices and progress. */
class ConfirmationActivity : AppCompatActivity() {
    private var ticket: PluginConfirmation.Ticket? = null
    private var dialog: androidx.appcompat.app.AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entry = intent.getStringExtra(PluginConfirmation.EXTRA_TOKEN)?.let { PluginConfirmation.attach(it, this) }
        if (entry == null) { finish(); return }
        ticket = entry
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val density = resources.displayMetrics.density
        val shape = MaterialShapeDrawable(ShapeAppearanceModel.builder().setAllCornerSizes(24 * density).build()).apply {
            fillColor = ColorStateList.valueOf(if (dark) Color.rgb(30, 30, 30) else Color.WHITE)
        }
        dialog = MaterialAlertDialogBuilder(this).setTitle(entry.title).setMessage(entry.message).setBackground(shape)
            .setNegativeButton(android.R.string.cancel) { _, _ -> entry.answer(false); finish() }
            .setPositiveButton(entry.positive) { _, _ -> entry.answer(true); finish() }
            .setOnCancelListener { entry.answer(false); finish() }
            .create().also { alert ->
                alert.setOnShowListener {
                    alert.findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)?.textSize = 20f
                    alert.findViewById<android.widget.TextView>(android.R.id.message)?.textSize = 14f
                    val accent = com.google.android.material.color.MaterialColors.getColorRoles(Color.rgb(255, 222, 173), !dark).accent
                    listOf(android.content.DialogInterface.BUTTON_POSITIVE, android.content.DialogInterface.BUTTON_NEGATIVE).forEach {
                        alert.getButton(it).apply { isAllCaps = false; setTextColor(accent) }
                    }
                    val width = minOf((560 * density).toInt(), resources.displayMetrics.widthPixels - (48 * density).toInt())
                    alert.window?.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
                }
                alert.show()
            }
    }
    override fun onDestroy() {
        if (!isChangingConfigurations) ticket?.answer(false)
        dialog?.dismiss()
        super.onDestroy()
    }
}

internal object PluginConfirmation {
    const val EXTRA_TOKEN = "confirmationToken"
    private val tickets = ConcurrentHashMap<String, Ticket>()
    private val main = Handler(Looper.getMainLooper())
    class Ticket(val title: String, val message: String, val positive: String) {
        val result = AtomicInteger(0)
        val ready = CountDownLatch(1)
        var activity = WeakReference<ConfirmationActivity>(null)
        fun answer(accepted: Boolean) { if (result.compareAndSet(0, if (accepted) 1 else 2)) ready.countDown() }
    }
    fun attach(token: String, activity: ConfirmationActivity): Ticket? = tickets[token]?.also { it.activity = WeakReference(activity) }

    fun install(context: Context, request: InstallRequest, prepared: PreparedPackage, target: InstallSession.Target, deadline: Long, checkActive: () -> Unit) {
        val details = mutableListOf(context.getString(R.string.confirm_install_body,
            prepared.label ?: prepared.displayName, prepared.packageName.orEmpty(), prepared.versionName ?: prepared.versionCode?.toString().orEmpty(),
            authorizer(context, target.authorizer), user(context, request.options.user, target.userId)))
        if (request.options.allowDowngrade) details += context.getString(R.string.confirm_allow_downgrade)
        if (request.options.allowTestOnly) details += context.getString(R.string.confirm_allow_test)
        if (request.options.bypassLowTargetSdk) details += context.getString(R.string.confirm_bypass_target)
        request.options.installer?.let { details += context.getString(R.string.confirm_installer, it) }
        await(context, Ticket(context.getString(R.string.confirm_install_title), details.joinToString("\n"), context.getString(R.string.action_install)), deadline, checkActive)
    }

    fun uninstall(context: Context, request: UninstallRequest, authorizer: Authorizer, userId: Int, deadline: Long, checkActive: () -> Unit) {
        var message = context.getString(R.string.confirm_uninstall_body, request.packageName, authorizer(context, authorizer), user(context, request.user, userId))
        if (request.keepData) message += "\n" + context.getString(R.string.confirm_keep_data)
        await(context, Ticket(context.getString(R.string.confirm_uninstall_title), message, context.getString(R.string.action_uninstall)), deadline, checkActive)
    }

    private fun authorizer(context: Context, authorizer: Authorizer) = when (authorizer) {
        Authorizer.NONE -> context.getString(R.string.confirm_system)
        Authorizer.ROOT -> "Root"
        Authorizer.SHIZUKU -> "Shizuku"
    }
    private fun user(context: Context, requested: String, id: Int) = if (requested == InstallerContract.USER_ALL) context.getString(R.string.confirm_all_users) else id.toString()

    internal fun await(context: Context, ticket: Ticket, deadline: Long, checkActive: () -> Unit) {
        val token = UUID.randomUUID().toString()
        tickets[token] = ticket
        try {
            checkActive()
            try {
                context.startActivity(Intent(context, ConfirmationActivity::class.java).putExtra(EXTRA_TOKEN, token)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
            } catch (failure: Exception) {
                throw InstallFailure(InstallerErrorCodes.BLOCKED_BY_POLICY, "The plugin confirmation could not be opened", systemMessage = failure.message)
            }
            val expires = SystemClock.elapsedRealtime() + InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS
            while (true) {
                checkActive()
                if (SystemClock.elapsedRealtime() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The operation timed out")
                if (SystemClock.elapsedRealtime() >= expires) throw InstallFailure(InstallerErrorCodes.USER_ACTION_TIMEOUT, "The plugin confirmation timed out")
                if (ticket.ready.await(100, TimeUnit.MILLISECONDS)) {
                    if (ticket.result.get() != 1) throw InstallFailure(InstallerErrorCodes.USER_CANCELLED, "The operation was declined")
                    checkActive()
                    return
                }
            }
        } finally {
            tickets.remove(token)
            main.post { ticket.activity.get()?.finish() }
        }
    }
}
