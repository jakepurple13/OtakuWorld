package com.programmersbox.mihon

import android.app.Application
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MihonCatalogJvmTest {
    private val root = Files.createTempDirectory("mihoncatalog").toFile()
    private val store = MihonApkStore(File(root, "apks"), File(root, "jars"))

    init {
        MihonRuntime.init(Application("mihon-test", File(root, "data")))
    }

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private suspend fun installFixture() {
        store.install(File(root, "dl.apk").also { MihonFixture.apk.copyTo(it) })
    }

    @Test fun initializeSurvivesOfflineRepo() = runTest {
        installFixture()
        val catalog = testCatalog(store, customUrls = listOf("https://repo.example/index.min.json"))
        catalog.initialize()
        val api = assertIs<UntrustedMihonApiService>(catalog.getSources().single().apiService)
        assertEquals("Untrusted", api.reason)
        assertTrue(catalog.shouldReload(MihonFixture.PACKAGE))
    }

    @Test fun uninstallRemovesFromStore() = runTest {
        installFixture()
        val catalog = testCatalog(store)
        catalog.initialize()
        assertTrue(catalog.uninstall(catalog.getSources().single()))
        assertTrue(store.installed().isEmpty())
    }
}
