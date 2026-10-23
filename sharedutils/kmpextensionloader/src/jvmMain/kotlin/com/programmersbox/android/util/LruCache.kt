package android.util

/** Entry-count LRU like android.util.LruCache with the default sizeOf of 1. */
open class LruCache<K : Any, V : Any>(private var maxSize: Int) {
    private val map = LinkedHashMap<K, V>(0, 0.75f, true)

    init {
        require(maxSize > 0) { "maxSize <= 0" }
    }

    @Synchronized fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V): V? {
        val previous = map.put(key, value)
        trimToSize(maxSize)
        return previous
    }

    @Synchronized fun remove(key: K): V? = map.remove(key)

    @Synchronized fun evictAll() = map.clear()

    @Synchronized fun size(): Int = map.size

    @Synchronized fun maxSize(): Int = maxSize

    @Synchronized fun snapshot(): Map<K, V> = LinkedHashMap(map)

    @Synchronized
    fun resize(maxSize: Int) {
        require(maxSize > 0) { "maxSize <= 0" }
        this.maxSize = maxSize
        trimToSize(maxSize)
    }

    @Synchronized
    fun trimToSize(maxSize: Int) {
        val entries = map.entries.iterator()
        while (map.size > maxSize && entries.hasNext()) {
            entries.next()
            entries.remove()
        }
    }
}
