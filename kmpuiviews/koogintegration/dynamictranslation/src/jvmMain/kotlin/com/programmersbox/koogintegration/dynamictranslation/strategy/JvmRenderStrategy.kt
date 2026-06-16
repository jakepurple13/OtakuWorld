package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import nu.pattern.OpenCV
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.photo.Photo
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class JvmRenderStrategy : RenderStrategy {

    init {
        OpenCV.loadLocally()
    }

    override suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray {
        val format = detectFormat(imageBytes)
        val original = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw DynamicTranslationException("Failed to decode image for rendering")

        val mat = bufferedImageToMat(original)
        val mask = Mat.zeros(mat.size(), CvType.CV_8UC1)
        val inpainted = Mat()
        val result: BufferedImage
        try {
            for (block in translations.blocks) {
                val b = block.bounds
                if (b.x >= 0 && b.y >= 0 && b.x + b.width <= mat.width() && b.y + b.height <= mat.height()) {
                    mask.submat(Rect(b.x, b.y, b.width, b.height)).setTo(Scalar(255.0))
                }
            }
            Photo.inpaint(mat, mask, inpainted, 3.0, Photo.INPAINT_TELEA)
            result = matToBufferedImage(inpainted)
        } finally {
            mat.release()
            mask.release()
            inpainted.release()
        }

        val g2d = result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            color = java.awt.Color.BLACK
            font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        }
        try {
            for (block in translations.blocks) {
                val b = block.bounds
                val fm = g2d.fontMetrics
                val textX = b.x + (b.width - fm.stringWidth(block.translated)) / 2
                val textY = b.y + (b.height + fm.ascent - fm.descent) / 2
                g2d.drawString(block.translated, textX, textY)
            }
        } finally {
            g2d.dispose()
        }

        val out = ByteArrayOutputStream()
        ImageIO.write(result, format, out)
        return out.toByteArray()
    }

    private fun detectFormat(bytes: ByteArray): String = when {
        bytes.size >= 4 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpeg"
        else -> throw DynamicTranslationException("Unsupported image format — only PNG and JPEG are supported")
    }

    private fun bufferedImageToMat(image: BufferedImage): Mat {
        val bgr = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)
        val g = bgr.createGraphics()
        try {
            g.drawImage(image, 0, 0, null)
        } finally {
            g.dispose()
        }
        val mat = Mat(bgr.height, bgr.width, CvType.CV_8UC3)
        mat.put(0, 0, (bgr.raster.dataBuffer as DataBufferByte).data)
        return mat
    }

    private fun matToBufferedImage(mat: Mat): BufferedImage {
        val image = BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR)
        val data = ByteArray(mat.width() * mat.height() * mat.channels())
        mat.get(0, 0, data)
        image.raster.setDataElements(0, 0, mat.width(), mat.height(), data)
        return image
    }
}
