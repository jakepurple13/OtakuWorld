package android.os

class Bundle {
    private val data = HashMap<String, Any?>()

    fun putString(key: String, value: String?) { data[key] = value }
    fun getString(key: String): String? = data[key] as? String
    fun getString(key: String, defaultValue: String): String = data[key] as? String ?: defaultValue

    fun putInt(key: String, value: Int) { data[key] = value }
    fun getInt(key: String): Int = data[key] as? Int ?: 0
    fun getInt(key: String, defaultValue: Int): Int = data[key] as? Int ?: defaultValue

    fun putLong(key: String, value: Long) { data[key] = value }
    fun getLong(key: String): Long = data[key] as? Long ?: 0L
    fun getLong(key: String, defaultValue: Long): Long = data[key] as? Long ?: defaultValue

    fun putFloat(key: String, value: Float) { data[key] = value }
    fun getFloat(key: String): Float = data[key] as? Float ?: 0f
    fun getFloat(key: String, defaultValue: Float): Float = data[key] as? Float ?: defaultValue

    fun putBoolean(key: String, value: Boolean) { data[key] = value }
    fun getBoolean(key: String): Boolean = data[key] as? Boolean ?: false
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = data[key] as? Boolean ?: defaultValue

    fun putStringArrayList(key: String, value: ArrayList<String>?) { data[key] = value }
    @Suppress("UNCHECKED_CAST")
    fun getStringArrayList(key: String): ArrayList<String>? = data[key] as? ArrayList<String>

    fun containsKey(key: String): Boolean = data.containsKey(key)
    fun remove(key: String) { data.remove(key) }
    fun size(): Int = data.size
    fun isEmpty(): Boolean = data.isEmpty()
    fun keySet(): Set<String> = data.keys
    fun clear() { data.clear() }
}
