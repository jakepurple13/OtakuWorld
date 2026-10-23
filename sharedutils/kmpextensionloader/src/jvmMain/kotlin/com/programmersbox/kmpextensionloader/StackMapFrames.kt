package com.programmersbox.kmpextensionloader

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * dex2jar emits Java 8 class files without StackMapTable frames, which the JVM verifier rejects
 * ("Expecting a stackmap frame") for any method with branches. Recomputes the frames in place.
 *
 * Type hierarchy is read from class file headers (the jar's own classes, then [hostClassLoader]'s
 * resources) instead of loading classes, so nothing from the extension is initialized or linked here.
 */
internal object StackMapFrames {
    private const val OBJECT = "java/lang/Object"

    private class Header(val superName: String?, val isInterface: Boolean)

    fun addTo(jar: File, hostClassLoader: ClassLoader = StackMapFrames::class.java.classLoader) {
        val out = File(jar.parentFile, jar.name + ".frames")
        try {
            ZipFile(jar).use { zip ->
                val entries = zip.entries().toList()
                val jarHeaders = entries
                    .filter { it.name.endsWith(".class") }
                    .associate { e ->
                        val reader = ClassReader(zip.getInputStream(e).use { it.readBytes() })
                        reader.className to Header(reader.superName, reader.access and Opcodes.ACC_INTERFACE != 0)
                    }
                val hierarchy = Hierarchy(jarHeaders, hostClassLoader)
                ZipOutputStream(out.outputStream().buffered()).use { zos ->
                    entries.forEach { e ->
                        val bytes = zip.getInputStream(e).use { it.readBytes() }
                        zos.putNextEntry(ZipEntry(e.name))
                        zos.write(if (e.name.endsWith(".class")) withFrames(bytes, hierarchy) else bytes)
                        zos.closeEntry()
                    }
                }
            }
            java.nio.file.Files.move(out.toPath(), jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally {
            out.delete()
        }
    }

    private fun withFrames(bytes: ByteArray, hierarchy: Hierarchy): ByteArray {
        val reader = ClassReader(bytes)
        val writer = object : ClassWriter(COMPUTE_FRAMES) {
            override fun getCommonSuperClass(type1: String, type2: String): String = hierarchy.commonSuperClass(type1, type2)
        }
        reader.accept(writer, ClassReader.SKIP_FRAMES)
        return writer.toByteArray()
    }

    private class Hierarchy(private val jarHeaders: Map<String, Header>, private val hostClassLoader: ClassLoader) {
        private val hostHeaders = HashMap<String, Header?>()

        private fun header(type: String): Header? = jarHeaders[type] ?: hostHeaders.getOrPut(type) {
            runCatching {
                hostClassLoader.getResourceAsStream("$type.class")?.use { input ->
                    val reader = ClassReader(input)
                    Header(reader.superName, reader.access and Opcodes.ACC_INTERFACE != 0)
                }
            }.getOrNull()
        }

        private fun ancestors(type: String): List<String> = generateSequence(type) { header(it)?.superName }.toList()

        // Interfaces and unknown types merge to Object; the verifier treats interface types as Object anyway.
        fun commonSuperClass(type1: String, type2: String): String {
            val h1 = header(type1) ?: return OBJECT
            val h2 = header(type2) ?: return OBJECT
            if (h1.isInterface || h2.isInterface) return OBJECT
            val chain1 = ancestors(type1).toSet()
            return ancestors(type2).firstOrNull { it in chain1 } ?: OBJECT
        }
    }
}
