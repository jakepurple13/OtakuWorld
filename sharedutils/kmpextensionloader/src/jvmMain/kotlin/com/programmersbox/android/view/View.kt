package android.view

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Headless: nothing is ever shown, measured, or laid out on desktop. Exists so extension code that builds
 * views (WebView helpers, settings dialogs) links; [post] still runs its runnable on the main looper.
 * The SDK's AccessibilityEventSource/Drawable.Callback/KeyEvent.Callback interfaces are left out: no
 * extension references them.
 */
open class View(private val context: Context) {
    private var parent: ViewParent? = null
    private var enabled: Boolean = true
    private var layoutParams: ViewGroup.LayoutParams? = null

    fun getContext(): Context = context
    fun getParent(): ViewParent? = parent
    internal fun setParent(parent: ViewParent?) {
        this.parent = parent
    }

    fun getRootView(): View {
        var root: View = this
        while (true) root = (root.parent as? View) ?: return root
    }

    open fun <T : View> findViewById(id: Int): T? = null

    open fun isEnabled(): Boolean = enabled
    open fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    open fun getLayoutParams(): ViewGroup.LayoutParams? = layoutParams
    open fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        layoutParams = params
    }

    fun measure(widthMeasureSpec: Int, heightMeasureSpec: Int) {}
    fun layout(l: Int, t: Int, r: Int, b: Int) {}

    open fun post(action: Runnable): Boolean = Handler(Looper.getMainLooper()).post(action)

    // CloudStream plugins' settings dialogs implement these with synthetic lambda classes; the JVM loads
    // a class's interfaces when the class loads, so they must exist even though nothing calls them here.
    fun interface OnClickListener {
        fun onClick(v: View)
    }

    fun interface OnFocusChangeListener {
        fun onFocusChange(v: View, hasFocus: Boolean)
    }

    class MeasureSpec {
        companion object {
            private const val MODE_SHIFT = 30
            private const val MODE_MASK = 0x3 shl MODE_SHIFT
            const val UNSPECIFIED = 0
            const val EXACTLY = 1 shl MODE_SHIFT
            const val AT_MOST = 2 shl MODE_SHIFT

            @JvmStatic fun makeMeasureSpec(size: Int, mode: Int): Int = (size and MODE_MASK.inv()) or (mode and MODE_MASK)
            @JvmStatic fun getMode(measureSpec: Int): Int = measureSpec and MODE_MASK
            @JvmStatic fun getSize(measureSpec: Int): Int = measureSpec and MODE_MASK.inv()
        }
    }
}

interface ViewParent {
    fun getParent(): ViewParent?
}

interface ViewManager {
    fun addView(view: View, params: ViewGroup.LayoutParams?)
    fun updateViewLayout(view: View, params: ViewGroup.LayoutParams?)
    fun removeView(view: View)
}

abstract class ViewGroup(context: Context) : View(context), ViewManager, ViewParent {
    private val children = mutableListOf<View>()

    open class LayoutParams(@JvmField var width: Int, @JvmField var height: Int) {
        companion object {
            const val MATCH_PARENT = -1
            const val FILL_PARENT = -1
            const val WRAP_CONTENT = -2
        }
    }

    fun getChildCount(): Int = children.size
    fun getChildAt(index: Int): View? = children.getOrNull(index)

    open fun addView(view: View) = addView(view, view.getLayoutParams())

    override fun addView(view: View, params: ViewGroup.LayoutParams?) {
        params?.let(view::setLayoutParams)
        children += view
        view.setParent(this)
    }

    override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams?) = view.setLayoutParams(params)

    override fun removeView(view: View) {
        if (children.remove(view)) view.setParent(null)
    }

    protected abstract fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int)
}
