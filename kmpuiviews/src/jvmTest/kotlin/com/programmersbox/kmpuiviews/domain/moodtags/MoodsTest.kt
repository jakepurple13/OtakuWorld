package com.programmersbox.kmpuiviews.domain.moodtags

import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.domain.aitext.MoodInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoodsTest {
    @Test
    fun `twenty built in moods each with a label`() {
        assertEquals(20, Moods.builtIn.size)
        assertEquals(Moods.builtIn.size, Moods.builtIn.toSet().size)
        Moods.builtIn.forEach { assertTrue(Moods.label(it) != null, it) }
    }

    @Test
    fun `built in check ignores case`() {
        assertTrue(Moods.isBuiltIn("Found Family"))
        assertFalse(Moods.isBuiltIn("comfort read"))
    }

    @Test
    fun `custom names are cleaned`() {
        assertEquals("comfort read", Moods.cleanCustom("  comfort    read "))
        assertEquals(30, Moods.cleanCustom("x".repeat(50))?.length)
        assertNull(Moods.cleanCustom("   "))
    }

    @Test
    fun `mood prompt lists every choice`() {
        val prepared = AiFeature.MoodTags.prepare(MoodInput("Frieren", "An elf mage", listOf("cozy", "comfort read")))
        assertTrue("Frieren" in prepared)
        assertTrue("cozy, comfort read" in prepared)
        assertEquals(AiToggle.MoodTags, AiFeature.MoodTags.toggle)
    }

    @Test
    fun `mood finish splits and cleans`() {
        assertEquals("cozy\nfound family\ndark", AiFeature.MoodTags.finish("1. Cozy, Found Family\n- \"dark\"\ncozy"))
    }

    @Test
    fun `custom names drop list separators`() {
        assertEquals("rainy cozy", Moods.cleanCustom("rainy, cozy;"))
    }
}
