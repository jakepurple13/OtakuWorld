package com.programmersbox.kmpmodels

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StreamContractTest {
    @Test
    fun defaultsKeepOldBehavior() = runTest {
        val storage = KmpStorage()
        assertEquals(emptyList(), storage.subtitles)
        assertNull(storage.server)
        assertNull(storage.mimeType)
        assertTrue(storage.extras.isEmpty())

        val service = object : KmpApiService {
            override val baseUrl = "http://example.com"
        }
        assertTrue(service.canCast)
        val s = KmpStorage(link = "x")
        assertSame(s, service.resolve(s))

        val catalog = object : KmpExternalCustomApiServicesCatalog {
            override val name = "test"
            override suspend fun initialize() = Unit
            override fun getSources() = emptyList<KmpSourceInformation>()
            override val hasRemoteSources = false
        }
        val remote = KmpRemoteSources("n", "p", "1", "", "", emptyList())
        assertFalse(catalog.install(remote))
        assertFalse(catalog.uninstall(KmpSourceInformation(service, "n", null, "p")))
    }
}
