package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtensionUpdateCheckerTest {

    private fun clientReturning(responsesByUrl: Map<String, String>): HttpClient {
        val mockEngine = MockEngine { request ->
            val body = responsesByUrl.getValue(request.url.toString())
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun findsUpdateFromCentralizedRegistryWhenNewer() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"ext-a","latestVersion":"2.0.0","downloadUrl":"https://example.com/ext-a.js"}]""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(InstalledExtension(id = "ext-a", currentVersion = "1.0.0", updateUrl = null))

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = "https://example.com/registry.json")

        assertEquals(1, updates.size)
        assertEquals("ext-a", updates.first().id)
    }

    @Test
    fun skipsRegistryEntryWhenNotNewer() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"ext-a","latestVersion":"1.0.0","downloadUrl":"https://example.com/ext-a.js"}]""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(InstalledExtension(id = "ext-a", currentVersion = "1.0.0", updateUrl = null))

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = "https://example.com/registry.json")

        assertTrue(updates.isEmpty())
    }

    @Test
    fun fallsBackToPerExtensionUpdateUrlWhenNotInRegistry() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to "[]",
                "https://example.com/ext-b/update.json" to
                    """{"id":"ext-b","latestVersion":"3.0.0","downloadUrl":"https://example.com/ext-b.js"}""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(
            InstalledExtension(id = "ext-b", currentVersion = "2.0.0", updateUrl = "https://example.com/ext-b/update.json"),
        )

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = "https://example.com/registry.json")

        assertEquals(1, updates.size)
        assertEquals("ext-b", updates.first().id)
    }

    @Test
    fun checksBothSourcesWhenNoRegistryEndpointGiven() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/ext-c/update.json" to
                    """{"id":"ext-c","latestVersion":"1.1.0","downloadUrl":"https://example.com/ext-c.js"}""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(
            InstalledExtension(id = "ext-c", currentVersion = "1.0.0", updateUrl = "https://example.com/ext-c/update.json"),
        )

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = null)

        assertEquals(1, updates.size)
        assertEquals("ext-c", updates.first().id)
    }
}
