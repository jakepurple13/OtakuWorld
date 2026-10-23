package android.graphics

/** Only what bitmap drawing needs, plus the text size and typeface [android.text.StaticLayout] reads. */
open class Paint() {
    private var flags: Int = 0
    private var color: Int = 0xFF000000.toInt()

    constructor(flags: Int) : this() {
        this.flags = flags
    }

    fun getFlags(): Int = flags
    fun setFlags(flags: Int) {
        this.flags = flags
    }

    fun isAntiAlias(): Boolean = flags and ANTI_ALIAS_FLAG != 0
    fun setAntiAlias(aa: Boolean) {
        flags = if (aa) flags or ANTI_ALIAS_FLAG else flags and ANTI_ALIAS_FLAG.inv()
    }

    fun isFilterBitmap(): Boolean = flags and FILTER_BITMAP_FLAG != 0
    fun setFilterBitmap(filter: Boolean) {
        flags = if (filter) flags or FILTER_BITMAP_FLAG else flags and FILTER_BITMAP_FLAG.inv()
    }

    fun getColor(): Int = color
    fun setColor(color: Int) {
        this.color = color
    }

    private var textSize: Float = 12f
    private var typeface: Typeface? = null

    fun getTextSize(): Float = textSize
    fun setTextSize(textSize: Float) {
        this.textSize = textSize
    }

    fun getTypeface(): Typeface? = typeface
    fun setTypeface(typeface: Typeface?): Typeface? {
        this.typeface = typeface
        return typeface
    }

    companion object {
        const val ANTI_ALIAS_FLAG = 1
        const val FILTER_BITMAP_FLAG = 2
    }
}
