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

    @Test fun `explicit batch preserves a single application including a split set`() {
        assertTrue(InstallRequest.parse("""{"id":"one","batch":true,"sources":[{"displayName":"one.apk"}]}""", 1).isBatch)
        val splitSet = """{"id":"split","batch":true,"sources":[{"item":0,"displayName":"base.apk"},{"item":0,"displayName":"config.apk"}]}"""
        val request = InstallRequest.parse(splitSet, 2)
        assertTrue(request.isBatch)
        assertEquals(1, request.items.size)
        assertFalse(InstallRequest.parse("""{"id":"one","batch":false,"sources":[{"displayName":"one.apk"}]}""", 1).isBatch)
    }

    @Test fun `legacy requests without batch retain the grouped item inference`() {
        assertFalse(InstallRequest.parse("""{"id":"one","sources":[{"displayName":"one.apk"}]}""", 1).isBatch)
        assertFalse(InstallRequest.parse("""{"id":"split","sources":[{"item":7,"displayName":"base.apk"},{"item":7,"displayName":"config.apk"}]}""", 2).isBatch)
        assertTrue(InstallRequest.parse("""{"id":"many","sources":[{"displayName":"one.apk"},{"displayName":"two.apk"}]}""", 2).isBatch)
    }

    @Test fun `batch rejects non boolean values and false with multiple applications`() {
        for (value in listOf("\"true\"", "1", "null", "[]", "{}")) {
            invalid("""{"id":"one","batch":$value,"sources":[{"displayName":"one.apk"}]}""", 1)
        }
        invalid("""{"id":"many","batch":false,"sources":[{"displayName":"one.apk"},{"displayName":"two.apk"}]}""", 2)
    }

    private fun invalid(json: String, count: Int) {
        assertEquals(json.take(80), "INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { InstallRequest.parse(json, count) }.code)
    }
}
