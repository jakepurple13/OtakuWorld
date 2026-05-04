package android.content

import java.io.File
import java.util.Properties

class SharedPreferences(private val file: File) {
    private val props = Properties()

    init {
        if (file.exists()) file.inputStream().use { props.load(it) }
    }

    fun getString(key: String, defValue: String?): String? = props.getProperty(key) ?: defValue
    fun getInt(key: String, defValue: Int): Int = props.getProperty(key)?.toIntOrNull() ?: defValue
    fun getLong(key: String, defValue: Long): Long = props.getProperty(key)?.toLongOrNull() ?: defValue
    fun getFloat(key: String, defValue: Float): Float = props.getProperty(key)?.toFloatOrNull() ?: defValue
    fun getBoolean(key: String, defValue: Boolean): Boolean =
        props.getProperty(key)?.toBooleanStrictOrNull() ?: defValue
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        val v = props.getProperty(key) ?: return defValues
        return if (v.isEmpty()) emptySet()
        else v.split(",").map { java.net.URLDecoder.decode(it, "UTF-8") }.toSet()
    }
    fun contains(key: String): Boolean = props.containsKey(key)
    fun getAll(): Map<String, *> = props.entries.associate { it.key.toString() to it.value }

    fun edit(): Editor = Editor(props, file)

    class Editor(private val props: Properties, private val file: File) {
        private val pending = Properties()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        fun putString(key: String, value: String?): Editor {
            if (value == null) removals.add(key) else pending.setProperty(key, value)
            return this
        }
        fun putInt(key: String, value: Int): Editor { pending.setProperty(key, value.toString()); return this }
        fun putLong(key: String, value: Long): Editor { pending.setProperty(key, value.toString()); return this }
        fun putFloat(key: String, value: Float): Editor { pending.setProperty(key, value.toString()); return this }
        fun putBoolean(key: String, value: Boolean): Editor { pending.setProperty(key, value.toString()); return this }
        fun putStringSet(key: String, values: Set<String>?): Editor {
            if (values == null) removals.add(key)
            else pending.setProperty(key, values.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") })
            return this
        }
        fun remove(key: String): Editor { removals.add(key); return this }
        fun clear(): Editor { clearAll = true; return this }

        fun apply() { commit() }

        fun commit(): Boolean = runCatching {
            if (clearAll) props.clear()
            removals.forEach { props.remove(it) }
            pending.forEach { k, v -> props.setProperty(k.toString(), v.toString()) }
            file.parentFile?.mkdirs()
            file.outputStream().use { props.store(it, null) }
        }.isSuccess
    }
}
