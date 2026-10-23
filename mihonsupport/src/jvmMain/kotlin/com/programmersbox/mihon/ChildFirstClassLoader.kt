package com.programmersbox.mihon

import java.net.URL
import java.net.URLClassLoader

/**
 * Child-first like Android's DelegateLastClassLoader. The JDK, Kotlin, and the extension ABI the host
 * provides (android.*, eu.kanade.tachiyomi.*, okhttp3.*, Injekt, ...) are always parent-first, so a copy
 * accidentally bundled in an extension can never shadow the host's and break `is Source` checks or Injekt.
 */
class ChildFirstClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

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

    private fun isParentFirst(name: String): Boolean = PARENT_FIRST.any { name.startsWith(it) }

    private companion object {
        val PARENT_FIRST = listOf(
            "java.", "javax.", "jdk.", "sun.", "kotlin.", "kotlinx.",
            "android.", "androidx.", "eu.kanade.tachiyomi.", "tachiyomi.", "mihon.",
            "okhttp3.", "okio.", "org.jsoup.", "uy.kohesive.injekt.", "rx.", "app.cash.quickjs.",
        )
    }
}
