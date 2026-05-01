package com.programmersbox.kmpextensionloader

import android.os.Bundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BundleTest {
    @Test fun `putString and getString round-trip`() {
        val b = Bundle()
        b.putString("key", "value")
        assertEquals("value", b.getString("key"))
    }

    @Test fun `getString returns null for missing key`() {
        assertNull(Bundle().getString("missing"))
    }

    @Test fun `getString returns default for missing key`() {
        assertEquals("default", Bundle().getString("missing", "default"))
    }

    @Test fun `putInt and getInt round-trip`() {
        val b = Bundle()
        b.putInt("n", 42)
        assertEquals(42, b.getInt("n"))
    }

    @Test fun `getInt returns 0 for missing key`() {
        assertEquals(0, Bundle().getInt("missing"))
    }

    @Test fun `putBoolean and getBoolean round-trip`() {
        val b = Bundle()
        b.putBoolean("flag", true)
        assertTrue(b.getBoolean("flag"))
    }

    @Test fun `containsKey returns false when missing`() {
        assertFalse(Bundle().containsKey("x"))
    }

    @Test fun `containsKey returns true after put`() {
        val b = Bundle()
        b.putString("x", "y")
        assertTrue(b.containsKey("x"))
    }

    @Test fun `remove deletes key`() {
        val b = Bundle()
        b.putString("k", "v")
        b.remove("k")
        assertNull(b.getString("k"))
    }
}
