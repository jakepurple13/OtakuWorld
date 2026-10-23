package com.programmersbox.aniyomi

import android.app.Application
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AniyomiCatalogJvmTest {
    private val root = Files.createTempDirectory("aniyomicatalog").toFile()
    private val store = AniyomiApkStore(File(root, "apks"), File(root, "jars"))

    init {
        AniyomiRuntime.init(Application("aniyomi-test", File(root, "data")))
    }

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private suspend fun installFixture() {
        store.install(File(root, "dl.apk").also { AniyomiFixture.apk.copyTo(it) })
    }

    @Test fun initializeSurvivesOfflineRepo() = runTest {
        installFixture()
        val catalog = testCatalog(store, customUrls = listOf("https://repo.example/index.min.json"))
        assertEquals("Aniyomi", catalog.name)
        assertTrue(catalog.hasRemoteSources)
        catalog.initialize()
        val api = assertIs<UntrustedAniyomiApiService>(catalog.getSources().single().apiService)
        assertEquals("Untrusted", api.reason)
        assertTrue(catalog.shouldReload(AniyomiFixture.PACKAGE))
    }

    @Test fun catalogUninstallRemovesSource() = runTest {
        installFixture()
        val catalog = testCatalog(store)
        catalog.initialize()
        assertTrue(catalog.uninstall(catalog.getSources().single()))
        assertTrue(store.installed().isEmpty())
        catalog.initialize()
        assertTrue(catalog.getSources().isEmpty())
    }
}
