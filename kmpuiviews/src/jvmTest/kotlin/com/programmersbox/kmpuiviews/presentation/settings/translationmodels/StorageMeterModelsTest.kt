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
    fun meterIncludesEnglishFirst() {
        val rows = listOf(downloaded("ja", 27_812_345L), downloaded("en", 0L), downloaded("ko", 30_000_000L))
        assertEquals(listOf("en", "ja", "ko"), meterModels(rows).map { it.language.code })
        assertEquals(listOf("en"), meterModels(listOf(downloaded("en", 0L))).map { it.language.code })
    }

    @Test
    fun englishGetsAVisibleFixedShare() {
        val total = 57_812_345L
        // 8% of the total, not its 0 bytes.
        assertEquals(total * 0.08f / 1_000_000f, meterWeight(downloaded("en", 0L), total), 0.0001f)
        // Floor so it still shows (and fills the bar alone) when little else is downloaded.
        assertEquals(2f, meterWeight(downloaded("en", 0L), 0L))
        assertEquals(27.812345f, meterWeight(downloaded("ja", 27_812_345L), total), 0.0001f)
    }
}
