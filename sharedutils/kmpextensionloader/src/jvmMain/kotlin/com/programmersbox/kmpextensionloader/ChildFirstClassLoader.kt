package com.programmersbox.kmpextensionloader

import java.net.URL
import java.net.URLClassLoader

/**
 * Child-first like Android's DelegateLastClassLoader. Names starting with any of [parentFirst] (the JDK,
 * Kotlin, and the extension ABI the host provides: android.*, eu.kanade.tachiyomi.*, okhttp3.*, Injekt, ...)
 * are always parent-first, so a copy accidentally bundled in an extension can never shadow the host's and
 * break `is Source` checks or Injekt.
 */
class ChildFirstClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
    private val parentFirst: List<String>,
) : URLClassLoader(urls, parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
        val loaded = findLoadedClass(name) ?: if (isParentFirst(name)) {
            super.loadClass(name, false)
        } else {
            try {
                findClass(name)
            } catch (_: ClassNotFoundException) {
                super.loadClass(name, false)
            }
        }
        if (resolve) resolveClass(loaded)
        loaded
    }

    override fun getResource(name: String): URL? {
        if (isParentFirst(name.replace('/', '.'))) return super.getResource(name)
        return findResource(name) ?: super.getResource(name)
    }

    private fun isParentFirst(name: String): Boolean = parentFirst.any { name.startsWith(it) }
}
