package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmMihonExtensionLoaderTest {
    private val root = Files.createTempDirectory("mihonloader").toFile()
    private val apkDir = File(root, "apks")
    private val jarDir = File(root, "jars")

    init {
        MihonRuntime.init(Application("mihon-test", File(root, "data")))
    }

    private val store = MihonApkStore(apkDir, jarDir).also { s ->
        val copy = File(root, "dl.apk").also { MihonFixture.apk.copyTo(it) }
        runBlocking { s.install(copy) }
    }
    private val loader = JvmMihonExtensionLoader(store)
    private val catalog = testCatalog(store)

    private val fixtureTrustId =
        MihonTrust.trustId(MihonFixture.PACKAGE, MihonFixture.VERSION_CODE, MihonFixture.REPO_FINGERPRINT)

    @AfterTest fun cleanup() {
        loader.close()
        root.deleteRecursively()
    }

    @Test fun repoSignedFixtureLoadsAsMihonApiService() {
        val sources = loader.load(catalog, setOf(MihonFixture.REPO_FINGERPRINT), emptySet()) {}
        assertTrue(sources.isNotEmpty())
        assertTrue(sources.all { it.apiService is MihonApiService && it.apiService.serviceName.startsWith("mihon:") })
        assertTrue(sources.none { it.apiService is KmpConfigurableApiService })
        assertTrue(sources.all { it.packageName == MihonFixture.PACKAGE })
    }

    @Test fun unknownSignerIsUntrustedAndTrustable() {
        val s = loader.load(catalog, emptySet(), emptySet()) {}.single()
        val api = assertIs<UntrustedMihonApiService>(s.apiService)
        assertTrue(api.canTrust)
        assertEquals("Untrusted", api.reason)
        assertEquals("mihon-untrusted:${MihonFixture.PACKAGE}", api.serviceName)
    }

    @Test fun trustIdLoadsWithoutRepoKey() {
        assertIs<MihonApiService>(loader.load(catalog, emptySet(), setOf(fixtureTrustId)) {}.first().apiService)
    }

    @Test fun onTrustReceivesExactTrustId() = runTest {
        var captured: String? = null
        val api = assertIs<UntrustedMihonApiService>(
            loader.load(catalog, emptySet(), emptySet()) { captured = it }.single().apiService
        )
        api.trust()
        assertEquals(fixtureTrustId, captured)
    }

    @Test fun brokenPackageDoesNotAbortOthers() {
        val brokenPkg = "eu.kanade.tachiyomi.extension.en.broken"
        apkDir.mkdirs()
        File(apkDir, "$brokenPkg.apk").writeText("not an apk")
        val brokenInfo = MihonApkInfo(
            packageName = brokenPkg,
            versionName = "1.6.1",
            versionCode = 1,
            label = "Broken",
            isExtension = true,
            libVersion = 1.6,
            sourceClasses = listOf("$brokenPkg.Missing"),
            nsfw = false,
            fingerprints = listOf(MihonFixture.REPO_FINGERPRINT),
        )
        val seamStore = MihonApkStore(
            apkDir = apkDir,
            jarCacheDir = jarDir,
            inspect = { if (it.name.startsWith(brokenPkg)) brokenInfo else MihonApkInspector.inspect(it) },
            convert = { apk, dir ->
                if (apk.name.startsWith(brokenPkg)) File(dir, "broken.jar").apply { writeText("garbage") }
                else DexConverter.convert(apk, dir)
            },
        )
        val seamLoader = JvmMihonExtensionLoader(seamStore)
        try {
            val sources = seamLoader.load(catalog, setOf(MihonFixture.REPO_FINGERPRINT), emptySet()) {}
            assertTrue(sources.any { it.packageName == MihonFixture.PACKAGE && it.apiService is MihonApiService })
            val broken = assertIs<UntrustedMihonApiService>(sources.single { it.packageName == brokenPkg }.apiService)
            assertTrue(broken.reason.startsWith("Failed to load: "), broken.reason)
        } finally {
            seamLoader.close()
        }
    }
}

internal fun testCatalog(
    store: MihonApkStore,
    customUrls: List<String> = emptyList(),
    repoClient: MihonRepoClient = MihonRepoClient(HttpClient(MockEngine { throw IOException("offline") })),
) = MihonCatalog(
    application = Application("mihon-test", Files.createTempDirectory("mihondata").toFile()),
    repoClient = repoClient,
    store = store,
    customUrls = { customUrls },
    trustedIds = { emptySet() },
    addTrusted = {},
)
