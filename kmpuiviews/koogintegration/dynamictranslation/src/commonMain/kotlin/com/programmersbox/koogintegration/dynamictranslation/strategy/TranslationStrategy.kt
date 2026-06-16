package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import java.io.Closeable

interface TranslationStrategy : Closeable {
    suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult
    override fun close() {}
}
