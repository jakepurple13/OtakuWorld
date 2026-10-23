package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.Method
import com.googlecode.d2j.dex.BaseDexExceptionHandler
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.node.DexMethodNode
import com.googlecode.d2j.reader.MultiDexFileReader
import org.objectweb.asm.MethodVisitor
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

object DexConverter {

    /** Bump when conversion output changes so jars from an older converter are not reused. */
    private const val CONVERTER_VERSION = 3

    fun convert(apkFile: File, cacheDir: File): File? {
        val hash = sha256(apkFile)
        val cachedJar = jarFor(apkFile, cacheDir)
        if (cachedJar.exists()) return cachedJar

        val tmp = File(cacheDir, "$hash.tmp.jar")
        return runCatching {
            cacheDir.mkdirs()
            val handler = StubbingExceptionHandler(apkFile.name)
            Dex2jar.from(ElidedConstructorFix.wrap(MultiDexFileReader.open(apkFile.readBytes())))
                .withExceptionHandler(handler)
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
            // Like Suwayomi, untranslatable methods become throwing stubs and the jar is kept.
            if (handler.failures.isNotEmpty()) {
                println("DexConverter: ${apkFile.name} converted with ${handler.failures.size} translation failure(s)")
            }
            check(tmp.exists() && hasClasses(tmp)) { "dex2jar produced no classes" }
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

    private fun hasClasses(jar: File): Boolean =
        ZipFile(jar).use { zip -> zip.entries().asSequence().any { it.name.endsWith(".class") } }
}

/**
 * Logs each dex2jar failure. A method that cannot be translated gets dex2jar's stub body
 * (`throw new RuntimeException("d2j fail translate: ...")`), so only that method fails, and only when called.
 */
internal class StubbingExceptionHandler(private val apkName: String) : BaseDexExceptionHandler() {
    private val recorded = mutableListOf<String>()
    val failures: List<String> get() = recorded

    override fun handleFileException(e: Exception) {
        record("$apkName: ${e.message ?: e}")
    }

    override fun handleMethodTranslateException(method: Method, methodNode: DexMethodNode, mv: MethodVisitor, e: Exception) {
        record("$apkName: ${method.owner}.${method.name}${method.desc}: ${e.message ?: e}")
        super.handleMethodTranslateException(method, methodNode, mv, e)
    }

    private fun record(failure: String) {
        recorded += failure
        println("DexConverter: translation failure in $failure")
    }
}
