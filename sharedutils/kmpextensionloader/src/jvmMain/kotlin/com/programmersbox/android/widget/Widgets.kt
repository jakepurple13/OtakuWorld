package android.widget

import android.content.Context
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup

/** Headless state holder, for extension settings code (e.g. EditTextPreference bind listeners). */
open class TextView(context: Context) : View(context) {
    private var text: CharSequence = ""
    private var hint: CharSequence? = null
    private var error: CharSequence? = null
    private var inputType: Int = 0
    private val watchers = mutableListOf<TextWatcher>()

    open fun getText(): CharSequence = text
    open fun setText(text: CharSequence?) {
        this.text = text ?: ""
    }

    fun getHint(): CharSequence? = hint
    fun setHint(hint: CharSequence?) {
        this.hint = hint
    }

    open fun getError(): CharSequence? = error
    open fun setError(error: CharSequence?) {
        this.error = error
    }

    open fun getInputType(): Int = inputType
    open fun setInputType(type: Int) {
        inputType = type
    }

    open fun setHorizontallyScrolling(whether: Boolean) {}

    fun addTextChangedListener(watcher: TextWatcher) {
        watchers += watcher
    }

    fun removeTextChangedListener(watcher: TextWatcher) {
        watchers -= watcher
    }
}

open class EditText(context: Context) : TextView(context) {
    fun selectAll() {}
}

open class Button(context: Context) : TextView(context)

/**
 * Holder of [OnCheckedChangeListener], which CloudStream plugins' settings dialogs implement with synthetic
 * lambda classes. The SDK's Checkable interface is left out: no extension references it.
 */
abstract class CompoundButton(context: Context) : Button(context) {
    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean)
    }
}

/** Prints instead of showing a toast. */
open class Toast(private val context: Context) {
    private var text: CharSequence? = null
    private var duration: Int = LENGTH_SHORT

    fun setText(s: CharSequence?) {
        text = s
    }

    fun getDuration(): Int = duration
    fun setDuration(duration: Int) {
        this.duration = duration
    }

    open fun show() {
        println("Toast (${context.packageName}): $text")
    }

    open fun cancel() {}

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        @JvmStatic fun makeText(context: Context, text: CharSequence?, duration: Int): Toast =
            Toast(context).apply {
                setText(text)
                setDuration(duration)
            }
    }
}

/** Only here as WebView's superclass (deprecated in the SDK). */
open class AbsoluteLayout(context: Context) : ViewGroup(context) {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
}
