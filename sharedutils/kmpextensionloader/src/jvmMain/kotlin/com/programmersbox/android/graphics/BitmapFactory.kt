package android.graphics

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.imageio.ImageIO

/** Decodes what ImageIO can read (PNG, JPEG, GIF, BMP; not WebP). Returns null otherwise, like Android. */
object BitmapFactory {
    @JvmStatic
    fun decodeStream(stream: InputStream?): Bitmap? = stream?.let { ImageIO.read(it) }?.let { Bitmap(toArgb(it)) }

    @JvmStatic
    fun decodeByteArray(data: ByteArray, offset: Int, length: Int): Bitmap? =
        decodeStream(ByteArrayInputStream(data, offset, length))

    private fun toArgb(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_ARGB) return image
        val argb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        argb.createGraphics().apply { drawImage(image, 0, 0, null); dispose() }
        return argb
    }
}
