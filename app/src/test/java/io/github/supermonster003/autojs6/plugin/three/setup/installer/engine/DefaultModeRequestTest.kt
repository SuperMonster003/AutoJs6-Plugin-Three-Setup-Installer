package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.junit.Assert.*
import org.junit.Test

class DefaultModeRequestTest {
    @Test fun `new mode is explicit and empty options preserve the legacy default`() {
        assertEquals(DefaultModeRequest("auto", "preferred"), DefaultModeRequest.parse("{}"))
        assertEquals(DefaultModeRequest("dhizuku", "persistent"),
            DefaultModeRequest.parse("""{"authorizer":"dhizuku","mode":"persistent"}"""))
    }

    @Test fun `malformed default options cannot trigger a different privileged operation`() {
        for (json in listOf("[]", "{} {}", "{mode:'persistent'}", """{"mode":true}""",
            """{"mode":null}""", """{"mode":"unknown"}""", """{"authorizer":3}""",
            """{"authorizer":"system"}""", """{"enable":true}""", " ".repeat(65_537))) {
            assertEquals(json.take(80), "INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) {
                DefaultModeRequest.parse(json)
            }.code)
        }
    }
}
