package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.junit.Assert.*
import org.junit.Test

class RequestDocumentsTest {
    @Test fun `wrong optional field types cannot silently become defaults`() {
        for (options in listOf("{\"authorizer\":false}", "{\"user\":0}", "{\"allowDowngrade\":\"true\"}",
            "{\"timeoutMillis\":1.5}", "{\"timeoutMillis\":1e99}", "[]")) {
            invalid("""{"id":"x","sources":[{"displayName":"x.apk"}],"options":$options}""", 1)
        }
        invalid("""{"id":"x","sources":[{"item":2147483648,"displayName":"x.apk"}]}""", 1)
        invalid("""{"id":"x","sources":[{"size":0.5,"displayName":"x.apk"}]}""", 1)
    }

    @Test fun `rejects lenient JSON trailing documents and utf8 byte overflow`() {
        for (json in listOf("{id:'x'}", "{} {}", "{\"id\":/* comment */\"x\"}", "{\"id\":\"" + "中".repeat(22000) + "\"}")) invalid(json, 1)
    }

    @Test fun `batch and split ceilings count grouped items separately`() {
        fun json(items: List<Int>) = """{"id":"x","sources":[${items.joinToString { """{"item":$it,"displayName":"x.apk"}""" }}]}"""
        val batch = (0 until 32).toList()
        assertEquals(32, InstallRequest.parse(json(batch), 32).items.size)
        invalid(json((0 until 33).toList()), 33)
        assertEquals(64, InstallRequest.parse(json(List(64) { 7 }), 64).items.single().size)
        invalid(json(List(65) { 7 }), 65)
        invalid(json(listOf(0)), 2)
    }

    @Test fun `defaults preserve auto and continue after individual failure`() {
        val request = InstallRequest.parse("""{"id":"x","sources":[{"displayName":"x.apk"}]}""", 1)
        assertEquals("auto", request.interaction)
        assertEquals("auto", request.options.authorizer)
        assertEquals("current", request.options.user)
        assertTrue(request.options.continueOnError)
    }

    private fun invalid(json: String, count: Int) {
        assertEquals(json.take(80), "INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { InstallRequest.parse(json, count) }.code)
    }
}
