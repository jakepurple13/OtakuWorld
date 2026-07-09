@file:OptIn(ExperimentalTime::class)

package com.programmersbox.kmpuiviews.utils

import com.programmersbox.kmpmodels.KmpInfoModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource


object Cached {
    val cache = KmpExpiringCache<String, KmpInfoModel>()
}

class KmpExpiringCache<K, V>(
    private val maxSize: Int = 10,
    private val ttl: Duration = 5.minutes,
) {
    // Stores the value along with a KMP-friendly timestamp
    private data class CacheEntry<V>(val value: V, val timestamp: TimeMark)

    // Mutex ensures thread-safety across iOS, Android, Desktop, etc.
    private val mutex = Mutex()

    // The actual cache storage
    private val cacheMap = mutableMapOf<K, CacheEntry<V>>()

    // Tracks access order to implement the LRU (Least Recently Used) eviction
    private val accessOrder = ArrayDeque<K>()

    suspend fun put(key: K, value: V) = mutex.withLock {
        // If it already exists, remove it from the queue so we can move it to the back
        if (cacheMap.containsKey(key)) {
            accessOrder.remove(key)
        }

        // Add to the back of the queue (most recently used)
        accessOrder.addLast(key)

        // Save the value with the current monotonic time
        cacheMap[key] = CacheEntry(value, TimeSource.Monotonic.markNow())

        // Enforce the size limit by evicting the oldest items from the front of the queue
        while (accessOrder.size > maxSize) {
            val oldestKey = accessOrder.removeFirst()
            cacheMap.remove(oldestKey)
        }
    }

    suspend operator fun set(key: K, value: V) = put(key, value)

    suspend operator fun get(key: K): V? = mutex.withLock {
        val entry = cacheMap[key] ?: return null

        // Check if the elapsed time since insertion is greater than our TTL
        if (entry.timestamp.elapsedNow() > ttl) {
            // Expired: evict it and return null
            cacheMap.remove(key)
            accessOrder.remove(key)
            return null
        } else {
            // Valid: mark as recently accessed by moving it to the back of the queue
            accessOrder.remove(key)
            accessOrder.addLast(key)
            return entry.value
        }
    }

    suspend fun remove(key: K): V? = mutex.withLock {
        accessOrder.remove(key)
        return cacheMap.remove(key)?.value
    }

    suspend fun cleanUp() = mutex.withLock {
        val keysToRemove = cacheMap.filter { it.value.timestamp.elapsedNow() > ttl }.keys
        keysToRemove.forEach { key ->
            cacheMap.remove(key)
            accessOrder.remove(key)
        }
    }

    suspend fun size(): Int = mutex.withLock { cacheMap.size }
}