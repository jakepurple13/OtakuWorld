package com.programmersbox.desktop

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.aniyomi.AniyomiApiService
import com.programmersbox.aniyomi.AniyomiApkStore
import com.programmersbox.aniyomi.AniyomiCatalog
import com.programmersbox.aniyomi.AniyomiTrust
import com.programmersbox.aniyomi.aniyomiModule
import com.programmersbox.cloudstream.CloudstreamApiService
import com.programmersbox.cloudstream.CloudstreamCatalog
import com.programmersbox.cloudstream.cloudstreamModule
import com.programmersbox.cloudstream.plugins.InstalledPlugin
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createDataStore
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpuiviews.presentation.settings.extensions.installAllViaCatalogs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The desktop catalogs as Koin wires them in `main.kt`: Aniyomi and CloudStream side by side. */
class CloudstreamDesktopWiringTest {
    private val root = createTempDirectory("animeworld-wiring").toFile()

    // A unique app name keeps the Koin-resolved stores out of the real user directories.
    private val appDirs = AppDirs { appName = "AnimeWorldWiringTest-${UUID.randomUUID()}"; appAuthor = "test" }
    private val settings = NewSettingsHandling(
        createDataStore(FileSystem.SYSTEM, SettingsSerializer()) { File(root, "settings.pb").toOkioPath() },
    )
    private val koin: KoinApplication = koinApplication {
        modules(
            module {
                single { appDirs }
                single { settings }
            },
            aniyomiModule,
            cloudstreamModule,
        )
    }

    @AfterTest
    fun cleanUp() {
        koin.close()
        root.deleteRecursively()
        File(appDirs.getUserDataDir()).deleteRecursively()
        File(appDirs.getUserCacheDir()).deleteRecursively()
    }

    private fun catalogs() = koin.koin.getAll<KmpExternalCustomApiServicesCatalog>()

    private fun remote(link: String) = KmpRemoteSources(
        name = "r", packageName = "pkg", version = "1", iconUrl = "", downloadLink = link, sources = emptyList(),
    )

    @Test
    fun cs3LinkClaimedByCatalog() = runBlocking {
        val catalogs = catalogs()
        assertEquals(setOf("Aniyomi", "CloudStream"), catalogs.map { it.name }.toSet())
        val fallbackLinks = mutableListOf<String>()

        val claimed = installAllViaCatalogs(
            catalogs,
            listOf(remote("https://repo.test/plugins/Some.cs3?x=1")),
        ) { fallbackLinks += it.downloadLink }

        // The CloudStream catalog owns the link (it throws for an unknown one), so the APK installer is never used.
        assertTrue(claimed)
        assertEquals(emptyList(), fallbackLinks)

        // Control: a link neither catalog owns does go to the fallback.
        assertEquals(false, installAllViaCatalogs(catalogs, listOf(remote("https://repo.test/a.apk"))) { fallbackLinks += it.downloadLink })
        assertEquals(listOf("https://repo.test/a.apk"), fallbackLinks)
    }

    @Test
    fun aniyomiAndCloudstreamCoexist() = runBlocking {
        val aniyomi = koin.koin.get<AniyomiCatalog>()
        val cloudstream = koin.koin.get<CloudstreamCatalog>()
        assertTrue(catalogs().let { all -> aniyomi in all && cloudstream in all })

        // Aniyomi: installed APK, trusted by its signing fingerprint.
        koin.koin.get<AniyomiApkStore>().install(File(root, "dl.apk").also { aniyomiFixtureApk().copyTo(it) })
        settings.addTrustedAniyomiExtension(AniyomiTrust.trustId(ANIYOMI_PACKAGE, ANIYOMI_VERSION_CODE, ANIYOMI_FINGERPRINT))

        // CloudStream: installed plugin in a trusted repo.
        val plugins = File(appDirs.getUserDataDir(), "cloudstream-plugins").also { it.mkdirs() }
        val cs3 = cloudstreamFixture().copyTo(File(plugins, "repo/HiAnime.cs3"))
        File(plugins, "installed.json").writeText(
            Json.encodeToString(
                ListSerializer(InstalledPlugin.serializer()),
                listOf(InstalledPlugin(CS_REPO_URL, "HiAnime", 2, cs3.absolutePath)),
            )
        )
        settings.addTrustedCloudstreamRepo(CS_REPO_URL)

        aniyomi.initialize()
        cloudstream.initialize()

        assertIs<AniyomiApiService>(aniyomi.getSources().single().apiService)
        val cloudstreamServices = cloudstream.getSources().map { it.apiService }
        assertTrue(cloudstreamServices.isNotEmpty() && cloudstreamServices.all { it is CloudstreamApiService }, "$cloudstreamServices")
        // Both stay loaded: a second initialize of either does not disturb the other.
        aniyomi.initialize()
        cloudstream.initialize()
        assertEquals(1, aniyomi.getSources().size)
        assertEquals(cloudstreamServices.size, cloudstream.getSources().size)
        assertContains(cloudstream.getSources().map { it.packageName }, "HiAnime")
    }

    // Gradle runs tests from `animeworld/desktop`; the fixtures live in the source_utilities modules' jvmTest resources.
    private fun aniyomiFixtureApk() =
        File("../../source_utilities/aniyomi/src/jvmTest/resources/fixtures/aniyomi-all.animeonsen-v14.10.apk")
            .also { assertTrue(it.isFile, "fixture missing at ${it.absolutePath}") }

    private fun cloudstreamFixture() =
        File("../../source_utilities/cloudstream/src/jvmTest/resources/fixtures/HiAnime.cs3")
            .also { assertTrue(it.isFile, "fixture missing at ${it.absolutePath}") }

    private companion object {
        // Mirror AniyomiFixture / CloudstreamFixture in the source_utilities jvmTest sources (not visible from this module).
        const val ANIYOMI_PACKAGE = "eu.kanade.tachiyomi.animeextension.all.animeonsen"
        const val ANIYOMI_VERSION_CODE = 10L
        const val ANIYOMI_FINGERPRINT = "cbec121aa82ebb02aaa73806992e0368a97d47b5451ed6524816d03084c45905"
        const val CS_REPO_URL = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json"
    }
}
