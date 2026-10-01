package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.widget.TextView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoveryPersistence
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoverySnapshot
import org.autojs.plugin.installer.api.InstallerContract
import java.io.File

/** DUMP-protected debug helper. Seeded results are synthetic; this never installs any package. */
class InstallRecoveryProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Recovery display probe (synthetic data only)" })
        dispatch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatch(intent)
    }

    private fun dispatch(input: Intent) {
        val caseId = input.getStringExtra("caseId")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) }
            ?: run { finish(); return }
        val mode = input.getStringExtra("mode") ?: "inspect"
        val application = applicationContext
        Thread({
            val evidence = File(application.cacheDir, "recovery-probe-$caseId.json")
            try {
                if (mode == "seed") {
                    check(!evidence.exists()) { "The probe case already exists" }
                    val request = InstallRequest("recovery-probe-$caseId", listOf(
                        SourceEntry(0, 0, "synthetic-confirmed.apk", 0), SourceEntry(1, 1, "synthetic-pending.apk", 0)),
                        InstallerContract.INTERACTION_DIALOG, InstallOptions())
                    val record = InstallPresentation.create(application, request, InstallPresentation.Callbacks(cancel = {}))
                    record.onStage(InstallerContract.STAGE_COMMITTING, InstallDocuments.stageDetail(1, "example.synthetic.pending"))
                    record.onItemResult(0, InstallDocuments.installResult("example.synthetic.confirmed", "2.0", 2, 1,
                        "none", "dialog", 0, false, emptyList()))
                    evidence.writeText(JsonObject().apply {
                        addProperty("synthetic", true); addProperty("seedPid", Process.myPid()); addProperty("token", record.token)
                        addProperty("seedElapsed", SystemClock.elapsedRealtime()); addProperty("mode", "seeded")
                    }.toString())
                    runOnUiThread { record.show(); finish() }
                    return@Thread
                }
                val previous = JsonParser.parseString(evidence.readText()).asJsonObject
                check(previous["synthetic"]?.asBoolean == true)
                val token = previous["token"].asString
                check(InstallRecoverySnapshot.validToken(token))
                if (mode == "clear") {
                    val live = InstallPresentation.find(token)
                    check(live == null || live.request.id == "recovery-probe-$caseId")
                    live?.close()
                    InstallRecoveryPersistence.remove(application, token) {
                        Thread {
                            previous.addProperty("mode", "cleared")
                            previous.addProperty("probePid", Process.myPid())
                            evidence.writeText(previous.toString())
                            runOnUiThread { finish() }
                        }.start()
                    }
                    return@Thread
                }
                check(mode == "inspect" || mode == "restore")
                val memoryPresent = InstallPresentation.find(token) != null
                InstallRecoveryPersistence.load(application, token) { snapshot ->
                    Thread {
                        previous.addProperty("mode", mode)
                        previous.addProperty("probePid", Process.myPid())
                        previous.addProperty("memoryRecord", memoryPresent)
                        previous.addProperty("snapshotFound", snapshot != null)
                        previous.addProperty("interrupted", snapshot?.terminal == false)
                        previous.add("knownOutcomes", JsonArray().apply { snapshot?.items?.forEach { add(it.ok?.toString() ?: "unknown") } })
                        evidence.writeText(previous.toString())
                        runOnUiThread {
                            if (mode == "restore") startActivity(Intent(this, InstallDialogActivity::class.java)
                                .putExtra(InstallPresentation.EXTRA_TOKEN, token)
                                .setData(Uri.parse("three-setup-install://session/$token"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT))
                            finish()
                        }
                    }.start()
                }
            } catch (failure: Exception) {
                File(application.cacheDir, "recovery-probe-$caseId-error.txt").writeText(failure.javaClass.simpleName + ": " + failure.message)
                runOnUiThread { finish() }
            }
        }, "recovery-probe").start()
    }
}
