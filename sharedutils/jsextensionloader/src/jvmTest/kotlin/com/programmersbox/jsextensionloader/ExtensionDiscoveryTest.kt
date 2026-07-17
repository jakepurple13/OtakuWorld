package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtensionDiscoveryTest {

    @Test
    fun scanLocalDirectoryFindsJsAndTsFilesWithCompanionManifests() = runTest {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        try {
            File(tempDir, "one.js").writeText("// name: One\n// version: 1.0.0\n")
            File(tempDir, "one.manifest.json").writeText("""{"name":"One","version":"1.0.0"}""")
            File(tempDir, "two.ts").writeText("// name: Two\n// version: 1.0.0\n")
            File(tempDir, "ignored.txt").writeText("not an extension")

            val discovery = ExtensionDiscovery(
                extensionsDir = { tempDir },
                bundledResourcesDir = "js_extensions",
                client = HttpClient(MockEngine { respond("") }),
            )

            val sources = discovery.scanLocalDirectory().sortedBy { it.sourceId }

            assertEquals(2, sources.size)
            assertEquals("one", sources[0].sourceId)
            assertEquals("""{"name":"One","version":"1.0.0"}""", sources[0].companionManifestJson)
            assertEquals("two", sources[1].sourceId)
            assertNull(sources[1].companionManifestJson)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun fetchRemoteDownloadsScriptText() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = SampleExtensionFixture.SCRIPT_TEXT,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/javascript"),
            )
        }
        val discovery = ExtensionDiscovery(
            extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
            bundledResourcesDir = "js_extensions",
            client = HttpClient(mockEngine),
        )

        val source = discovery.fetchRemote("https://example.com/sample-extension.js")

        assertEquals("sample-extension", source.sourceId)
        assertEquals(SampleExtensionFixture.SCRIPT_TEXT, source.scriptText)
    }
}
