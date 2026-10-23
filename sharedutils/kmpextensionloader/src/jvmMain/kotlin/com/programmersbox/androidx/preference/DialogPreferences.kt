package androidx.preference

import android.content.Context

open class DialogPreference(context: Context) : Preference(context) {
    open var dialogTitle: CharSequence? = null
    open var dialogMessage: CharSequence? = null
}

open class ListPreference(context: Context) : DialogPreference(context) {
    open var entries: Array<CharSequence>? = null
    open var entryValues: Array<CharSequence>? = null
    open var value: String? = null
}

open class MultiSelectListPreference(context: Context) : DialogPreference(context) {
    open var entries: Array<CharSequence>? = null
    open var entryValues: Array<CharSequence>? = null
    open var values: Set<String> = emptySet()
}

open class EditTextPreference(context: Context) : DialogPreference(context) {
    open var text: String? = null
}
