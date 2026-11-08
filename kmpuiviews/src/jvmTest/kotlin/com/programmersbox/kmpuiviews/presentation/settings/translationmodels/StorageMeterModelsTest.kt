package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.domain.TranslationModel
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageMeterModelsTest {
    private fun downloaded(code: String, bytes: Long) = TranslationModel(
        language = TranslationLanguages.byCode(code)!!,
        state = ModelState.Downloaded,
        sizeBytes = bytes,
        sizeIsEstimate = false,
        required = code == "en",
        lastUsedMillis = null,
    )

    @Test
    fun requiredRowHasNoSize() {
        assertEquals(null, rowSizeText(downloaded("en", 0L)))
        assertEquals("27.8 MB", rowSizeText(downloaded("ja", 27_812_345L)))
    }

    @Test
    fun meterLeavesOutRequiredModels() {
        val rows = listOf(downloaded("en", 0L), downloaded("ja", 27_812_345L), downloaded("ko", 30_000_000L))
        assertEquals(listOf("ja", "ko"), meterModels(rows).map { it.language.code })
        assertEquals(emptyList(), meterModels(listOf(downloaded("en", 0L))))
    }
}
