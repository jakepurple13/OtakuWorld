package com.programmersbox.koogintegration.dynamictranslation.strategy

import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import com.programmersbox.koogintegration.dynamictranslation.model.BoundingBox
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrBlock
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult

class AndroidOcrStrategy : OcrStrategy {

    override suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw DynamicTranslationException("Failed to decode image for OCR")

        try {
            val api = TessBaseAPI()
            if (!api.init(config.tessDataPath, config.sourceLanguage)) {
                throw DynamicTranslationException(
                    "Tesseract init failed — check tessDataPath '${config.tessDataPath}' " +
                        "contains tessdata/${config.sourceLanguage}.traineddata"
                )
            }

            try {
                api.setImage(bitmap)
                api.getHOCRText(0) // trigger recognition

                val iterator = api.resultIterator
                    ?: return OcrResult(emptyList())

                val blocks = mutableListOf<OcrBlock>()
                iterator.begin()
                do {
                    val word = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                        ?.trim() ?: continue
                    if (word.isBlank()) continue

                    val bounds = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                    blocks.add(
                        OcrBlock(
                            text = word,
                            bounds = BoundingBox(
                                x = bounds.left,
                                y = bounds.top,
                                width = bounds.right - bounds.left,
                                height = bounds.bottom - bounds.top,
                            ),
                        )
                    )
                } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))

                iterator.delete()
                return OcrResult(blocks)
            } finally {
                api.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
