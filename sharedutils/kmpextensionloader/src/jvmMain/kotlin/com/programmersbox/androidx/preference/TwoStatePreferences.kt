package androidx.preference

import android.content.Context

open class TwoStatePreference(context: Context) : Preference(context) {
    open var isChecked: Boolean = false
    open var summaryOn: CharSequence? = null
    open var summaryOff: CharSequence? = null
}

open class CheckBoxPreference(context: Context) : TwoStatePreference(context)

open class SwitchPreferenceCompat(context: Context) : TwoStatePreference(context)
