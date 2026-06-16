package com.programmersbox.koogintegration.dynamictranslation.strategy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import java.io.ByteArrayOutputStream

class AndroidRenderStrategy : RenderStrategy {

    override suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray {
        if (!OpenCVLoader.initDebug()) {
            throw DynamicTranslationException("OpenCV failed to initialize")
        }

        val formatOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, formatOptions)
        val compressFormat = when {
            formatOptions.outMimeType?.contains("png", ignoreCase = true) == true ->
                Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }

        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw DynamicTranslationException("Failed to decode image for rendering")
        val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
        original.recycle()

        val mat = Mat()
        val bgrMat = Mat()
        val mask = Mat()
        val inpainted = Mat()
        val rgbaMat = Mat()
        try {
            Utils.bitmapToMat(mutable, mat)
            Imgproc.cvtColor(mat, bgrMat, Imgproc.COLOR_RGBA2BGR)

            Mat.zeros(bgrMat.size(), CvType.CV_8UC1).copyTo(mask)
            for (block in translations.blocks) {
                val b = block.bounds
                if (b.x >= 0 && b.y >= 0 &&
                    b.x + b.width <= bgrMat.width() && b.y + b.height <= bgrMat.height()
                ) {
                    mask.submat(Rect(b.x, b.y, b.width, b.height)).setTo(Scalar(255.0))
                }
            }

            Photo.inpaint(bgrMat, mask, inpainted, 3.0, Photo.INPAINT_TELEA)
            Imgproc.cvtColor(inpainted, rgbaMat, Imgproc.COLOR_BGR2RGBA)
            Utils.matToBitmap(rgbaMat, mutable)
        } finally {
            mat.release()
            bgrMat.release()
            mask.release()
            inpainted.release()
            rgbaMat.release()
        }

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val canvas = Canvas(mutable)
        for (block in translations.blocks) {
            val b = block.bounds
            val textWidth = paint.measureText(block.translated)
            val x = b.x + (b.width - textWidth) / 2f
            val y = b.y + b.height / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(block.translated, x, y, paint)
        }

        val quality = if (compressFormat == Bitmap.CompressFormat.PNG) 100 else 90
        val out = ByteArrayOutputStream()
        mutable.compress(compressFormat, quality, out)
        mutable.recycle()
        return out.toByteArray()
    }
}
