package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PatternMatcher
import android.os.SystemClock
import android.util.Xml
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootPersistentDefaultProtocol
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootPersistentDefaults
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootShellAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.RootSystemDefaultMain
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.RootDuplicateDefaultFixture
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Opt-in API-24 probe; a supervisor must preserve the complete external default baseline. */
@RunWith(AndroidJUnit4::class)
class RootPersistentDefaultsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun providerSpecificAndWildcardApkPoliciesAreStillProtected() {
        for (type in listOf("application/vnd.android.package-archive", "application/*", "*/*")) {
            val filter = IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataType(type)
                addDataScheme("content")
                addDataAuthority("restricted.documents", null)
                addDataPath("/only-this-folder/", PatternMatcher.PATTERN_PREFIX)
            }
            assertTrue(type, RootSystemDefaultMain.overlapsApk(filter))
        }
        assertTrue(RootSystemDefaultMain.overlapsApk(IntentFilter(Intent.ACTION_VIEW).apply {
            addDataType("application/vnd.android.package-archive")
        }))
        assertFalse(RootSystemDefaultMain.overlapsApk(IntentFilter(Intent.ACTION_VIEW).apply {
            addDataType("text/plain"); addDataScheme("content")
        }))
        assertFalse(RootSystemDefaultMain.overlapsApk(IntentFilter(Intent.ACTION_SEND).apply {
            addDataType("application/vnd.android.package-archive"); addDataScheme("content")
        }))
        assertFalse(RootSystemDefaultMain.overlapsApk(IntentFilter(Intent.ACTION_VIEW).apply {
            addDataType("application/vnd.android.package-archive"); addDataScheme("https")
        }))
    }

    @Test fun unknownXmlFieldsAreNotClaimedAfterTheFrameworkDropsThem() {
        val component = ComponentName(context.packageName, "${context.packageName}.ui.ExternalInstallActivity")
        for (action in PackageManagerHidden.INSTALL_ACTIONS) for (scheme in listOf("content", "file")) {
            val writer = StringWriter()
            Xml.newSerializer().apply {
                setOutput(writer); startTag(null, "filter")
                PackageManagerHidden.filter(action, scheme).writeToXml(this)
                endTag(null, "filter"); flush()
            }
            val xml = writer.toString()
            assertEquals("$action/$scheme", RootSystemDefaultMain.knownPolicyKeyFromXml(component, xml))
            for (unknown in listOf(
                xml.replaceFirst("<filter", "<filter futurePolicy=\"true\""),
                xml.replaceFirst("<action", "<action futurePolicy=\"true\""),
                xml.replace("</filter>", "<future-policy name=\"keep-me\" /></filter>"),
            )) {
                assertNull("Unknown XML must be preserved even when IntentFilter ignores it", RootSystemDefaultMain.knownPolicyKeyFromXml(component, unknown))
            }
        }
    }

    @Test fun realSystemBridgeSetsIdempotentlyClearsAndPreservesPolicyWhenCancelledBeforeCommit() {
        assumeTrue("Opt in with rootSystemDefaults=true", InstrumentationRegistry.getArguments().getString("rootSystemDefaults") == "true")
        assumeTrue("This supervised probe is for API 24 x86", Build.VERSION.SDK_INT == 24 && Build.SUPPORTED_ABIS.first() == "x86")
        check(AuthorizerStates.request(context, Authorizer.ROOT, 30_000)) { "Explicit Root authorization is required" }
        val before = readSettled()
        check(before.ownedFilters == 0 && before.persistentMatches == 0) { "Pre-existing plugin persistent policy is protected" }
        var ownsPolicy = false
        try {
            cancelAtPrepared()
            assertEquals(0, readSettled().ownedFilters)
            ownsPolicy = true
            assertEquals(4, RootPersistentDefaults.set(context, true) {})
            val locked = readSettled()
            assertEquals(RootPersistentDefaultProtocol.State(4, 4, 4), locked)
            assertEquals(4, RootPersistentDefaults.set(context, true) {})
            duplicateKnownPolicies()
            val duplicated = readSettled()
            assertEquals(RootPersistentDefaultProtocol.State(4, 4, 8), duplicated)
            assertEquals(4, RootPersistentDefaults.set(context, true) {})
            assertEquals("Idempotent set must preserve the existing duplicate set without new writes", duplicated, readSettled())
            cancelAtPrepared()
            assertEquals("Cancellation must preserve the already-existing duplicate records", duplicated, readSettled())
            assertEquals(0, RootPersistentDefaults.set(context, false) {})
            ownsPolicy = false
            assertEquals(0, readSettled().ownedFilters)
            instrumentation.sendStatus(0, Bundle().apply {
                putString("root-system-default", "API=24 systemUidBridge=true persistent=4/4 normalResolve=4/4 idempotent=true duplicateFixture=8 duplicateSetPreserved=true cancelNew=true cancelExistingPreserved=true clearVerified=true")
            })
        } finally {
            if (ownsPolicy) RootPersistentDefaults.set(context, false) {}
        }
    }

    private fun duplicateKnownPolicies() {
        val token = UUID.randomUUID().toString().replace("-", "")
        val command = "CLASSPATH=${RootPersistentDefaultProtocol.quote(context.applicationInfo.sourceDir)} exec /system/bin/app_process /system/bin ${RootDuplicateDefaultFixture::class.java.name} $token"
        val output = RootShellAccess.submit(context, { shell ->
            check(shell.isRoot)
            val out = ArrayList<String>()
            val result = shell.newJob().add("su 1000 -c ${RootPersistentDefaultProtocol.quote(command)}").to(out).exec()
            check(result.isSuccess) { "The fixed duplicate policy fixture failed: $out" }
            out
        }).get(15, TimeUnit.SECONDS)
        assertTrue(output.toString(), output.contains("ROOT_DUPLICATE_FIXTURE_OK:$token:8"))
    }

    private fun cancelAtPrepared() {
        val root = File(context.noBackupFilesDir, "root-persistent-default")
        val existing = root.listFiles().orEmpty().map { it.name }.toSet()
        var observedPrepared = false
        val failure = assertThrows(InstallFailure::class.java) {
            RootPersistentDefaults.set(context, true) {
                val own = root.listFiles().orEmpty().filter { it.isDirectory && it.name !in existing }
                observedPrepared = own.any { directory ->
                    val output = File(directory, "output")
                    output.isFile && output.length() <= RootPersistentDefaultProtocol.MAX_OUTPUT &&
                        output.readText().contains("\"type\":\"prepared\"")
                }
                if (observedPrepared) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Owned test cancellation before commit")
            }
        }
        assertTrue("The isolated child never reached its prepared handshake", observedPrepared)
        assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
    }

    private fun readSettled(): RootPersistentDefaultProtocol.State {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (true) try {
            return RootPersistentDefaults.read(context) {}
        } catch (failure: InstallFailure) {
            if (!failure.message.orEmpty().contains("operation is still active") || SystemClock.elapsedRealtime() >= deadline) throw failure
            SystemClock.sleep(25) // Only read is retried while the previous child releases its lock.
        }
    }
}
