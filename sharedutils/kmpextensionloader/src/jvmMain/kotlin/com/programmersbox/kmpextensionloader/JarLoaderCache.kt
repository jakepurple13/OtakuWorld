package com.programmersbox.kmpextensionloader

import java.io.File

/**
 * One [ChildFirstClassLoader] per extension jar, plus whatever [getOrLoad] built from it.
 *
 * Keyed by jar path. Jar names are content hashes, so an unchanged jar keeps its loader and value across loads:
 * values handed out earlier stay usable, and their unloaded classes can still load.
 */
class JarLoaderCache<T>(
    private val parentClassLoader: ClassLoader,
    private val parentFirst: List<String>,
) {
    private class Entry<T>(val classLoader: ChildFirstClassLoader, val value: T)

    private val entries = mutableMapOf<String, Entry<T>>()

    /** Closes the loaders of jars not in [liveJarPaths]. Call before their files are deleted. */
    @Synchronized
    fun retain(liveJarPaths: Set<String>) {
        (entries.keys - liveJarPaths).forEach { path ->
            entries.remove(path)?.let { runCatching { it.classLoader.close() } }
        }
    }

    /** The cached value for [jar], or [build]s one with a new loader; the loader is closed if [build] throws. */
    @Synchronized
    fun getOrLoad(jar: File, build: (ClassLoader) -> T): T = entries.getOrPut(jar.absolutePath) {
        val classLoader = ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), parentClassLoader, parentFirst)
        try {
            Entry(classLoader, build(classLoader))
        } catch (t: Throwable) {
            runCatching { classLoader.close() }
            throw t
        }
    }.value

    /** Closes every class loader; values from earlier loads stop working. */
    @Synchronized
    fun close() {
        entries.values.forEach { runCatching { it.classLoader.close() } }
        entries.clear()
    }
}
