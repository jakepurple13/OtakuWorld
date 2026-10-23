package android.graphics

class Rect() {
    @JvmField var left: Int = 0
    @JvmField var top: Int = 0
    @JvmField var right: Int = 0
    @JvmField var bottom: Int = 0

    constructor(left: Int, top: Int, right: Int, bottom: Int) : this() {
        set(left, top, right, bottom)
    }

    fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun width(): Int = right - left
    fun height(): Int = bottom - top

    override fun equals(other: Any?): Boolean =
        other is Rect && left == other.left && top == other.top && right == other.right && bottom == other.bottom

    override fun hashCode(): Int = ((left * 31 + top) * 31 + right) * 31 + bottom

    override fun toString(): String = "Rect($left, $top - $right, $bottom)"
}
