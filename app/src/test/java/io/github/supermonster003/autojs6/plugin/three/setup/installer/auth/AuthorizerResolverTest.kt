package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthorizerResolverTest {

    private fun state(authorizer: Authorizer, available: Boolean, running: Boolean = available, granted: Boolean = running) =
        AuthorizerState(authorizer, available, running, granted)

    private val none = state(Authorizer.NONE, available = true)

    @Test
    fun `auto walks the order and takes the first usable privileged authorizer`() {
        val states = mapOf(
            Authorizer.SHIZUKU to state(Authorizer.SHIZUKU, available = true),
            Authorizer.ROOT to state(Authorizer.ROOT, available = true),
            Authorizer.DHIZUKU to state(Authorizer.DHIZUKU, available = true),
            Authorizer.NONE to none,
        )
        assertEquals(Authorizer.SHIZUKU, AuthorizerResolver.resolve("auto", states))
        assertEquals(Authorizer.SHIZUKU, AuthorizerResolver.resolve(null, states))
        assertEquals(Authorizer.ROOT, AuthorizerResolver.resolve("auto", states, order = listOf(Authorizer.ROOT, Authorizer.SHIZUKU)))
        assertEquals(Authorizer.ROOT, AuthorizerResolver.resolve("auto", states, enabled = setOf(Authorizer.ROOT, Authorizer.NONE)))
        assertEquals(Authorizer.DHIZUKU, AuthorizerResolver.resolve("auto", states, enabled = setOf(Authorizer.DHIZUKU, Authorizer.NONE)))
    }

    @Test
    fun `auto ends at none without prompting when nothing privileged is usable`() {
        val states = mapOf(
            Authorizer.SHIZUKU to state(Authorizer.SHIZUKU, available = true, running = true, granted = false),
            Authorizer.ROOT to state(Authorizer.ROOT, available = false),
            Authorizer.DHIZUKU to state(Authorizer.DHIZUKU, available = true, running = false),
            Authorizer.NONE to none,
        )
        assertEquals(Authorizer.NONE, AuthorizerResolver.resolve("auto", states))
        val failure = assertThrows(InstallFailure::class.java) {
            AuthorizerResolver.resolve("auto", states, enabled = setOf(Authorizer.SHIZUKU))
        }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
    }

    @Test
    fun `an explicit authorizer never falls back`() {
        val states = mapOf(
            Authorizer.SHIZUKU to state(Authorizer.SHIZUKU, available = true, running = false, granted = false),
            Authorizer.ROOT to state(Authorizer.ROOT, available = true, running = true, granted = false),
            Authorizer.NONE to none,
        )
        assertEquals(Authorizer.NONE, AuthorizerResolver.resolve("none", states))
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) { AuthorizerResolver.resolve("shizuku", states) }.code)
        assertEquals(InstallerErrorCodes.AUTHORIZER_DENIED, assertThrows(InstallFailure::class.java) { AuthorizerResolver.resolve("root", states) }.code)
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) { AuthorizerResolver.resolve("dhizuku", states) }.code)
        assertEquals(InstallerErrorCodes.AUTHORIZER_DENIED, assertThrows(InstallFailure::class.java) {
            AuthorizerResolver.resolve("dhizuku", states + (Authorizer.DHIZUKU to state(Authorizer.DHIZUKU, true, granted = false)))
        }.code)
        assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, assertThrows(InstallFailure::class.java) { AuthorizerResolver.resolve("adb", states) }.code)
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) { AuthorizerResolver.resolve("root", mapOf(Authorizer.NONE to none)) }.code)
    }

    @Test
    fun `identifiers match the contract and auto is not an authorizer`() {
        assertEquals(listOf("none", "shizuku", "root", "dhizuku"), Authorizer.entries.map { it.id })
        assertEquals(listOf(Authorizer.SHIZUKU, Authorizer.ROOT, Authorizer.DHIZUKU, Authorizer.NONE), Authorizer.DEFAULT_ORDER)
        assertEquals(null, Authorizer.fromId("auto"))
        assertEquals(true, Authorizer.isAuto("auto"))
        assertEquals(false, Authorizer.NONE.privileged)
        assertEquals(true, Authorizer.SHIZUKU.privileged && Authorizer.ROOT.privileged && Authorizer.DHIZUKU.privileged)
    }

    @Test
    fun `explicit script choices take priority over disabled and reordered settings`() {
        val states = Authorizer.entries.associateWith { state(it, available = true) }
        for (authorizer in Authorizer.entries) {
            assertEquals(authorizer, AuthorizerResolver.resolve(authorizer.id, states,
                order = listOf(Authorizer.NONE), enabled = emptySet()))
        }
        assertEquals(Authorizer.NONE, AuthorizerResolver.resolve("auto", states,
            order = listOf(Authorizer.NONE, Authorizer.ROOT, Authorizer.SHIZUKU)))
    }

    @Test
    fun `decision table covers settings order enabled subsets and every meaningful privileged state`() {
        val choices = Authorizer.entries
        fun permutations(rest: List<Authorizer>): List<List<Authorizer>> = if (rest.isEmpty()) listOf(emptyList())
            else rest.flatMap { first -> permutations(rest - first).map { listOf(first) + it } }
        val orders = permutations(choices)
        assertEquals(24, orders.size)
        fun statesFor(authorizer: Authorizer) = listOf(
            state(authorizer, available = false),
            state(authorizer, available = true, running = false),
            state(authorizer, available = true, running = true, granted = false),
            state(authorizer, available = true),
        )
        for (shizuku in statesFor(Authorizer.SHIZUKU)) for (root in statesFor(Authorizer.ROOT)) for (dhizuku in statesFor(Authorizer.DHIZUKU)) {
            val states = mapOf(Authorizer.SHIZUKU to shizuku, Authorizer.ROOT to root, Authorizer.DHIZUKU to dhizuku, Authorizer.NONE to none)
            for (selected in listOf(shizuku, root, dhizuku)) {
                if (selected.usable) assertEquals(selected.authorizer,
                    AuthorizerResolver.resolve(selected.authorizer.id, states, order = listOf(Authorizer.NONE), enabled = emptySet()))
                else assertEquals(if (selected.available && selected.running) InstallerErrorCodes.AUTHORIZER_DENIED
                    else InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) {
                        AuthorizerResolver.resolve(selected.authorizer.id, states, order = listOf(Authorizer.NONE), enabled = emptySet())
                    }.code)
            }
            for (order in orders) for (mask in 0 until (1 shl choices.size)) {
                val enabled = choices.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet()
                val expected = order.firstOrNull { it in enabled && states.getValue(it).usable }
                if (expected != null) assertEquals(expected, AuthorizerResolver.resolve("auto", states, order, enabled))
                else assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) {
                    AuthorizerResolver.resolve("auto", states, order, enabled)
                }.code)
            }
        }
    }
}
