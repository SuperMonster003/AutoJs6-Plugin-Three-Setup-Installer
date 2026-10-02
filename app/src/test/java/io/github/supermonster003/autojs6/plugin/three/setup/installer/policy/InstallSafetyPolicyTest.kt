package io.github.supermonster003.autojs6.plugin.three.setup.installer.policy

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InstallSafetyPolicyTest {
    private val revision = UUID.randomUUID().toString()
    private fun binding() = InstallSafetyBinding(0, "example.app", null,
        listOf(ApkSafetyDigest("base.apk", 10, "a".repeat(64))), 0, "current", "root", revision,
        setOf("source"), InstalledSigningFact(true, true, true, setOf("installed"), 1, "1", 100))
    private fun approval(facts: InstallSafetyBinding = binding()) = DialogSafetyApproval.fromDialog("owner", UUID.randomUUID().toString(), facts)

    @Test fun unconfiguredPolicyAndExplicitEmptyPolicyAreReadable() {
        assertTrue(InstallSafetyPolicy.decode(null, null, null).readable)
        val saved = InstallSafetyPolicy.decode(revision, "[]", "[]")
        assertTrue(saved.readable)
        assertNull(saved.block("example.app", null))
    }

    @Test fun malformedStoredRulesFailClosedInsteadOfDroppingTheirEntries() {
        for (bad in listOf(null, "", "{}", "[1]", "['example.app']", "[example.app]", "[\"example.app\"] trailing",
            "[\"example.*\"]", "[\"example.app\",\"example.app\"]")) {
            assertFalse(bad, InstallSafetyPolicy.decode(revision, bad, "[]").readable)
            assertFalse(bad, InstallSafetyPolicy.decode(revision, "[]", bad).readable)
        }
        assertFalse(InstallSafetyPolicy.decode(null, "[]", "[]").readable)
        assertFalse(InstallSafetyPolicy.decode("invalid", "[]", "[]").readable)
        assertEquals(InstallSafetyPolicy.Block.UNREADABLE, InstallSafetyPolicy("bad", readable = false).block("example.app", null))
    }

    @Test fun rulesAreExactCaseSensitiveIdentifiersAndAllowTheFrameworkPackage() {
        val rules = InstallSafetyPolicy.parseLines("\nandroid\n example.App \nexample.App\n")
        assertEquals(setOf("android", "example.App"), rules)
        val policy = InstallSafetyPolicy(revision, rules)
        assertEquals(InstallSafetyPolicy.Block.PACKAGE, policy.block("android", null))
        assertEquals(InstallSafetyPolicy.Block.PACKAGE, policy.block("example.App", null))
        assertNull(policy.block("example.app", null))
        assertNull(policy.block("example.App.extra", null))
        for (bad in listOf("example.*", "example.", "example..app", "example.2app", "example/app", "example.app;pm", "ä.app", "a".repeat(256))) {
            assertThrows(bad, IllegalArgumentException::class.java) { InstallSafetyPolicy.parseLines(bad) }
        }
        assertThrows(IllegalArgumentException::class.java) { InstallSafetyPolicy.parseLines((0..256).joinToString("\n") { "example.p$it" }) }
    }

    @Test fun declaredAndExistingSharedUsersAreBothBlockedWithoutAnException() {
        val policy = InstallSafetyPolicy(revision, sharedUsers = setOf("example.shared"))
        assertEquals(InstallSafetyPolicy.Block.SHARED_USER, policy.block("example.app", "example.shared"))
        assertEquals(InstallSafetyPolicy.Block.SHARED_USER,
            policy.block("example.app", null, InstalledSigningFact(true, true, sharedUserId = "example.shared")))
        assertNull(policy.block("example.app", "example.shared.other", InstalledSigningFact(true, false)))
    }

    @Test fun unknownOrUserScopedAbsenceCannotBypassExistingSharedUserRules() {
        val policy = InstallSafetyPolicy(revision, sharedUsers = setOf("example.shared"))
        assertEquals(InstallSafetyPolicy.Block.INSTALLED_IDENTITY_UNKNOWN, policy.block("example.app", null, InstalledSigningFact(false, false)))
        assertEquals(InstallSafetyPolicy.Block.INSTALLED_IDENTITY_UNKNOWN,
            policy.block("example.app", null, InstalledSigningFact(true, false, global = false)))
        assertNull(policy.block("example.app", null, InstalledSigningFact(true, false)))
        assertNull(InstallSafetyPolicy().block("example.app", null, InstalledSigningFact(true, false, global = false)))
    }

    @Test fun knownAbsenceAllowsNewSignedPackagesButEmptyOrUnreadableSignersRemainUnknown() {
        val original = binding()
        assertEquals(SignatureRisk.NONE, original.copy(installed = InstalledSigningFact(true, false, global = false)).signatureRisk)
        assertEquals(SignatureRisk.UNKNOWN, original.copy(sourceSigners = emptySet(), installed = InstalledSigningFact(true, false)).signatureRisk)
        assertEquals(SignatureRisk.UNKNOWN, original.copy(installed = InstalledSigningFact(false, false)).signatureRisk)
        assertEquals(SignatureRisk.UNKNOWN, original.copy(installed = InstalledSigningFact(true, true)).signatureRisk)
        assertEquals(SignatureRisk.NONE, original.copy(installed = original.installed.copy(signers = original.sourceSigners)).signatureRisk)
        assertEquals(SignatureRisk.MISMATCH, original.signatureRisk)
    }

    @Test fun partialSignerOverlapIsNotAValidMultisignerMatch() {
        val original = binding().copy(sourceSigners = setOf("a", "b"), installed = InstalledSigningFact(true, true, signers = setOf("a", "c")))
        assertEquals(SignatureRisk.MISMATCH, original.signatureRisk)
        assertEquals(SignatureRisk.NONE, original.copy(installed = original.installed.copy(signers = setOf("b", "a"))).signatureRisk)
    }

    @Test fun approvalBindsEverySelectedByteTargetAndPolicyFactAndCannotBeReplayed() {
        val original = binding()
        val changed = listOf(original.copy(itemIndex = 1), original.copy(packageName = "example.other"), original.copy(sharedUserId = "example.shared"),
            original.copy(apks = listOf(original.apks.single().copy(sha256 = "b".repeat(64)))),
            original.copy(apks = original.apks + ApkSafetyDigest("feature.apk", 1, "c".repeat(64))),
            original.copy(userId = 10), original.copy(requestedUser = "all"), original.copy(authorizer = "shizuku"),
            original.copy(policyRevision = UUID.randomUUID().toString()), original.copy(sourceSigners = setOf("different")),
            original.copy(installed = original.installed.copy(signers = setOf("different"))),
            original.copy(installed = original.installed.copy(versionCode = 2)), original.copy(installed = original.installed.copy(lastUpdateTime = 101)),
            original.copy(installed = original.installed.copy(global = false)))
        changed.forEach { current ->
            val proof = approval(original)
            assertFalse(proof.consume("owner", current))
            assertFalse("A failed attempt is still one use", proof.consume("owner", original))
        }
        val wrongOwner = approval(original)
        assertFalse(wrongOwner.consume("another-owner", original))
        assertFalse(wrongOwner.consume("owner", original))
        val valid = approval(original)
        assertTrue(valid.consume("owner", original))
        assertFalse(valid.consume("owner", original))
    }

    @Test fun callerOwnedCollectionsCannotRewriteAnAlreadyApprovedIdentity() {
        val signers = mutableSetOf("source")
        val apks = binding().apks.toMutableList()
        val facts = binding().copy(apks = apks, sourceSigners = signers)
        val proof = approval(facts)
        signers += "another"
        apks += ApkSafetyDigest("new.apk", 1, "b".repeat(64))
        assertFalse(proof.consume("owner", facts))
    }

    @Test fun racingConsumersCanOnlyUseAnApprovalOnce() {
        val facts = binding()
        val proof = approval(facts)
        val go = CountDownLatch(1)
        val done = CountDownLatch(16)
        val accepted = AtomicInteger()
        val executor = Executors.newFixedThreadPool(16)
        try {
            repeat(16) { executor.execute { try { go.await(); if (proof.consume("owner", facts)) accepted.incrementAndGet() } finally { done.countDown() } } }
            go.countDown()
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals(1, accepted.get())
        } finally { executor.shutdownNow() }
    }
}
