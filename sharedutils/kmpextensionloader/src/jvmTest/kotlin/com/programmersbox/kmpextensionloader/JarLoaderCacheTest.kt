package com.programmersbox.kmpextensionloader

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class JarLoaderCacheTest {
    private val root = Files.createTempDirectory("jarloadercache").toFile()
    private val cache = JarLoaderCache<ClassLoader>(javaClass.classLoader, ChildFirstClassLoader.BASE_PARENT_FIRST)

    @AfterTest fun cleanup() {
        cache.close()
        root.deleteRecursively()
    }

    /** A jar holding one resource; a loader that still serves it is open, a closed one returns null. */
    private fun jar(name: String): File = File(root, name).also { file ->
        ZipOutputStream(file.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry(MARKER))
            zos.write(name.toByteArray())
            zos.closeEntry()
        }
    }

    @Test fun sameJarPathReusesLoaderAndValue() {
        val a = jar("a.jar")
        var builds = 0
        val first = cache.getOrLoad(a) { builds++; it }
        val second = cache.getOrLoad(a) { builds++; it }
        assertSame(first, second)
        assertEquals(1, builds)
        assertNotSame(first, cache.getOrLoad(jar("b.jar")) { it })
    }

    @Test fun retainClosesOnlyGoneJars() {
        val a = jar("a.jar")
        val b = jar("b.jar")
        val loaderA = cache.getOrLoad(a) { it }
        val loaderB = cache.getOrLoad(b) { it }

        cache.retain(setOf(b.absolutePath))

        assertNull(loaderA.getResource(MARKER), "gone jar's loader should be closed")
        assertNotNull(loaderB.getResource(MARKER))
        assertSame(loaderB, cache.getOrLoad(b) { error("must reuse") })
        assertNotSame(loaderA, cache.getOrLoad(a) { it }, "a closed jar gets a fresh loader")
    }

    @Test fun loaderIsClosedWhenBuildThrows() {
        val a = jar("a.jar")
        var built: ClassLoader? = null
        assertFailsWith<IllegalStateException> {
            cache.getOrLoad(a) { built = it; error("boom") }
        }
        assertNull(assertNotNull(built).getResource(MARKER))
        // Nothing was cached, so the next call builds again with a new, open loader.
        val next = cache.getOrLoad(a) { it }
        assertNotSame(built, next)
        assertNotNull(next.getResource(MARKER))
    }

    @Test fun closeClosesEveryLoader() {
        val loaders = listOf(jar("a.jar"), jar("b.jar")).map { cache.getOrLoad(it) { l -> l } }
        cache.close()
        loaders.forEach { assertNull(it.getResource(MARKER)) }
    }

    private companion object {
        const val MARKER = "marker.txt"
    }
}
