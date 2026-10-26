package com.programmersbox.cloudstream

import android.app.Application
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.programmersbox.cloudstream.plugins.InstalledPlugin
import com.programmersbox.cloudstream.plugins.PluginLoader
import com.programmersbox.cloudstream.plugins.PluginStore
import com.programmersbox.cloudstream.repo.CloudstreamRepoClient
import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudstreamCatalogJvmTest {
    private val tempDir = createTempDirectory("cloudstream-catalog").toFile()
    private val storeRoot = File(tempDir, "plugins")
    private val jarCacheDir = File(tempDir, "jars")
    private val registrations = GlobalRegistrations()
    private val trusted = mutableSetOf<String>()
    private val store = PluginStore(storeRoot, OkHttpClient())
    private val application = Application("cloudstream-test", File(tempDir, "data"))
    private val catalog = CloudstreamCatalog(
        application = application,
        repoClient = CloudstreamRepoClient(HttpClient(MockEngine { throw IOException("offline") })),
        store = store,
        loader = PluginLoader(jarCacheDir, application),
        trustedRepos = { trusted.toSet() },
        addTrustedRepo = { trusted += it },
    )

    @AfterTest
    fun cleanUp() {
        registrations.removeAdded()
        tempDir.deleteRecursively()
    }

    /** Records [plugins] as installed, the way [PluginStore.install] leaves them. */
    private fun install(vararg plugins: InstalledPlugin) {
        storeRoot.mkdirs()
        File(storeRoot, "installed.json").writeText(
            Json.encodeToString(ListSerializer(InstalledPlugin.serializer()), plugins.toList())
        )
    }

    private fun fixture(): InstalledPlugin {
        CloudstreamFixture.assumeAvailable()
        val cs3 = CloudstreamFixture.cs3.copyTo(File(storeRoot, "repo/${CloudstreamFixture.INTERNAL_NAME}.cs3"))
        return InstalledPlugin(CloudstreamFixture.REPO_URL, CloudstreamFixture.INTERNAL_NAME, CloudstreamFixture.VERSION, cs3.absolutePath)
    }

    private fun testPlugin(name: String, pluginClass: Class<out BasePlugin>, requiresResources: Boolean = false) =
        InstalledPlugin(
            TEST_REPO, name, 1,
            TestPlugins.cs3(File(storeRoot, "repo"), jarCacheDir, name, pluginClass, requiresResources).absolutePath,
        )

    private fun isRegistered(api: MainAPI) = APIHolder.allProviders.withLock { APIHolder.allProviders.any { it === api } }

    @Test
    fun untrustedRepoShowsTrustPrompt() = runTest {
        install(fixture())

        catalog.initialize()

        val source = catalog.getSources().single()
        val api = assertIs<UntrustedCloudstreamApiService>(source.apiService)
        assertTrue(api.canTrust)
        assertEquals("cloudstream-untrusted:${CloudstreamFixture.INTERNAL_NAME}", api.serviceName)
        assertContains(source.name, "Untrusted repo")
        assertEquals(emptyList(), registrations.addedApis(), "untrusted plugin code ran")

        api.trust()
        assertEquals(setOf(CloudstreamFixture.REPO_URL), trusted)
        catalog.initialize()
        assertTrue(catalog.getSources().all { it.apiService is CloudstreamApiService })
    }

    @Test
    fun trustedRepoLoadsSources() = runTest {
        install(fixture())
        trusted += CloudstreamFixture.REPO_URL

        catalog.initialize()

        val services = catalog.getSources().map { assertIs<CloudstreamApiService>(it.apiService) }
        assertTrue(services.isNotEmpty())
        assertTrue(catalog.getSources().all { it.packageName == CloudstreamFixture.INTERNAL_NAME && it.catalog === catalog })
        assertEquals(serviceNamesFor(services.map { it.internalName to it.api }), services.map { it.serviceName })
        assertTrue(services.all { isRegistered(it.api) })
    }

    @Test
    fun uninstallUnregistersApisAndClosesLoader() = runTest {
        install(fixture())
        trusted += CloudstreamFixture.REPO_URL
        catalog.initialize()
        val services = catalog.getSources().map { it.apiService as CloudstreamApiService }
        val classLoader = services.first().api.javaClass.classLoader
        assertNotNull(classLoader.getResource("manifest.json"))

        assertTrue(catalog.uninstall(catalog.getSources().first()))

        assertTrue(catalog.getSources().isEmpty())
        assertTrue(store.installed().isEmpty())
        assertTrue(services.none { isRegistered(it.api) })
        assertEquals(emptyList(), registrations.addedApis())
        assertEquals(emptyList(), registrations.addedExtractors())
        assertNull(classLoader.getResource("manifest.json"), "class loader still open")
        catalog.initialize()
        assertTrue(catalog.getSources().isEmpty())
    }

    @Test
    fun initializeDeletesStaleConvertedJars() = runTest {
        install(fixture())
        trusted += CloudstreamFixture.REPO_URL
        val staleCs3 = TestPlugins.cs3(File(tempDir, "old"), jarCacheDir, "Stale", ConfigurableTestPlugin::class.java)
        val staleJar = DexConverter.jarFor(staleCs3, jarCacheDir)
        assertTrue(staleJar.isFile)

        catalog.initialize()

        assertFalse(staleJar.exists(), "stale jar should be cleaned up")
        val liveJar = DexConverter.jarFor(File(store.installed().single().path), jarCacheDir)
        assertTrue(liveJar.isFile, "loaded plugin's jar was deleted")
    }

    @Test
    fun updateUnregistersOldApisAndClosesOldLoader() = runTest {
        val old = fixture()
        install(old)
        trusted += CloudstreamFixture.REPO_URL
        catalog.initialize()
        val oldServices = catalog.getSources().map { it.apiService as CloudstreamApiService }
        val oldClassLoader = oldServices.first().api.javaClass.classLoader

        // What PluginStore.install records for a newer version.
        val updatedFile = File(old.path).copyTo(File(storeRoot, "repo/${CloudstreamFixture.INTERNAL_NAME}-v3.cs3"))
        install(old.copy(version = old.version + 1, path = updatedFile.absolutePath))
        catalog.initialize()

        val newServices = catalog.getSources().map { assertIs<CloudstreamApiService>(it.apiService) }
        assertTrue(newServices.isNotEmpty())
        assertTrue(oldServices.none { isRegistered(it.api) })
        assertTrue(newServices.all { isRegistered(it.api) })
        assertNull(oldClassLoader.getResource("manifest.json"), "old class loader still open")
        assertTrue(newServices.all { it.api.javaClass.classLoader !== oldClassLoader })
    }

    @Test
    fun failedLoadShowsPlaceholder() = runTest {
        install(
            testPlugin("Throwing", ThrowingTestPlugin::class.java),
            testPlugin("Resources", ConfigurableTestPlugin::class.java, requiresResources = true),
        )
        trusted += TEST_REPO

        catalog.initialize()

        val byName = catalog.getSources().associateBy { it.packageName }
        val throwing = assertIs<UntrustedCloudstreamApiService>(byName.getValue("Throwing").apiService)
        assertContains(throwing.reason, "Failed to load")
        assertFalse(throwing.canTrust)
        val resources = assertIs<UntrustedCloudstreamApiService>(byName.getValue("Resources").apiService)
        assertContains(resources.reason, "Needs Android resources (not supported on desktop)")
        assertFalse(resources.canTrust)
        assertEquals(emptyList(), registrations.addedApis())
    }

    @Test
    fun noConfigurableServicesOnDesktop() = runTest {
        install(fixture(), testPlugin("Configurable", ConfigurableTestPlugin::class.java))
        trusted += setOf(CloudstreamFixture.REPO_URL, TEST_REPO)

        catalog.initialize()

        val services = catalog.getSources().map { it.apiService }
        assertTrue(services.any { (it as? CloudstreamApiService)?.internalName == "Configurable" })
        assertTrue(services.all { it is CloudstreamApiService })
        assertTrue(services.none { it is KmpConfigurableApiService })
    }

    private companion object {
        const val TEST_REPO = "https://repo.test/repo.json"
    }
}
