package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InstallChoicesTest {
    private val apks = listOf(InstallChoices.Apk("base.apk", true), InstallChoices.Apk("feature.apk", false))

    @Test fun baseCannotBeRemovedAndUnknownComponentsCannotBeAdded() {
        val draft = InstallChoices(InstallOptions(), apks, false)
        assertFalse(draft.select("base.apk", false))
        assertFalse(draft.select("not-staged.apk", true))
        assertTrue(draft.select("feature.apk", false))
        assertEquals(setOf("base.apk"), draft.snapshot().selectedApkNames)
        assertTrue(draft.select("feature.apk", true))
        assertEquals(setOf("base.apk", "feature.apk"), draft.snapshot().selectedApkNames)
    }

    @Test fun deletionChoiceCannotAcquireAuthorityFromAHostDescriptor() {
        val draft = InstallChoices(InstallOptions(), apks, false)
        draft.updateOptions(draft.snapshot().options.copy(deleteSource = true, allowTestOnly = true))
        assertFalse(draft.snapshot().options.deleteSource)
        assertTrue(draft.snapshot().options.allowTestOnly)
        val senderOwned = InstallChoices(InstallOptions(deleteSource = true), apks, false)
        senderOwned.updateOptions(senderOwned.snapshot().options.copy(deleteSource = false))
        assertTrue(senderOwned.snapshot().options.deleteSource)
        val external = InstallChoices(InstallOptions(), apks, true)
        external.updateOptions(external.snapshot().options.copy(deleteSource = true))
        assertTrue(external.snapshot().options.deleteSource)
    }

    @Test fun invalidUserDraftSurvivesOtherOptionChangesAndCannotBeConfirmed() {
        val draft = InstallChoices(InstallOptions(), apks, false)
        for (input in listOf("", "-1", "2147483648", "1.2", "other")) {
            draft.setUser(input)
            draft.updateOptions(draft.snapshot().options.copy(allowDowngrade = true))
            assertEquals(input, draft.userInput())
            assertFalse(draft.valid())
            assertEquals(InstallerContract.USER_CURRENT, draft.snapshot().options.user)
        }
        draft.setUser("10")
        assertTrue(draft.valid())
        assertEquals("10", draft.snapshot().options.user)
        draft.setUser(InstallerContract.USER_ALL)
        assertTrue(draft.valid())
        assertEquals(InstallerContract.USER_ALL, draft.snapshot().options.user)
    }

    @Test fun publishedChoiceIsUnaffectedByLaterDraftChanges() {
        val draft = InstallChoices(InstallOptions(), apks, false)
        val submitted = draft.snapshot()
        draft.select("feature.apk", false)
        draft.updateOptions(draft.snapshot().options.copy(allowDowngrade = true))
        assertEquals(setOf("base.apk", "feature.apk"), submitted.selectedApkNames)
        assertFalse(submitted.options.allowDowngrade)
    }

    @Test fun competingAnswersOnlyReleaseOneOutcome() {
        val decision = InstallDecision<String>()
        val pool = Executors.newFixedThreadPool(2)
        val gate = CountDownLatch(1)
        val winners = AtomicInteger()
        try {
            val a = pool.submit { gate.await(); if (decision.answer("approved")) winners.incrementAndGet() }
            val b = pool.submit { gate.await(); if (decision.answer(null)) winners.incrementAndGet() }
            gate.countDown()
            a.get(2, TimeUnit.SECONDS)
            b.get(2, TimeUnit.SECONDS)
            assertEquals(1, winners.get())
            assertTrue(decision.await(1))
            assertFalse(decision.answer("late click"))
            assertTrue(decision.result() == null || decision.result() == "approved")
        } finally { pool.shutdownNow() }
    }
}
