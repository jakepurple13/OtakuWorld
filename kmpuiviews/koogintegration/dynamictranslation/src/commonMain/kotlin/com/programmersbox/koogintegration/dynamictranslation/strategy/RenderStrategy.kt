package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult

interface RenderStrategy {
    suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray
}
