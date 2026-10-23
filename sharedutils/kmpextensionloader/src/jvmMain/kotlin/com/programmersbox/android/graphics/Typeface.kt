package android.graphics

/** Family plus style; [android.text.StaticLayout] maps it onto a Java2D font. */
class Typeface private constructor(internal val family: String, private val style: Int) {
    fun getStyle(): Int = style
    fun isBold(): Boolean = style and BOLD != 0
    fun isItalic(): Boolean = style and ITALIC != 0

    companion object {
        const val NORMAL = 0
        const val BOLD = 1
        const val ITALIC = 2
        const val BOLD_ITALIC = 3

        @JvmField val DEFAULT = Typeface("SansSerif", NORMAL)
        @JvmField val DEFAULT_BOLD = Typeface("SansSerif", BOLD)
        @JvmField val SANS_SERIF = Typeface("SansSerif", NORMAL)
        @JvmField val SERIF = Typeface("Serif", NORMAL)
        @JvmField val MONOSPACE = Typeface("Monospaced", NORMAL)

        @JvmStatic fun create(family: Typeface?, style: Int): Typeface = Typeface((family ?: DEFAULT).family, style)
        @JvmStatic fun defaultFromStyle(style: Int): Typeface = Typeface(DEFAULT.family, style)
    }
}
