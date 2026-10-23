package android.graphics

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform

/** Draws onto a [Bitmap] through Java2D. Covers the calls descrambling extensions make. */
class Canvas(private var bitmap: Bitmap?) {
    private var graphics: Graphics2D? = bitmap?.let(::graphicsFor)
    private val saved = ArrayDeque<AffineTransform>()

    constructor() : this(null)

    fun setBitmap(bitmap: Bitmap?) {
        graphics?.dispose()
        this.bitmap = bitmap
        graphics = bitmap?.let(::graphicsFor)
    }

    fun getWidth(): Int = bitmap?.getWidth() ?: 0
    fun getHeight(): Int = bitmap?.getHeight() ?: 0

    fun drawBitmap(source: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        val s = src ?: Rect(0, 0, source.getWidth(), source.getHeight())
        g().drawImage(source.image, dst.left, dst.top, dst.right, dst.bottom, s.left, s.top, s.right, s.bottom, null)
    }

    fun drawBitmap(source: Bitmap, left: Float, top: Float, paint: Paint?) {
        val at = AffineTransform.getTranslateInstance(left.toDouble(), top.toDouble())
        g().drawImage(source.image, at, null)
    }

    /** Fills the whole bitmap, replacing its pixels (Android's default SRC_OVER differs only for translucent colors). */
    fun drawColor(color: Int) {
        val target = bitmap ?: return
        val g = g()
        val composite = g.composite
        val transform = g.transform
        g.transform = AffineTransform()
        g.composite = AlphaComposite.Src
        g.color = Color(color, true)
        g.fillRect(0, 0, target.getWidth(), target.getHeight())
        g.composite = composite
        g.transform = transform
    }

    fun save(): Int {
        saved.addLast(g().transform)
        return saved.size
    }

    fun restore() {
        g().transform = saved.removeLastOrNull() ?: throw IllegalStateException("Underflow in restore")
    }

    fun translate(dx: Float, dy: Float) = g().translate(dx.toDouble(), dy.toDouble())

    fun scale(sx: Float, sy: Float) = g().scale(sx.toDouble(), sy.toDouble())

    fun rotate(degrees: Float) = g().rotate(Math.toRadians(degrees.toDouble()))

    /** For other stubs that draw (e.g. text layouts). */
    internal fun graphics(): Graphics2D = g()

    private fun g(): Graphics2D = checkNotNull(graphics) { "Canvas has no bitmap" }

    private fun graphicsFor(bitmap: Bitmap): Graphics2D = bitmap.image.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    }
}
