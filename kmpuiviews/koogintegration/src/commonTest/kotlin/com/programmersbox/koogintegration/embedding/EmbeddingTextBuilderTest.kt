package com.programmersbox.koogintegration.embedding

import com.programmersbox.favoritesdatabase.DbModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmbeddingTextBuilderTest {

    private fun model(description: String = "A ninja story") = DbModel(
        title = "Naruto",
        description = description,
        url = "https://example.com/naruto",
        imageUrl = "https://example.com/naruto.png",
        source = "ExampleSource",
        numChapters = 700,
        shouldCheckForUpdate = true,
    )

    @Test
    fun includesAllEmbeddedFieldsAndExcludesImageUrl() {
        val text = model().toEmbeddingText()!!
        assertTrue("Naruto" in text)
        assertTrue("A ninja story" in text)
        assertTrue("https://example.com/naruto" in text)
        assertTrue("ExampleSource" in text)
        assertTrue("700" in text)
        assertTrue("true" in text)
        assertTrue("naruto.png" !in text)
    }

    @Test
    fun returnsNullForEmptyDescription() {
        assertNull(model(description = "").toEmbeddingText())
        assertNull(model(description = "   ").toEmbeddingText())
    }

    @Test
    fun textIsDeterministic() {
        assertEquals(model().toEmbeddingText(), model().toEmbeddingText())
    }
}
