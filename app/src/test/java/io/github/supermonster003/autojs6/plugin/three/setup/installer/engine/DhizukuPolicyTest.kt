package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test

class DhizukuPolicyTest {
    private val owner = "example.device.owner"

    @Test fun currentOwnerUserAndItsExactNumericIdAreAcceptedForInstallAndUninstall() {
        for (currentId in listOf(0, 10)) for (user in listOf(C.USER_CURRENT, currentId.toString())) {
            DhizukuPolicy.install(InstallOptions(authorizer = C.AUTHORIZER_DHIZUKU, user = user), currentId, owner)
            DhizukuPolicy.install(InstallOptions(authorizer = C.AUTHORIZER_DHIZUKU, user = user, installer = owner,
                deleteSource = true, continueOnError = false), currentId, owner)
            DhizukuPolicy.uninstall(uninstall(user = user), currentId)
        }
    }

    @Test fun crossUserAllUsersAndInvalidUserSelectorsAreRejectedBeforeAnyOperation() {
        for (user in listOf(C.USER_ALL, "10", "-1", "", "not-a-user")) {
            invalid { DhizukuPolicy.install(InstallOptions(user = user), 0, owner) }
            invalid { DhizukuPolicy.uninstall(uninstall(user = user), 0) }
        }
        invalid { DhizukuPolicy.install(InstallOptions(user = "0"), 10, owner) }
        invalid { DhizukuPolicy.uninstall(uninstall(user = "0"), 10) }
    }

    @Test fun shellAndRootOnlyInstallFlagsAreRejectedIndividuallyAndTogether() {
        for (options in listOf(
            InstallOptions(allowDowngrade = true),
            InstallOptions(allowTestOnly = true),
            InstallOptions(bypassLowTargetSdk = true),
            InstallOptions(allowDowngrade = true, allowTestOnly = true, bypassLowTargetSdk = true),
        )) invalid { DhizukuPolicy.install(options, 0, owner) }
    }

    @Test fun attributionCannotBeChangedToThePluginShellOrAnotherOwner() {
        for (installer in listOf("com.android.shell", "example.other.owner", "io.github.supermonster003.autojs6.plugin.three.setup.installer", "")) {
            invalid { DhizukuPolicy.install(InstallOptions(installer = installer), 0, owner) }
        }
        DhizukuPolicy.install(InstallOptions(installer = null), 0, owner)
        DhizukuPolicy.install(InstallOptions(installer = owner), 0, owner)
    }

    @Test fun keepDataIsNotClaimedForDeviceOwnerUninstallation() {
        invalid { DhizukuPolicy.uninstall(uninstall(keepData = true), 0) }
        DhizukuPolicy.uninstall(uninstall(), 0)
    }

    @Test fun newNotificationInteractionIsStillRejectedByTheUninstallBoundary() {
        invalid {
            UninstallRequest.parse("""{"packageName":"example.fixture","authorizer":"dhizuku","interaction":"notification"}""")
        }
    }

    private fun uninstall(user: String = C.USER_CURRENT, keepData: Boolean = false) = UninstallRequest(
        "example.fixture", keepData, user, C.AUTHORIZER_DHIZUKU, C.INTERACTION_SILENT, 30_000)

    private fun invalid(action: () -> Unit) {
        val failure = assertThrows(InstallFailure::class.java) { action() }
        assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, failure.code)
        assertFalse(failure.retryable)
    }
}
