package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

/** A permission detour and an activity recreation must never submit the confirmation twice. */
internal class UserActionState(private val requiresPermission: Boolean) {
    enum class Action { NONE, REQUEST_PERMISSION, CONFIRM, ACCEPTED, RETURNED, CANCELLED }
    private enum class Phase { NEW, PERMISSION, CONFIRMATION, ACCEPTED, RETURNED, CLOSED }

    private var phase = Phase.NEW

    val pending: Boolean @Synchronized get() = phase == Phase.NEW || phase == Phase.PERMISSION || phase == Phase.CONFIRMATION
    val accepted: Boolean @Synchronized get() = phase == Phase.ACCEPTED
    val awaitingPlatformResult: Boolean @Synchronized get() = phase == Phase.ACCEPTED || phase == Phase.RETURNED

    @Synchronized
    fun start(permissionGranted: Boolean): Action {
        if (phase != Phase.NEW) return Action.NONE
        return if (requiresPermission && !permissionGranted) {
            phase = Phase.PERMISSION
            Action.REQUEST_PERMISSION
        } else {
            phase = Phase.CONFIRMATION
            Action.CONFIRM
        }
    }

    @Synchronized
    fun permissionResult(granted: Boolean): Action {
        if (phase != Phase.PERMISSION) return Action.NONE
        phase = if (granted) Phase.CONFIRMATION else Phase.CLOSED
        return if (granted) Action.CONFIRM else Action.CANCELLED
    }

    @Synchronized
    fun confirmationResult(accepted: Boolean): Action {
        if (phase != Phase.CONFIRMATION) return Action.NONE
        phase = if (accepted) Phase.ACCEPTED else Phase.CLOSED
        return if (accepted) Action.ACCEPTED else Action.CANCELLED
    }

    /** Older system installers also return the default cancelled result after an approval. */
    @Synchronized
    fun confirmationDismissed(): Action {
        if (phase != Phase.CONFIRMATION) return Action.NONE
        phase = Phase.RETURNED
        return Action.RETURNED
    }

    @Synchronized
    fun cancel(): Boolean {
        if (phase == Phase.ACCEPTED || phase == Phase.RETURNED || phase == Phase.CLOSED) return false
        phase = Phase.CLOSED
        return true
    }

    @Synchronized
    fun close() { phase = Phase.CLOSED }
}
