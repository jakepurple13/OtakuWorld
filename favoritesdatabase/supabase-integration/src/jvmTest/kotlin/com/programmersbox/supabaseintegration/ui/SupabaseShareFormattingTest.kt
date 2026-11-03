package com.programmersbox.supabaseintegration.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseShareFormattingTest {

    @Test
    fun projectHostDropsSchemeAndPath() {
        assertEquals("abcd.supabase.co", projectHost("https://abcd.supabase.co"))
        assertEquals("abcd.supabase.co", projectHost(" https://abcd.supabase.co/rest/v1 "))
        assertEquals("abcd.supabase.co", projectHost("abcd.supabase.co"))
    }

    @Test
    fun maskKeyHidesTheMiddleOfLongKeys() {
        val key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.signatureXk9Q"
        val masked = maskKey(key)
        assertTrue(masked.startsWith("eyJhbGciOi"))
        assertFalse(masked.contains("payload"))
    }

    @Test
    fun maskKeyHidesShortKeysCompletely() {
        assertEquals("•••••••••", maskKey("secret123"))
    }
}
