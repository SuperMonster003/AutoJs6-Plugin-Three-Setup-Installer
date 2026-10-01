package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UninstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UserActionLauncher
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Privileged uninstall confirmation. A retained ticket owns the draft, never the Activity. */
class ConfirmationActivity : HostAppearanceActivity() {
    private var token: String? = null
    private var ticket: PluginConfirmation.Ticket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        val supplied = intent.getStringExtra(PluginConfirmation.EXTRA_TOKEN)
        val restored = savedInstanceState?.getString(PluginConfirmation.EXTRA_TOKEN)
        val entry = supplied?.takeIf { restored == null || restored == it }?.let { PluginConfirmation.attach(it, this) }
        if (entry == null) { finish(); return }
        token = supplied
        ticket = entry
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { entry.answer(false); finish() }
        })
        render()
    }

    override fun onAppearanceChanged() { if (ticket != null) render() }

    private fun render() {
        val entry = ticket ?: return
        val dialog = kit.dialog(getString(R.string.confirm_uninstall_title))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        entry.icon?.let { icon ->
            row.addView(ImageView(this).apply {
                setImageBitmap(icon)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(kit.dp(48), kit.dp(48)).apply { marginEnd = kit.dp(16) })
        }
        row.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(kit.text(entry.label, 16f, medium = true))
            addView(kit.text(entry.request.packageName, color = kit.palette.muted).apply { setTextIsSelectable(true) })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        dialog.content.addView(row)
        val authorization = when (entry.authorizer) {
            Authorizer.NONE -> getString(R.string.confirm_system)
            Authorizer.ROOT -> "Root"
            Authorizer.SHIZUKU -> "Shizuku"
        }
        val target = if (entry.request.user == InstallerContract.USER_ALL) getString(R.string.confirm_all_users) else entry.userId.toString()
        dialog.content.addView(kit.text(getString(R.string.uninstall_authorization, authorization), color = kit.palette.muted).apply {
            setPaddingRelative(0, kit.dp(16), 0, kit.dp(4))
        })
        dialog.content.addView(kit.text(getString(R.string.uninstall_target_user, target), color = kit.palette.muted))
        dialog.content.addView(kit.switch(getString(R.string.confirm_keep_data), entry.keepData, TAG_KEEP_DATA, entry::setKeepData).apply {
            isEnabled = entry.authorizer.privileged
            minHeight = kit.dp(72)
        })
        dialog.content.addView(kit.text(getString(R.string.uninstall_keep_data_explanation), color = kit.palette.muted))
        dialog.actions.addView(kit.textButton(getString(R.string.action_cancel), TAG_CANCEL) {
            entry.answer(false)
            finish()
        }.apply { id = android.R.id.button2 })
        dialog.actions.addView(kit.textButton(getString(R.string.action_uninstall), TAG_CONFIRM, danger = true) {
            entry.answer(true)
            finish()
        }.apply { id = android.R.id.button1 })
        setContentView(dialog.root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        token?.let { outState.putString(PluginConfirmation.EXTRA_TOKEN, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        token?.let { PluginConfirmation.detach(it, this, isChangingConfigurations) }
        ticket = null
        super.onDestroy()
    }

    companion object {
        const val TAG_KEEP_DATA = "uninstall_keep_data"
        const val TAG_CANCEL = "uninstall_cancel"
        const val TAG_CONFIRM = "uninstall_confirm"
    }
}

internal object PluginConfirmation {
    const val EXTRA_TOKEN = "confirmationToken"
    private val tickets = ConcurrentHashMap<String, Ticket>()
    private val main = Handler(Looper.getMainLooper())

    class Ticket(
        val request: UninstallRequest,
        val authorizer: Authorizer,
        val userId: Int,
        val label: String = request.packageName,
        val icon: Bitmap? = null,
    ) {
        val result = AtomicInteger(0)
        val ready = CountDownLatch(1)
        @Volatile private var draftKeepData = request.keepData
        @Volatile var context: Context? = null
        @Volatile var activity = WeakReference<ConfirmationActivity>(null)
        val keepData: Boolean get() = draftKeepData
        val isPending: Boolean get() = result.get() == 0
        private var acceptedRequest: UninstallRequest? = null

        @Synchronized
        fun setKeepData(value: Boolean) {
            if (isPending && authorizer.privileged) draftKeepData = value
        }

        @Synchronized
        fun answer(accepted: Boolean) {
            if (!result.compareAndSet(0, if (accepted) 1 else 2)) return
            if (accepted) acceptedRequest = request.copy(keepData = draftKeepData)
            ready.countDown()
        }

        @Synchronized
        fun confirmedRequest(): UninstallRequest = acceptedRequest
            ?: throw InstallFailure(InstallerErrorCodes.USER_CANCELLED, "The uninstallation was declined")
    }

    fun attach(token: String, activity: ConfirmationActivity): Ticket? {
        val ticket = tickets[token] ?: return null
        val attached = synchronized(ticket) {
            if (!ticket.isPending) return@synchronized null
            val previous = ticket.activity.get()
            if (previous != null && previous !== activity && !previous.isChangingConfigurations &&
                !previous.isFinishing && !previous.isDestroyed) return@synchronized null
            ticket.activity = WeakReference(activity)
            ticket
        }
        if (attached != null) ticket.context?.let { UserActionLauncher.dismissNotification(it, token) }
        return attached
    }

    fun detach(token: String, activity: ConfirmationActivity, changingConfigurations: Boolean) {
        val ticket = tickets[token] ?: return
        synchronized(ticket) {
            if (ticket.activity.get() !== activity) return
            ticket.activity.clear()
            if (!changingConfigurations) ticket.answer(false)
        }
    }

    fun activityIntent(context: Context, token: String): Intent? = tickets[token]?.takeIf { it.isPending }?.let {
        Intent(context, ConfirmationActivity::class.java).putExtra(EXTRA_TOKEN, token)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    fun isAttached(token: String): Boolean = tickets[token]?.activity?.get()?.let { !it.isFinishing && !it.isDestroyed } == true

    @Suppress("DEPRECATION")
    fun uninstall(context: Context, request: UninstallRequest, authorizer: Authorizer, userId: Int,
        deadline: Long, checkActive: () -> Unit): UninstallRequest {
        checkActive()
        val packages = context.packageManager
        val info = runCatching { packages.getApplicationInfo(request.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES) }.getOrNull()
        val label = info?.let { runCatching { packages.getApplicationLabel(it).toString() }.getOrNull() } ?: request.packageName
        val icon = runCatching {
            val drawable = info?.loadIcon(packages) ?: packages.defaultActivityIcon
            Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).also { bitmap ->
                drawable.setBounds(0, 0, 96, 96)
                drawable.draw(Canvas(bitmap))
            }
        }.getOrNull()
        return await(context, Ticket(request, authorizer, userId, label, icon), deadline, checkActive)
    }

    internal fun await(context: Context, ticket: Ticket, deadline: Long, checkActive: () -> Unit): UninstallRequest {
        val token = UUID.randomUUID().toString()
        ticket.context = context.applicationContext
        tickets[token] = ticket
        try {
            checkActive()
            val expires = SystemClock.elapsedRealtime() + InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS
            UserActionLauncher.launch(context, requireNotNull(activityIntent(context, token)))
            while (true) {
                checkActive()
                if (SystemClock.elapsedRealtime() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The operation timed out")
                if (SystemClock.elapsedRealtime() >= expires) throw InstallFailure(InstallerErrorCodes.USER_ACTION_TIMEOUT, "The plugin confirmation timed out")
                if (ticket.ready.await(100, TimeUnit.MILLISECONDS)) {
                    if (ticket.result.get() != 1) throw InstallFailure(InstallerErrorCodes.USER_CANCELLED, "The operation was declined")
                    checkActive()
                    return ticket.confirmedRequest()
                }
            }
        } finally {
            tickets.remove(token)
            ticket.answer(false)
            UserActionLauncher.dismissNotification(context, token)
            main.post { ticket.activity.get()?.finish() }
        }
    }
}
