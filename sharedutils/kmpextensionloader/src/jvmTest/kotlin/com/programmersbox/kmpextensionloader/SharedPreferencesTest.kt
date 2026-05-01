package com.programmersbox.kmpextensionloader

import android.content.SharedPreferences
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedPreferencesTest {
    private lateinit var tmpDir: File
    private lateinit var prefs: SharedPreferences

    @BeforeTest fun setUp() {
        tmpDir = Files.createTempDirectory("prefs-test").toFile()
        prefs = SharedPreferences(File(tmpDir, "test.properties"))
    }

    @AfterTest fun tearDown() { tmpDir.deleteRecursively() }

    @Test fun `getString returns null when key absent`() {
        assertNull(prefs.getString("k", null))
    }

    @Test fun `putString persists after edit commit`() {
        prefs.edit().putString("name", "alice").commit()
        val fresh = SharedPreferences(File(tmpDir, "test.properties"))
        assertEquals("alice", fresh.getString("name", null))
    }

    @Test fun `putInt round-trips`() {
        prefs.edit().putInt("count", 7).apply()
        assertEquals(7, SharedPreferences(File(tmpDir, "test.properties")).getInt("count", 0))
    }

    @Test fun `putBoolean round-trips`() {
        prefs.edit().putBoolean("flag", true).apply()
        assertTrue(SharedPreferences(File(tmpDir, "test.properties")).getBoolean("flag", false))
    }

    @Test fun `remove deletes key`() {
        prefs.edit().putString("x", "y").commit()
        prefs.edit().remove("x").commit()
        assertNull(SharedPreferences(File(tmpDir, "test.properties")).getString("x", null))
    }

    @Test fun `clear removes all keys`() {
        prefs.edit().putString("a", "1").putString("b", "2").commit()
        prefs.edit().clear().commit()
        val fresh = SharedPreferences(File(tmpDir, "test.properties"))
        assertNull(fresh.getString("a", null))
        assertNull(fresh.getString("b", null))
    }

    @Test fun `contains returns false when absent`() {
        assertFalse(prefs.contains("missing"))
    }

    @Test fun `contains returns true after put`() {
        prefs.edit().putString("k", "v").commit()
        assertTrue(SharedPreferences(File(tmpDir, "test.properties")).contains("k"))
    }
}
