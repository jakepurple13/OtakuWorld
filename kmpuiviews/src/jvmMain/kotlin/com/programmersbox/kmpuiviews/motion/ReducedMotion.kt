package com.programmersbox.kmpuiviews.motion

import java.util.concurrent.TimeUnit

/**
 * Whether the OS asks for less motion: Reduce motion on macOS, GNOME's animations switch on Linux. False when unknown.
 * Windows is always false: there is no JDK-only source for SPI_GETCLIENTAREAANIMATION (JNA would be needed).
 * Read at most once a minute because it runs a process (up to 500 ms) and callers are on the UI thread.
 */
fun systemReducesMotion(): Boolean = reducesMotion.get()

private val reducesMotion = TimedCache(60_000) {
    runCatching {
        val os = System.getProperty("os.name").orEmpty()
        when {
            os.startsWith("Mac", ignoreCase = true) ->
                run("defaults", "read", "com.apple.universalaccess", "reduceMotion")?.trim() == "1"

            os.startsWith("Windows", ignoreCase = true) -> false

            else -> run("gsettings", "get", "org.gnome.desktop.interface", "enable-animations")?.trim() == "false"
        }
    }.getOrDefault(false)
}

/** Output of the command, or null when it fails to start or takes over 500 ms. */
private fun run(vararg command: String): String? {
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly()
        return null
    }
    return process.inputStream.bufferedReader().use { it.readText() }
}

private class TimedCache<T>(
    private val ttlMs: Long,
    private val clock: () -> Long = System::currentTimeMillis,
    private val load: () -> T,
) {
    private var value: T? = null
    private var readAt = Long.MIN_VALUE

    @Synchronized
    fun get(): T {
        val now = clock()
        @Suppress("UNCHECKED_CAST")
        if (readAt != Long.MIN_VALUE && now - readAt < ttlMs) return value as T
        return load().also {
            value = it
            readAt = now
        }
    }
}
