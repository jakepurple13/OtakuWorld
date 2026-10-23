package android.graphics

import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Minimal AWT-backed Bitmap for extensions that descramble page images: create, measure, draw onto
 * via [Canvas], and compress. ImageIO has no WebP codec, so WEBP output is written as PNG.
 */
class Bitmap internal constructor(internal val image: BufferedImage) {
    private var recycled = false

    fun getWidth(): Int = image.width
    fun getHeight(): Int = image.height
    fun getConfig(): Config = Config.ARGB_8888
    fun isRecycled(): Boolean = recycled

    fun recycle() {
        recycled = true
    }

    fun getPixel(x: Int, y: Int): Int = image.getRGB(x, y)
    fun setPixel(x: Int, y: Int, color: Int) = image.setRGB(x, y, color)

    fun compress(format: CompressFormat, quality: Int, stream: OutputStream): Boolean = when (format) {
        CompressFormat.JPEG -> writeJpeg(quality, stream)
        else -> ImageIO.write(image, "png", stream)
    }

    private fun writeJpeg(quality: Int, stream: OutputStream): Boolean {
        // JPEG has no alpha channel.
        val rgb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        rgb.createGraphics().apply { drawImage(image, 0, 0, null); dispose() }
        val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull() ?: return false
        ImageIO.createImageOutputStream(stream).use { out ->
            writer.output = out
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality.coerceIn(0, 100) / 100f
            }
            writer.write(null, IIOImage(rgb, null, null), params)
            writer.dispose()
        }
        return true
    }

    enum class Config { ALPHA_8, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE, RGBA_1010102 }

    enum class CompressFormat { JPEG, PNG, WEBP, WEBP_LOSSY, WEBP_LOSSLESS }

    companion object {
        @JvmStatic
        fun createBitmap(width: Int, height: Int, config: Config): Bitmap =
            Bitmap(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))

        @JvmStatic
        fun createBitmap(source: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
            val copy = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            copy.createGraphics().apply { drawImage(source.image.getSubimage(x, y, width, height), 0, 0, null); dispose() }
            return Bitmap(copy)
        }
    }
}
