package android.util

/** Fixed phone-like metrics; extensions use them to size off-screen WebViews or images. */
class DisplayMetrics {
    @JvmField var widthPixels: Int = 1080
    @JvmField var heightPixels: Int = 1920
    @JvmField var density: Float = 2.625f
    @JvmField var densityDpi: Int = 420
    @JvmField var scaledDensity: Float = 2.625f
    @JvmField var xdpi: Float = 420f
    @JvmField var ydpi: Float = 420f
}
