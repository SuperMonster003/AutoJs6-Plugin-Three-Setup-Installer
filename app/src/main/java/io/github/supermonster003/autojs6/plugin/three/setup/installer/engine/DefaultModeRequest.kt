package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.autojs.plugin.installer.api.InstallerContract

internal data class DefaultModeRequest(val authorizer: String, val mode: String) {
    companion object {
        fun parse(json: String): DefaultModeRequest {
            if (json.toByteArray(Charsets.UTF_8).size > InstallerContract.MAX_JSON_BYTES) throw RequestDocuments.invalid("Default request is too large")
            val value = RequestDocuments.parseObject(json, "default request")
            if (value.keySet().any { it !in setOf("authorizer", "mode") }) throw RequestDocuments.invalid("Unknown default request field")
            fun string(key: String, fallback: String): String {
                val item = value.get(key) ?: return fallback
                if (!item.isJsonPrimitive || !item.asJsonPrimitive.isString) throw RequestDocuments.invalid("$key must be a string")
                return item.asString
            }
            val authorizer = string("authorizer", InstallerContract.AUTHORIZER_AUTO)
            if (!InstallerContract.isAuthorizer(authorizer)) throw RequestDocuments.invalid("Unknown authorizer")
            val mode = string("mode", InstallerContract.DEFAULT_MODE_PREFERRED)
            if (mode !in setOf(InstallerContract.DEFAULT_MODE_PREFERRED, InstallerContract.DEFAULT_MODE_PERSISTENT)) throw RequestDocuments.invalid("Unknown default mode")
            return DefaultModeRequest(authorizer, mode)
        }
    }
}
