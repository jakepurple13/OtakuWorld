package com.programmersbox.aniyomi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AniyomiLibTest {
    @Test
    fun libVersion_fromMetadataThenVersionName() {
        assertEquals(17.0, AniyomiLib.libVersion(17, "16.1"))
        assertEquals(16.0, AniyomiLib.libVersion(0, "16.55"))
        val fallback = AniyomiLib.libVersion(0, "15.3")
        assertEquals(15.0, fallback)
        assertFalse(AniyomiLib.isSupported(fallback))
    }

    @Test
    fun unparseable() {
        assertNull(AniyomiLib.libVersion(0, null))
        assertNull(AniyomiLib.libVersion("abc"))
    }

    @Test
    fun onlyExactVersionsSupported() {
        listOf(14.0, 16.0, 17.0).forEach { assertTrue(AniyomiLib.isSupported(it), "$it") }
        listOf(13.0, 15.0, 18.0).forEach { assertFalse(AniyomiLib.isSupported(it), "$it") }
        assertFalse(AniyomiLib.isSupported(null))
    }
}
