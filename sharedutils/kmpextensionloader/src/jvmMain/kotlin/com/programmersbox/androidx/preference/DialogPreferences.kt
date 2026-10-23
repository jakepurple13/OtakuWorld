package androidx.preference

import android.content.Context
import android.widget.EditText

open class DialogPreference(context: Context) : Preference(context) {
    open var dialogTitle: CharSequence? = null
    open var dialogMessage: CharSequence? = null
}

open class ListPreference(context: Context) : DialogPreference(context) {
    open var entries: Array<CharSequence>? = null
    open var entryValues: Array<CharSequence>? = null
    open var value: String? = null

    open fun findIndexOfValue(value: String?): Int =
        if (value == null) -1 else entryValues?.indexOfLast { it.toString() == value } ?: -1
}

open class MultiSelectListPreference(context: Context) : DialogPreference(context) {
    open var entries: Array<CharSequence>? = null
    open var entryValues: Array<CharSequence>? = null
    open var values: Set<String> = emptySet()
}

open class EditTextPreference(context: Context) : DialogPreference(context) {
    fun interface OnBindEditTextListener {
        fun onBindEditText(editText: EditText)
    }

    open var text: String? = null

    var onBindEditTextListener: OnBindEditTextListener? = null
        private set

    open fun setOnBindEditTextListener(listener: OnBindEditTextListener?) {
        onBindEditTextListener = listener
    }
}
