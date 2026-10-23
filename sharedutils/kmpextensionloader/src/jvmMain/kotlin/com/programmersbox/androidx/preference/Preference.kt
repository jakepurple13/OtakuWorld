package androidx.preference

import android.content.Context

open class Preference(private val ctx: Context) {
    fun interface OnPreferenceChangeListener {
        fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean
    }

    fun interface OnPreferenceClickListener {
        fun onPreferenceClick(preference: Preference): Boolean
    }

    open var key: String? = null
    open var title: CharSequence? = null
    open var summary: CharSequence? = null
    open var isVisible: Boolean = true
    open var isEnabled: Boolean = true

    var defaultValue: Any? = null
        private set
    var onPreferenceChangeListener: OnPreferenceChangeListener? = null
        private set

    open fun setDefaultValue(defaultValue: Any?) {
        this.defaultValue = defaultValue
    }

    open fun setOnPreferenceChangeListener(listener: OnPreferenceChangeListener?) {
        onPreferenceChangeListener = listener
    }

    var onPreferenceClickListener: OnPreferenceClickListener? = null
        private set

    open fun setOnPreferenceClickListener(listener: OnPreferenceClickListener?) {
        onPreferenceClickListener = listener
    }

    open fun getContext(): Context = ctx
}

open class PreferenceGroup(context: Context) : Preference(context) {
    private val children = mutableListOf<Preference>()

    open fun addPreference(preference: Preference): Boolean = children.add(preference)
}

open class PreferenceScreen(context: Context) : PreferenceGroup(context)
