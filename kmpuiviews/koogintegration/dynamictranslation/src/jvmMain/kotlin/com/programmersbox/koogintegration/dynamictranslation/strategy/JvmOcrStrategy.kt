package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.BoundingBox
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrBlock
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import io.github.mymonstercat.ocr.InferenceEngine
import io.github.mymonstercat.ocr.config.ParamConfig
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class JvmOcrStrategy : OcrStrategy {

    override suspend fun extract(
        imageBytes: ByteArray,
        config: DynamicTranslationConfig,
    ): OcrResult {

        // 1. Initialize the engine (it will automatically load the ONNX models)
        val engine = InferenceEngine.getInstance(io.github.mymonstercat.Model.ONNX_PPOCR_V4)

        // 1. Create and configure the OCR Parameters
        val param = ParamConfig()

        // 1. The Bounding Box Expansion (Crucial for Vertical Text)
        // Default is usually 1.5. Increasing this forces the AI to expand the bounding
        // boxes further outward. This helps group the vertical characters (and furigana)
        // into a single cohesive block instead of fragmenting them.
        param.unClipRatio = 2.5f

        // 2. Lower the detection threshold significantly
        // Because the furigana makes the text look "messy" to the AI, it will have low
        // confidence. We must force it to accept lower-confidence hits.
        param.boxScoreThresh = 0.2f

        // 3. Force direction classification
        // Essential for vertical Japanese text.
        param.isDoAngle = true

        // 4. Do not shrink the image
        // Manga relies on crisp pixels for Kanji strokes. If the engine scales it down,
        // the furigana bleeds into the Kanji and becomes an illegible blob.
        param.maxSideLen = 4096

        // 2. Run the OCR inference
        val ocrResult = engine.runOcr(
            "/Users/jacobrein/Downloads/ruri.jpeg",
            param,
        )

        // 3. Iterate over the detected text blocks
        val i = ocrResult
            .textBlocks
            .also { println("Detected Text Blocks: ${it.size}") }
            .map { block ->
                val text = block.text
                val confidence = block.boxScore

                // RapidOCR returns exact pixel coordinates for the 4 corners of the polygon.
                // It returns them in this order: Top-Left, Top-Right, Bottom-Right, Bottom-Left
                val topLeft = block.boxPoint[0]
                val boxWidth = block.boxPoint[1].x - topLeft.x
                val boxHeight = block.boxPoint[1].y - topLeft.y

                val x = topLeft.x
                val y = topLeft.y

                println("Text: $text, Confidence: $confidence, Box: ($x, $y, $boxWidth, $boxHeight)")

                OcrBlock(
                    text = text.trim(),
                    bounds = BoundingBox(
                        x = x,
                        y = y,
                        width = boxWidth,
                        height = boxHeight,
                    ),
                )
            }

        return OcrResult(i)

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
