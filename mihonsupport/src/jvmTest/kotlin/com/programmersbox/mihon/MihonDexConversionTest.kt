package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
import com.programmersbox.kmpextensionloader.DexConverter
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Converts the real fixture APK and checks the jar actually links on the JVM. */
class MihonDexConversionTest {
    private val root = Files.createTempDirectory("mihondex").toFile()

    init {
        MihonRuntime.init(Application("mihon-test", File(root, "data")))
    }

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private fun convertFixture(apk: File = MihonFixture.apk): File = assertNotNull(DexConverter.convert(apk, File(root, "jars")))

    private fun classEntries(jar: File): Map<String, ByteArray> = ZipFile(jar).use { zip ->
        zip.entries().toList()
            .filter { it.name.endsWith(".class") }
            .associate { e -> e.name.removeSuffix(".class").replace('/', '.') to zip.getInputStream(e).use { it.readBytes() } }
    }

    @Test fun kotlinMangledNamesKeepTheirDash() {
        val texts = classEntries(convertFixture()).values.map { String(it, Charsets.ISO_8859_1) }
        assertTrue(texts.any { "maxAge-LRDsOJo" in it }, "expected a reference to okhttp's maxAge-LRDsOJo")
        assertFalse(texts.any { "maxAge_LRDsOJo" in it }, "dex2jar sanitized '-' to '_'")
    }

    @Test fun everyConvertedClassVerifiesAndInitializes() = assertEveryClassVerifies(MihonFixture.apk)

    @Test fun everyMangaDexClassVerifiesAndInitializes() = assertEveryClassVerifies(MihonFixture.MangaDex.apk)

    // Activity subclasses (deep-link UrlActivity) are skipped: desktop never loads them.
    private fun assertEveryClassVerifies(apk: File) {
        val jar = convertFixture(apk)
        val entries = classEntries(jar)
        ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), javaClass.classLoader, ChildFirstClassLoader.BASE_PARENT_FIRST).use { loader ->
            val failures = entries.keys.filterNot { isActivity(it, entries) }.mapNotNull { name ->
                runCatching { Class.forName(name, true, loader) }.exceptionOrNull()?.let { "$name: $it" }
            }
            assertEquals(emptyList(), failures)
        }
    }

    // Android's class loader serves APK entries as resources; Keiyoushi's i18n lib reads assets/i18n/*.properties that way.
    @Test fun apkAssetsAreClassLoaderResources() {
        val jar = convertFixture(MihonFixture.MangaDex.apk)
        ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), javaClass.classLoader, ChildFirstClassLoader.BASE_PARENT_FIRST).use { loader ->
            val text = assertNotNull(loader.getResourceAsStream("assets/i18n/messages_en.properties")).use { it.readBytes().decodeToString() }
            assertTrue(text.isNotBlank())
        }
        ZipFile(jar).use { zip ->
            assertTrue(zip.entries().toList().none { it.name.startsWith("META-INF/") && !it.isDirectory && it.name.substringAfterLast('.') in setOf("SF", "RSA", "DSA", "EC") })
            assertTrue(zip.entries().toList().none { it.name.endsWith(".dex") })
        }
    }

    @Test fun mangaDexLoadsEverySourceAsMihonApiService() {
        val store = MihonApkStore(File(root, "apks"), File(root, "jars")).also { s ->
            runBlocking { s.install(File(root, "mangadex.apk").also { MihonFixture.MangaDex.apk.copyTo(it) }) }
        }
        val loader = JvmMihonExtensionLoader(store)
        try {
            val sources = loader.load(testCatalog(store), setOf(MihonFixture.REPO_FINGERPRINT), emptySet()) {}
            assertEquals(emptyList(), sources.map { it.apiService }.filterIsInstance<UntrustedMihonApiService>().map { it.reason })
            assertEquals(MihonFixture.MangaDex.SOURCE_COUNT, sources.size)
            assertTrue(sources.all { it.apiService is MihonApiService && it.packageName == MihonFixture.MangaDex.PACKAGE })
        } finally {
            loader.close()
        }
    }

    private fun isActivity(name: String, entries: Map<String, ByteArray>): Boolean {
        var current: String? = name
        while (current != null) {
            if (current == "android.app.Activity") return true
            current = entries[current]?.let { ClassReader(it).superName?.replace('/', '.') }
        }
        return false
    }

    @Test fun cacheKeyCarriesConverterVersion() {
        assertTrue(DexConverter.jarFor(MihonFixture.apk, root).name.endsWith("-v5.jar"))
    }

    // R8 drops the fixture's trivial interceptor constructor (new-instance La; + invoke-direct Object.<init>).
    @Test fun instancesOfClassesWithElidedConstructorsKeepTheirType() {
        val store = MihonApkStore(File(root, "apks"), File(root, "jars")).also { s ->
            runBlocking { s.install(File(root, "dl.apk").also { MihonFixture.apk.copyTo(it) }) }
        }
        val loader = JvmMihonExtensionLoader(store)
        try {
            val api = assertIs<MihonApiService>(
                loader.load(testCatalog(store), setOf(MihonFixture.REPO_FINGERPRINT), emptySet()) {}.first().apiService
            )
            val client = assertIs<HttpSource>(api.source).client
            val interceptors: List<Any> = client.interceptors + client.networkInterceptors
            assertTrue(interceptors.isNotEmpty())
            interceptors.forEach { assertIs<Interceptor>(it) }
        } finally {
            loader.close()
        }
    }
}
