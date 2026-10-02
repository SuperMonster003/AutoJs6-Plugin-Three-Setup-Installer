package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerResolver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

internal object InstallationUi {
    private val main = Handler(Looper.getMainLooper())
    fun show(context: Context, record: InstallPresentation.Record, grants: Intent? = null) {
        record.show(grants)
        if (record.notificationMode) return
        main.postDelayed({
            record.notifyIfWaitingForActivity {
                InstallNotifications.notifyAction(context, record.token, record.activityIntent())
            }
        }, 1_500)
    }
    fun configure(context: Context, record: InstallPresentation.Record, environment: InstallSession.Environment,
        index: Int, prepared: PreparedPackage, target: InstallSession.Target, request: InstallRequest,
        deadline: Long, checkActive: () -> Unit): InstallSession.Selection {
        val choice = record.confirm(index, prepared, target, request.options, deadline, checkActive)
        checkActive()
        val selected = ArchiveOpener.select(prepared, choice.selectedApkNames)
        val authorizer = Authorizer.fromId(choice.options.authorizer)
        if (authorizer?.privileged == true && !AuthorizerStates.state(context, authorizer).usable) {
            val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(1)
            if (!AuthorizerStates.request(context, authorizer, minOf(remaining, InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS))) {
                val state = AuthorizerStates.state(context, authorizer)
                throw InstallFailure(if (state.available && state.running) InstallerErrorCodes.AUTHORIZER_DENIED else InstallerErrorCodes.AUTHORIZER_UNAVAILABLE,
                    state.reason ?: "Authorization is not available")
            }
        }
        checkActive()
        val resolved = environment.resolve(request.copy(options = choice.options), deadline, checkActive)
        return InstallSession.Selection(selected, resolved, choice.options)
    }
}
