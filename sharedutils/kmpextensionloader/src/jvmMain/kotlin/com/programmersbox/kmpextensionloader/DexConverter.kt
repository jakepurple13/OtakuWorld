package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.Method
import com.googlecode.d2j.dex.BaseDexExceptionHandler
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.node.DexMethodNode
import com.googlecode.d2j.reader.BaseDexFileReader
import com.googlecode.d2j.reader.MultiDexFileReader
import org.objectweb.asm.MethodVisitor
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object DexConverter {

    /** Bump when conversion output changes so jars from an older converter are not reused. */
    private const val CONVERTER_VERSION = 5

    fun convert(apkFile: File, cacheDir: File): File? {
        val hash = sha256(apkFile)
        val cachedJar = jarFor(apkFile, cacheDir)
        if (cachedJar.exists()) return cachedJar

        // Not "*.jar", so cache cleanup never mistakes an in-progress conversion for a stale jar.
        val tmp = File(cacheDir, "$hash.jar.part")
        return runCatching {
            cacheDir.mkdirs()
            translate(MultiDexFileReader.open(apkFile.readBytes()), tmp, apkFile.name)
            addApkResources(apkFile, tmp)
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

    /** Translates the dex in [reader] to [jar]; throws when no classes come out. */
    internal fun translate(reader: BaseDexFileReader, jar: File, name: String) {
        val handler = StubbingExceptionHandler(name)
        Dex2jar.from(ElidedConstructorFix.wrap(reader))
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
            .to(jar.toPath())
        // Like Suwayomi, untranslatable methods become throwing stubs and the jar is kept.
        if (handler.failures.isNotEmpty()) {
            println("DexConverter: $name converted with ${handler.failures.size} translation failure(s)")
        }
        check(jar.exists() && hasClasses(jar)) { "dex2jar produced no classes" }
        StackMapFrames.addTo(jar)
    }

    /**
     * Android's class loader serves every APK entry as a resource (e.g. Keiyoushi's i18n reads
     * `assets/i18n/messages_en.properties` through it), so the jar gets them too. Dex files and META-INF
     * (APK signatures would make the JVM try to verify the jar) are left out.
     */
    private fun addApkResources(apkFile: File, jar: File) {
        val merged = File(jar.path + ".res")
        try {
            mergeInto(merged, jar, apkFile)
            java.nio.file.Files.move(merged.toPath(), jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally {
            merged.delete()
        }
    }

    private fun mergeInto(merged: File, jar: File, apkFile: File) {
        ZipFile(jar).use { classes ->
            ZipFile(apkFile).use { apk ->
                ZipOutputStream(merged.outputStream().buffered()).use { out ->
                    val names = mutableSetOf<String>()
                    fun copy(zip: ZipFile, entry: ZipEntry) {
                        if (!names.add(entry.name)) return
                        out.putNextEntry(ZipEntry(entry.name))
                        zip.getInputStream(entry).use { it.copyTo(out) }
                        out.closeEntry()
                    }
                    classes.entries().asSequence().forEach { copy(classes, it) }
                    apk.entries().asSequence()
                        .filter { !it.isDirectory && !it.name.endsWith(".dex") && !it.name.startsWith("META-INF/") }
                        .forEach { copy(apk, it) }
                }
            }
        }
    }

    /** True for a finished jar [convert] wrote (any converter version); false for temp files and anything else. */
    fun isCachedJar(file: File): Boolean = file.isFile && CACHED_JAR.matches(file.name)

    private val CACHED_JAR = Regex("^[0-9a-f]{64}-v[0-9]+\\.jar$")

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
