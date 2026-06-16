package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult

interface OcrStrategy {
    suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult
}
