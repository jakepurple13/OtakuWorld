package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.Method
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.dex.DexExceptionHandler
import com.googlecode.d2j.node.DexMethodNode
import org.objectweb.asm.MethodVisitor
import java.io.File
import java.security.MessageDigest

object DexConverter {

    /** Bump when conversion output changes so jars from an older converter are not reused. */
    private const val CONVERTER_VERSION = 2

    fun convert(apkFile: File, cacheDir: File): File? {
        val hash = sha256(apkFile)
        val cachedJar = jarFor(apkFile, cacheDir)
        if (cachedJar.exists()) return cachedJar

        val tmp = File(cacheDir, "$hash.tmp.jar")
        return runCatching {
            cacheDir.mkdirs()
            val errors = RecordingExceptionHandler()
            Dex2jar.from(apkFile)
                .withExceptionHandler(errors)
                .reUseReg(false)
                .topoLogicalSort()
                .skipDebug(true)
                .optimizeSynchronized(false)
                .printIR(false)
                .noCode(false)
                .skipExceptions(false)
                // Keep '-' in names: Kotlin mangles value-class APIs with it (e.g. okhttp's maxAge-LRDsOJo).
                .dontSanitizeNames(true)
                .to(tmp.toPath())
            // A partially translated jar fails later in confusing ways; reject it here instead.
            errors.first?.let { throw IllegalStateException("dex2jar failed: ${it.message}", it) }
            check(tmp.exists() && tmp.length() > 0) { "dex2jar produced no output" }
            StackMapFrames.addTo(tmp)
            java.nio.file.Files.move(
                tmp.toPath(),
                cachedJar.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
            cachedJar
        }.onFailure {
            tmp.delete()
            println("DexConverter: failed to convert ${apkFile.name}: ${it.message}")
            it.printStackTrace()
        }.getOrNull()
    }

    /** The cache location [convert] uses for [apkFile]'s jar (may not exist yet). */
    fun jarFor(apkFile: File, cacheDir: File): File = File(cacheDir, "${sha256(apkFile)}-v$CONVERTER_VERSION.jar")

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Records the first translation failure; dex2jar itself only logs or stubs them. */
    private class RecordingExceptionHandler : DexExceptionHandler {
        var first: Exception? = null
            private set

        override fun handleFileException(e: Exception) {
            if (first == null) first = e
        }

        override fun handleMethodTranslateException(method: Method, methodNode: DexMethodNode, mv: MethodVisitor, e: Exception) {
            if (first == null) first = IllegalStateException("Could not translate $method", e)
        }
    }
}
