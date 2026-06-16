package com.programmersbox.koogintegration.dynamictranslation.strategy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import java.io.ByteArrayOutputStream

class AndroidRenderStrategy : RenderStrategy {

    override suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray {
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return imageBytes

        val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }

        for (block in translations.blocks) {
            val b = block.bounds
            val rect = Rect(b.x, b.y, b.x + b.width, b.y + b.height)
            canvas.drawRect(rect, backgroundPaint)
            // Draw translated text clipped to the original bounding box
            canvas.drawText(
                block.translated,
                b.x.toFloat(),
                (b.y + b.height).toFloat(),
                textPaint,
            )
        }

        val output = ByteArrayOutputStream()
        mutable.compress(Bitmap.CompressFormat.PNG, 100, output)
        mutable.recycle()
        return output.toByteArray()
    }
}
