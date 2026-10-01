package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.os.DeadObjectException
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.InvocationTargetException

class DhizukuFrameworkFailureTest {
    @Test fun libraryAndReflectionWrappersPreserveBinderDeath() {
        val death = DeadObjectException()
        assertSame(death, DhizukuFramework.unwrap(InvocationTargetException(RuntimeException(death))))
        val failure = assertThrows(InstallFailure::class.java) { DhizukuFramework.call<Unit> { throw RuntimeException(death) } }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
    }

    @Test fun unrelatedRuntimeFailuresAndKnownFailuresKeepTheirMeaning() {
        val cause = IllegalArgumentException("caller error")
        val outer = RuntimeException(cause)
        assertSame(outer, DhizukuFramework.unwrap(outer))
        val known = InstallFailure(InstallerErrorCodes.CANCELLED, "cancelled")
        assertSame(known, assertThrows(InstallFailure::class.java) { DhizukuFramework.call<Unit> { throw known } })
    }

    @Test fun anAuthorityNameAloneCannotAuthenticateAnOwnerProvider() {
        assertTrue(DhizukuFramework.providerMatchesOwner("com.owner", 10042, 0, "com.owner", 10042, true))
        assertFalse(DhizukuFramework.providerMatchesOwner("com.owner", 10042, 0, "com.attacker", 10042, true))
        assertFalse(DhizukuFramework.providerMatchesOwner("com.owner", 10042, 0, "com.owner", 10043, true))
        assertFalse(DhizukuFramework.providerMatchesOwner("com.owner", 10042, 10, "com.owner", 10042, true))
        assertFalse(DhizukuFramework.providerMatchesOwner("com.owner", 10042, 0, "com.owner", 10042, false))
        assertTrue(DhizukuFramework.providerMatchesOwner("com.owner", 1010042, 10, "com.owner", 1010042, true))
    }
}
