package com.programmersbox.kmpmodels

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepoCheckDefaultTest {

    private class FakeCatalog(private val result: () -> List<KmpRemoteSources>) : KmpExternalCustomApiServicesCatalog {
        override val name = "fake"
        override val hasRemoteSources = true
        override suspend fun initialize() = Unit
        override fun getSources(): List<KmpSourceInformation> = emptyList()
        override suspend fun getRemoteSources(customUrls: List<String>) = result()
    }

    private fun remote(packageName: String) = KmpRemoteSources(
        name = packageName,
        packageName = packageName,
        version = "1",
        iconUrl = "",
        downloadLink = "",
        sources = emptyList(),
    )

    @Test
    fun defaultCheckFoundWhenSourcesReturned() = runTest {
        val remotes = listOf(remote("a"), remote("b"))
        assertEquals(RepoCheck.Found("", remotes), FakeCatalog { remotes }.checkRepo("https://x"))
    }

    @Test
    fun defaultCheckNotThisFormatWhenEmpty() = runTest {
        assertEquals(RepoCheck.NotThisFormat, FakeCatalog { emptyList() }.checkRepo("https://x"))
    }

    @Test
    fun defaultCheckFailedWhenThrows() = runTest {
        assertEquals(
            RepoCheck.Failed("boom"),
            FakeCatalog { throw IllegalStateException("boom") }.checkRepo("https://x"),
        )
    }
}
