package com.programmersbox.kmpuiviews

import androidx.compose.ui.graphics.ImageBitmap
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ByteBoundedImageCacheTest {

    // 10x10 ARGB = 400 bytes each. A proxy, since JVM tests have no Compose graphics backend.
    private fun image(): ImageBitmap = Proxy.newProxyInstance(
        ImageBitmap::class.java.classLoader,
        arrayOf(ImageBitmap::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "getWidth", "getHeight" -> 10
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ImageBitmap

    @Test
    fun evictsLeastRecentlyUsedWhenOverBudget() {
        val cache = ByteBoundedImageCache(maxBytes = 1_000)
        cache["a"] = image()
        cache["b"] = image()
        cache["a"] // touch a so b is least recent
        cache["c"] = image()

        assertNotNull(cache["a"])
        assertNull(cache["b"])
        assertNotNull(cache["c"])
        assertEquals(2, cache.size)
    }

    @Test
    fun skipsImagesLargerThanBudget() {
        val cache = ByteBoundedImageCache(maxBytes = 300)
        cache["big"] = image()

        assertNull(cache["big"])
        assertEquals(0, cache.size)
    }

    @Test
    fun replacingAKeyDoesNotDoubleCountBytes() {
        val cache = ByteBoundedImageCache(maxBytes = 800)
        cache["a"] = image()
        cache["a"] = image()
        cache["b"] = image()

        assertNotNull(cache["a"])
        assertNotNull(cache["b"])
    }

    @Test
    fun removeAndClearFreeBudget() {
        val cache = ByteBoundedImageCache(maxBytes = 800)
        cache["a"] = image()
        cache["b"] = image()

        assertTrue(cache.remove("a"))
        assertFalse(cache.remove("a"))
        cache["c"] = image()
        assertNotNull(cache["b"])
        assertNotNull(cache["c"])

        cache.clear()
        assertEquals(0, cache.size)
        cache["d"] = image()
        cache["e"] = image()
        assertEquals(2, cache.size)
    }
}
