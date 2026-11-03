package com.programmersbox.supabaseintegration.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SupabaseShareFormattingTest {

    @Test
    fun projectHostDropsSchemeAndPath() {
        assertEquals("abcd.supabase.co", projectHost("https://abcd.supabase.co"))
        assertEquals("abcd.supabase.co", projectHost(" https://abcd.supabase.co/rest/v1 "))
        assertEquals("abcd.supabase.co", projectHost("abcd.supabase.co"))
    }

    @Test
    fun shortenKeyKeepsOnlyTheEnds() {
        val key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.signatureXk9Q"
        assertEquals("eyJhb…Xk9Q", shortenKey(key))
    }

    @Test
    fun shortenKeyHidesShortKeysCompletely() {
        val short = shortenKey("secret123")
        assertEquals("•••••••••".take(8), short)
        assertFalse(short.contains("sec"))
    }
}
