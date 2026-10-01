package io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps

import org.junit.Assert.*
import org.junit.Test

class BoundedIconCacheTest {
    @Test fun `visible icons are retained ahead of least recently accessed icons`() {
        val cache = BoundedIconCache<String, String>(6, String::length)
        cache.put("a", "aaa")
        cache.put("b", "bb")
        assertEquals("aaa", cache["a"])
        cache.put("c", "ccc")
        assertNull(cache["b"])
        assertEquals("aaa", cache["a"])
        assertEquals("ccc", cache["c"])
        assertEquals(6, cache.weight())
    }

    @Test fun `replacement and oversized icons cannot exceed the byte budget`() {
        val cache = BoundedIconCache<String, String>(4, String::length)
        cache.put("a", "aaa")
        cache.put("a", "a")
        cache.put("b", "bbb")
        assertEquals(4, cache.weight())
        cache.put("large", "12345")
        assertNull(cache["large"])
        assertEquals(4, cache.weight())
        cache.put("b", "12345")
        assertNull(cache["b"])
        assertEquals(1, cache.weight())
        assertEquals(1, cache.size())
    }

    @Test fun `close style clearing drops all cached values`() {
        val cache = BoundedIconCache<String, String>(10, String::length)
        cache.put("a", "aaa")
        cache.clear()
        assertNull(cache["a"])
        assertEquals(0, cache.size())
        assertEquals(0, cache.weight())
        cache.put("b", "b")
        assertEquals("b", cache["b"])
    }

    @Test fun `invalid weights cannot create an unbounded zero weight cache`() {
        assertThrows(IllegalArgumentException::class.java) { BoundedIconCache<String, String>(0, String::length) }
        val cache = BoundedIconCache<String, String>(1, String::length)
        assertThrows(IllegalArgumentException::class.java) { cache.put("empty", "") }
        assertEquals(0, cache.size())
    }
}
