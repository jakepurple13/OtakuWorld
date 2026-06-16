package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.BoundingBox
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrBlock
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class JvmOcrStrategy : OcrStrategy {

    override suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult {
        val image: BufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw DynamicTranslationException("Failed to decode image for OCR")

        val tesseract = Tesseract().apply {
            setDatapath(config.tessDataPath)
            setLanguage(config.sourceLanguage)
            setPageSegMode(3) // PSM_AUTO
        }

        val words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD)

        val blocks = words
            .filter { it.text.isNotBlank() }
            .map { word ->
                OcrBlock(
                    text = word.text.trim(),
                    bounds = BoundingBox(
                        x = word.boundingBox.x,
                        y = word.boundingBox.y,
                        width = word.boundingBox.width,
                        height = word.boundingBox.height,
                    ),
                )
            }

        return OcrResult(blocks)
    }
}
