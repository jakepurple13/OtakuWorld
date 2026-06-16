package com.programmersbox.koogintegration.dynamictranslation.tool

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationOutput
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.strategy.OcrStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.RenderStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.TranslationStrategy
import java.io.Closeable

class TranslateTool(
    private val ocr: OcrStrategy,
    private val translation: TranslationStrategy,
    private val render: RenderStrategy,
) : Closeable {

    suspend fun execute(imageBytes: ByteArray, config: DynamicTranslationConfig): DynamicTranslationOutput {
        val ocrResult = try {
            ocr.extract(imageBytes, config)
        } catch (e: Exception) {
            throw DynamicTranslationException("OCR failed: ${e.message}", e)
        }

        if (ocrResult.blocks.isEmpty()) {
            return DynamicTranslationOutput(imageBytes = imageBytes, translations = emptyList())
        }

        val translationResult = try {
            translation.translate(ocrResult, config)
        } catch (e: Exception) {
            throw DynamicTranslationException("Translation failed: ${e.message}", e)
        }

        val renderedImage = try {
            render.render(imageBytes, translationResult, config)
        } catch (e: Exception) {
            throw DynamicTranslationException("Render failed: ${e.message}", e)
        }

        return DynamicTranslationOutput(
            imageBytes = renderedImage,
            translations = translationResult.blocks,
        )
    }

    override fun close() {
        translation.close()
    }
}
