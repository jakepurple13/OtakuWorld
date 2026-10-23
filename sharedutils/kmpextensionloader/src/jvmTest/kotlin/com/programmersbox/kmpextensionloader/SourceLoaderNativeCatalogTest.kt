package com.programmersbox.kmpextensionloader

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.NativeCatalogs
import com.programmersbox.kmpmodels.SourceRepository
import java.nio.file.Files
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

    private fun loader(repo: SourceRepository, vararg catalogs: FakeCatalog): SourceLoader {
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
}
