package com.programmersbox.aniyomi

import android.app.Application
import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmAniyomiExtensionLoaderTest {
    private val root = Files.createTempDirectory("aniyomiloader").toFile()
    private val apkDir = File(root, "apks")
    private val jarDir = File(root, "jars")

    init {
        AniyomiRuntime.init(Application("aniyomi-test", File(root, "data")))
    }

    private val store = AniyomiApkStore(apkDir, jarDir).also { s ->
        runBlocking { s.install(fixtureCopy()) }
    }
    private val loader = JvmAniyomiExtensionLoader(store)
    private val catalog = testCatalog(store)
    private val keys = setOf(AniyomiFixture.REPO_FINGERPRINT)

    private val fixtureTrustId =
        AniyomiTrust.trustId(AniyomiFixture.PACKAGE, AniyomiFixture.VERSION_CODE, AniyomiFixture.REPO_FINGERPRINT)

    private fun fixtureCopy(): File = File(root, "dl-${System.nanoTime()}.apk").also { AniyomiFixture.apk.copyTo(it) }

    @AfterTest fun cleanup() {
        loader.close()
        root.deleteRecursively()
    }

    @Test fun trustedFixtureLoadsSources() {
        val sources = loader.load(catalog, keys, emptySet()) {}
        assertTrue(sources.isNotEmpty())
        sources.forEach {
            val api = assertIs<AniyomiApiService>(it.apiService)
            assertTrue(api.serviceName.startsWith("aniyomi:"))
            assertTrue(api.displayName.isNotBlank())
            assertEquals(api.displayName, it.name)
            assertEquals(AniyomiFixture.PACKAGE, it.packageName)
        }
        assertTrue(sources.none { it.apiService is KmpConfigurableApiService })
    }

    @Test fun untrustedFixtureShowsTrustPrompt() = runTest {
        var captured: String? = null
        val source = loader.load(catalog, emptySet(), emptySet()) { captured = it }.single()
        val api = assertIs<UntrustedAniyomiApiService>(source.apiService)
        assertTrue(api.canTrust)
        assertEquals("Untrusted", api.reason)
        assertTrue(source.name.endsWith("(Untrusted)"), source.name)
        assertFalse(source.name.startsWith("Aniyomi: "), source.name)
        api.trust()
        assertEquals(fixtureTrustId, captured)
    }

    @Test fun trustIdLoadsWithoutRepoKey() {
        assertIs<AniyomiApiService>(loader.load(catalog, emptySet(), setOf(fixtureTrustId)) {}.first().apiService)
    }

    @Test fun unsupportedLibShowsReason() {
        val seamStore = AniyomiApkStore(apkDir, jarDir, inspect = { AniyomiApkInspector.inspect(it).copy(libVersion = 13.0) })
        val api = assertIs<UntrustedAniyomiApiService>(
            JvmAniyomiExtensionLoader(seamStore).load(catalog, keys, emptySet()) {}.single().apiService
        )
        assertEquals("Unsupported lib 13.0", api.reason)
    }

    @Test fun missingMetadataIsMalformedBeforeLibCheck() {
        val seamStore = AniyomiApkStore(
            apkDir, jarDir,
            inspect = { AniyomiApkInspector.inspect(it).copy(hasMetadata = false, libVersion = null) },
        )
        val api = assertIs<UntrustedAniyomiApiService>(
            JvmAniyomiExtensionLoader(seamStore).load(catalog, keys, emptySet()) {}.single().apiService
        )
        assertEquals("Malformed", api.reason)
    }

    @Test fun conversionFailureIsVisibleOnceTrusted() {
        val seamStore = AniyomiApkStore(apkDir, jarDir, convert = { _, _ -> null })
        val seamLoader = JvmAniyomiExtensionLoader(seamStore)
        val trusted = assertIs<UntrustedAniyomiApiService>(seamLoader.load(catalog, keys, emptySet()) {}.single().apiService)
        assertEquals("Failed to convert", trusted.reason)
        val untrusted = assertIs<UntrustedAniyomiApiService>(seamLoader.load(catalog, emptySet(), emptySet()) {}.single().apiService)
        assertEquals("Untrusted", untrusted.reason)
    }

    @Test fun reloadAfterUpdateUsesNewJar() = runTest {
        // The update is the fixture plus one extra entry: different bytes, so a different content-hashed jar.
        // That breaks the signature, so inspection is faked from the original APK.
        val original = AniyomiApkInspector.inspect(AniyomiFixture.apk)
        var updated = false
        val seamStore = AniyomiApkStore(
            apkDir, jarDir,
            inspect = { if (updated) original.copy(versionCode = AniyomiFixture.VERSION_CODE + 1) else original },
        )
        val seamLoader = JvmAniyomiExtensionLoader(seamStore)
        try {
            val first = assertIs<AniyomiApiService>(seamLoader.load(catalog, keys, emptySet()) {}.first().apiService)
            val oldLoader = first.source.javaClass.classLoader
            val generated = sourceClassName().replace('.', '/') + ".class"
            assertNotNull(oldLoader.getResource(generated))
            val oldJar = assertNotNull(seamStore.installed().single().jar)

            updated = true
            seamStore.install(updatedFixture())
            val second = assertIs<AniyomiApiService>(seamLoader.load(catalog, keys, emptySet()) {}.first().apiService)

            assertNotSame(first, second)
            assertNotSame(oldLoader, second.source.javaClass.classLoader)
            assertNull(oldLoader.getResource(generated), "old loader should be closed")
            assertFalse(oldJar.exists(), "stale jar should be cleaned up")
        } finally {
            seamLoader.close()
        }
    }

    @Test fun brokenPackageDoesNotAbortOthers() {
        val brokenPkg = "eu.kanade.tachiyomi.animeextension.en.broken"
        File(apkDir, "$brokenPkg.apk").writeText("not an apk")
        val brokenInfo = AniyomiApkInfo(
            packageName = brokenPkg,
            versionName = "14.1",
            versionCode = 1,
            label = "Broken",
            isExtension = true,
            hasMetadata = true,
            libVersion = 14.0,
            sourceClasses = ".Missing",
            nsfw = false,
            fingerprints = listOf(AniyomiFixture.REPO_FINGERPRINT),
        )
        val seamStore = AniyomiApkStore(
            apkDir = apkDir,
            jarCacheDir = jarDir,
            inspect = { if (it.name.startsWith(brokenPkg)) brokenInfo else AniyomiApkInspector.inspect(it) },
            convert = { apk, dir ->
                if (apk.name.startsWith(brokenPkg)) File(dir, "broken.jar").apply { writeText("garbage") }
                else DexConverter.convert(apk, dir)
            },
        )
        val seamLoader = JvmAniyomiExtensionLoader(seamStore)
        try {
            val sources = seamLoader.load(catalog, keys, emptySet()) {}
            assertTrue(sources.any { it.packageName == AniyomiFixture.PACKAGE && it.apiService is AniyomiApiService })
            val broken = assertIs<UntrustedAniyomiApiService>(sources.single { it.packageName == brokenPkg }.apiService)
            assertTrue(broken.reason.startsWith("Failed to load: "), broken.reason)
        } finally {
            seamLoader.close()
        }
    }

    @Test fun uninstallThenLoadDropsTheSource() = runTest {
        loader.load(catalog, keys, emptySet()) {}
        assertTrue(store.uninstall(AniyomiFixture.PACKAGE))
        assertTrue(loader.load(catalog, keys, emptySet()) {}.isEmpty())
    }

    @Test fun isExtensionReflectsStore() = runTest {
        assertTrue(loader.isExtension(AniyomiFixture.PACKAGE))
        store.uninstall(AniyomiFixture.PACKAGE)
        assertFalse(loader.isExtension(AniyomiFixture.PACKAGE))
    }

    private fun sourceClassName() = AniyomiFixture.PACKAGE + AniyomiFixture.SOURCE_CLASS

    private fun updatedFixture(): File {
        val out = File(root, "update-${System.nanoTime()}.apk")
        ZipFile(AniyomiFixture.apk).use { zip ->
            ZipOutputStream(out.outputStream()).use { zos ->
                zip.entries().asSequence().forEach { e ->
                    zos.putNextEntry(ZipEntry(e.name))
                    zip.getInputStream(e).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                zos.putNextEntry(ZipEntry("assets/update-marker.txt"))
                zos.write("v${AniyomiFixture.VERSION_CODE + 1}".toByteArray())
                zos.closeEntry()
            }
        }
        return out
    }
}

internal fun testCatalog(
    store: AniyomiApkStore,
    customUrls: List<String> = emptyList(),
    repoClient: AniyomiRepoClient = AniyomiRepoClient(HttpClient(MockEngine { throw IOException("offline") })),
) = AniyomiCatalog(
    application = Application("aniyomi-test", Files.createTempDirectory("aniyomidata").toFile()),
    repoClient = repoClient,
    store = store,
    customUrls = { customUrls },
    trustedIds = { emptySet() },
    addTrusted = {},
)
