package com.programmersbox.kmpuiviews

import androidx.compose.ui.graphics.ImageBitmap
import io.kamel.core.cache.Cache

/**
 * LRU image cache bounded by decoded bytes instead of entry count. Kamel's default holds 100 images
 * regardless of size, which for full-size manga pages (~11MB each) can grow past 1GB.
 */
internal class ByteBoundedImageCache(private val maxBytes: Long) : Cache<Any, ImageBitmap> {

    private val entries = LinkedHashMap<Any, ImageBitmap>(16, 0.75f, true)
    private var bytes = 0L

    override val size: Int
        @Synchronized get() = entries.size

    override val maxSize: Int get() = Int.MAX_VALUE

    @Synchronized
    override fun get(key: Any): ImageBitmap? = entries[key]

    @Synchronized
    override fun set(key: Any, value: ImageBitmap) {
        val weight = value.byteSize()
        if (weight > maxBytes) return
        entries.put(key, value)?.let { bytes -= it.byteSize() }
        bytes += weight
        val iterator = entries.entries.iterator()
        while (bytes > maxBytes && iterator.hasNext()) {
            bytes -= iterator.next().value.byteSize()
            iterator.remove()
        }
    }

    @Synchronized
    override fun remove(key: Any): Boolean {
        val removed = entries.remove(key) ?: return false
        bytes -= removed.byteSize()
        return true
    }

    @Synchronized
    override fun clear() {
        entries.clear()
        bytes = 0
    }

    private fun ImageBitmap.byteSize(): Long = width.toLong() * height * 4
}
