package com.programmersbox.kmpuiviews.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TranslationLanguagesTest {
    @Test
    fun `all has 59 unique codes`() {
        assertEquals(59, TranslationLanguages.all.size)
        assertEquals(59, TranslationLanguages.all.map { it.code }.toSet().size)
    }

    @Test
    fun `byCode finds japanese`() {
        assertEquals(TranslationLanguage("ja", "Japanese", "日本語", "日"), TranslationLanguages.byCode("ja"))
    }

    @Test
    fun `byCode unknown is null`() {
        assertNull(TranslationLanguages.byCode("xx"))
    }

    @Test
    fun `all is sorted by englishName`() {
        val names = TranslationLanguages.all.map { it.englishName }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `glyphs follow script rules`() {
        assertEquals("Es", TranslationLanguages.byCode("es")?.glyph)
        assertEquals("Р", TranslationLanguages.byCode("ru")?.glyph)
    }
}
