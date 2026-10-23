package android.content.res

import android.util.DisplayMetrics

open class Resources {
    private val metrics = DisplayMetrics()

    open fun getDisplayMetrics(): DisplayMetrics = metrics

    companion object {
        private val system = Resources()

        @JvmStatic fun getSystem(): Resources = system
    }
}
