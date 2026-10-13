package com.programmersbox.mihon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MihonLibTest {
    @Test
    fun metadataLibWins() {
        assertEquals(1.6, MihonLib.libVersion(1.6f, "1.4.100"))
    }

    @Test
    fun fallsBackToVersionName() {
        assertEquals(1.4, MihonLib.libVersion(0f, "1.4.190"))
        assertEquals(1.4, MihonLib.libVersion("1.4.190"))
    }

    @Test
    fun unparseable() {
        assertNull(MihonLib.libVersion(0f, null))
        assertNull(MihonLib.libVersion("abc"))
    }

    @Test
    fun onlyExactVersionsSupported() {
        assertTrue(MihonLib.isSupported(1.4))
        assertTrue(MihonLib.isSupported(1.6))
        assertFalse(MihonLib.isSupported(1.3))
        assertFalse(MihonLib.isSupported(1.5))
        assertFalse(MihonLib.isSupported(1.7))
        assertFalse(MihonLib.isSupported(null))
    }
}
