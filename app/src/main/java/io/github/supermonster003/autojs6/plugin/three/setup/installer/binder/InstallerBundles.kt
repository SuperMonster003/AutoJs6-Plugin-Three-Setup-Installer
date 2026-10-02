package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import android.os.Bundle
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.AdvancedInstallOptions
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

internal object InstallerBundles {
    @Suppress("DEPRECATION")
    fun request(bundle: Bundle?): String {
        if (bundle == null) throw RequestDocuments.invalid("Request bundle is missing")
        try {
            val version = bundle[InstallerContract.KEY_CONTRACT_VERSION] as? Int
                ?: throw RequestDocuments.invalid("contractVersion must be an integer")
            if (!InstallerContract.supportsContractVersion(version)) throw RequestDocuments.invalid("Unsupported installer contract version")
            val host = bundle[InstallerContract.KEY_HOST_VERSION_CODE] as? Long
                ?: throw RequestDocuments.invalid("hostVersionCode must be a long")
            if (host < ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION) throw RequestDocuments.invalid("Unsupported host version")
            val json = (bundle[InstallerContract.KEY_REQUEST_JSON] as? String)
                ?.takeIf { it.toByteArray(Charsets.UTF_8).size <= InstallerContract.MAX_JSON_BYTES }
                ?: throw RequestDocuments.invalid("Request JSON is missing or too large")
            if (version < 3) {
                val root = com.google.gson.JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
                if (root?.has(InstallerContract.FIELD_APPLY_SOURCE_PROFILES) == true) throw RequestDocuments.invalid("Source profiles require installer contract version 3")
                listOfNotNull(root, root?.get("options")?.takeIf { it.isJsonObject }?.asJsonObject).forEach { options ->
                    if (version < 2 && (options.get("authorizer")?.takeIf { it.isJsonPrimitive }?.asString == InstallerContract.AUTHORIZER_DHIZUKU ||
                        options.get("interaction")?.takeIf { it.isJsonPrimitive }?.asString == InstallerContract.INTERACTION_NOTIFICATION || options.has(InstallerContract.FIELD_MODE))) {
                        throw RequestDocuments.invalid("This option requires installer contract version 2")
                    }
                    if (options.keySet().any { it in AdvancedInstallOptions.keys }) throw RequestDocuments.invalid("Advanced installation options require installer contract version 3")
                }
            }
            return json
        } catch (failure: InstallFailure) { throw failure }
        catch (_: RuntimeException) { throw RequestDocuments.invalid("Malformed request bundle") }
    }

    fun hostId(bundle: Bundle?): String = runCatching {
        bundle?.getString(InstallerContract.KEY_HOST_SESSION_ID)?.takeIf { it.isNotBlank() && it.length <= 128 }
    }.getOrNull().orEmpty()

    fun document(key: String, document: JsonObject): Bundle {
        val json = document.toString()
        if (json.toByteArray(Charsets.UTF_8).size > InstallerContract.MAX_JSON_BYTES) {
            throw InstallFailure(InstallerErrorCodes.INTERNAL, "Installer response exceeds the JSON ceiling")
        }
        return Bundle().apply {
            // The result envelope itself is unchanged. V1 callers must continue to decode it.
            putInt(InstallerContract.KEY_CONTRACT_VERSION, InstallerContract.MIN_CONTRACT_VERSION)
            putString(key, json)
        }
    }

    fun error(failure: InstallFailure): Bundle = try { document(InstallerContract.KEY_ERROR_JSON, failure.toJson()) }
    catch (_: InstallFailure) { document(InstallerContract.KEY_ERROR_JSON,
        InstallFailure(failure.code, "Error details were omitted to fit the response limit", status = failure.status,
            packageName = failure.packageName?.takeIf { it.length <= InstallerContract.MAX_INSTALLER_PACKAGE_LENGTH }).toJson()) }
}
