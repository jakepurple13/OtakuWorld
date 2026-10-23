package com.programmersbox.kmpextensionloader

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.NativeCatalogs
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLoaderNativeCatalogTest {

    private class NamedService(override val serviceName: String) : KmpApiService {
        override val baseUrl: String = "https://example.com/"
    }

    private class FakeCatalog(
        private val names: List<String>,
        private val failOnInit: Boolean = false,
    ) : KmpExternalCustomApiServicesCatalog {
        override val name: String = "fake"
        override val hasRemoteSources: Boolean = false
        override suspend fun initialize() {
            if (failOnInit) error("boom")
        }

        override fun getSources(): List<KmpSourceInformation> = names.map {
            KmpSourceInformation(apiService = NamedService(it), name = it, icon = null, packageName = it)
        }
    }

    /** First initialize() waits for [gate] and then reports "old"; later calls report "new" right away. */
    private class SlowFirstCatalog : KmpExternalCustomApiServicesCatalog {
        val firstStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        private val calls = AtomicInteger()
        @Volatile private var names = emptyList<String>()
        override val name: String = "slow"
        override val hasRemoteSources: Boolean = false

        override suspend fun initialize() {
            if (calls.incrementAndGet() == 1) {
                firstStarted.complete(Unit)
                gate.await()
                names = listOf("old")
            } else {
                names = listOf("new")
            }
        }

        override fun getSources(): List<KmpSourceInformation> = names.map {
            KmpSourceInformation(apiService = NamedService(it), name = it, icon = null, packageName = it)
        }
    }

    private fun loader(repo: SourceRepository, vararg catalogs: KmpExternalCustomApiServicesCatalog): SourceLoader {
        val root = Files.createTempDirectory("sourceloader").toFile().apply { deleteOnExit() }
        val ext = root.resolve("ext").apply { mkdirs() }
        return SourceLoader(
            extensionsDir = { ext },
            sourceType = "manga",
            sourceRepository = repo,
            appDirs = AppDirs {
                appName = "test"
                appAuthor = "test"
            },
            nativeCatalogs = NativeCatalogs(catalogs.toList()),
        )
    }

    @Test fun nativeCatalogSourcesAreMerged() {
        val repo = SourceRepository()

        loader(repo, FakeCatalog(listOf("b", "a"))).load()

        assertEquals(listOf("a", "b"), repo.list.map { it.apiService.serviceName })
    }

    @Test fun failingCatalogIsSkipped() {
        val repo = SourceRepository()

        loader(repo, FakeCatalog(listOf("bad"), failOnInit = true), FakeCatalog(listOf("good"))).load()

        assertEquals(listOf("good"), repo.list.map { it.apiService.serviceName })
    }

    @Test fun slowerEarlierReloadDoesNotPublishLast() = runBlocking {
        val repo = SourceRepository()
        val catalog = SlowFirstCatalog()
        val loader = loader(repo, catalog)

        val first = launch(Dispatchers.Default) { loader.blockingLoad() }
        catalog.firstStarted.await()
        val second = launch(Dispatchers.Default) { loader.blockingLoad() }
        delay(200) // without serialization the second load would finish here
        catalog.gate.complete(Unit)
        first.join()
        second.join()

        assertEquals(listOf("new"), repo.list.map { it.apiService.serviceName })
    }
}
