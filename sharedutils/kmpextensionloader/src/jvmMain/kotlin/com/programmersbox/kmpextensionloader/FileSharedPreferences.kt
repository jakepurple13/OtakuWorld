package com.programmersbox.kmpextensionloader

import android.content.SharedPreferences
import java.io.File
import java.util.Properties
import java.util.concurrent.CopyOnWriteArraySet

class FileSharedPreferences(private val file: File) : SharedPreferences {
    private val props = Properties()
    private val listeners = CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()

    init {
        if (file.exists()) file.inputStream().use { props.load(it) }
    }

    override fun getString(key: String, defValue: String?): String? = props.getProperty(key) ?: defValue
    override fun getInt(key: String, defValue: Int): Int = props.getProperty(key)?.toIntOrNull() ?: defValue
    override fun getLong(key: String, defValue: Long): Long = props.getProperty(key)?.toLongOrNull() ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = props.getProperty(key)?.toFloatOrNull() ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        props.getProperty(key)?.toBooleanStrictOrNull() ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        val v = props.getProperty(key) ?: return defValues
        return if (v.isEmpty()) emptySet()
        else v.split(",").map { java.net.URLDecoder.decode(it, "UTF-8") }.toSet()
    }

    override fun contains(key: String): Boolean = props.containsKey(key)
    override fun getAll(): Map<String, *> = props.entries.associate { it.key.toString() to it.value }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = Properties()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            if (value == null) removals.add(key) else pending.setProperty(key, value)
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor { pending.setProperty(key, value.toString()); return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { pending.setProperty(key, value.toString()); return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { pending.setProperty(key, value.toString()); return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { pending.setProperty(key, value.toString()); return this }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            if (values == null) removals.add(key)
            else pending.setProperty(key, values.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") })
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor { removals.add(key); return this }
        override fun clear(): SharedPreferences.Editor { clearAll = true; return this }

        override fun apply() { commit() }

        override fun commit(): Boolean {
            val changedKeys = linkedSetOf<String>()
            val ok = runCatching {
                if (clearAll) {
                    changedKeys.addAll(props.stringPropertyNames())
                    props.clear()
                }
                removals.forEach { if (props.remove(it) != null) changedKeys.add(it) }
                pending.forEach { k, v ->
                    props.setProperty(k.toString(), v.toString())
                    changedKeys.add(k.toString())
                }
                file.parentFile?.mkdirs()
                file.outputStream().use { props.store(it, null) }
            }.isSuccess
            if (ok) changedKeys.forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(this@FileSharedPreferences, key) }
            }
            return ok
        }
    }
}
