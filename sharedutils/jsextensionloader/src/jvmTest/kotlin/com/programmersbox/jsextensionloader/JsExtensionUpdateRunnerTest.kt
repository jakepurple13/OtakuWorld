package com.programmersbox.jsextensionloader

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.extensioninterfaces.ExtensionUpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsExtensionUpdateRunnerTest {

    private class NoOpHostBridge : HostBridge {
        override suspend fun httpGet(url: String, headersJson: String): String = ""
    }

    private val repository = JsExtensionRepository()

    @BeforeTest
    fun setUp() {
        val tempFile = File.createTempFile("test-datastore", ".preferences_pb").also {
            it.delete()
            it.deleteOnExit()
        }
        otakuDataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.absolutePath.toPath() }
        )
    }

    @AfterTest
    fun tearDown() {
        repository.extensions.value.forEach { repository.unload(it.manifest.id) }
    }

    private fun clientReturning(responsesByUrl: Map<String, String>): HttpClient {
        val mockEngine = MockEngine { request ->
            val body = responsesByUrl[request.url.toString()]
                ?: SampleExtensionFixture.SCRIPT_TEXT
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
    fun disabledModeDoesNothing() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.DISABLED) }
        val notified = mutableListOf<ExtensionUpdateInfo>()
        val client = clientReturning(emptyMap())
        val runner = JsExtensionUpdateRunner(
            repository = repository,
            discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = client,
            ),
            loader = JSExtensionLoader(NoOpHostBridge()),
            updateChecker = ExtensionUpdateChecker(client),
            settings = settings,
            registryEndpoint = null,
            onUpdateAvailable = { notified.add(it) },
        )

        runner.run()

        assertTrue(notified.isEmpty())
    }

    @Test
    fun notifyModeReportsAvailableUpdatesWithoutReloading() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.NOTIFY) }
        val notified = mutableListOf<ExtensionUpdateInfo>()
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"sample-extension","latestVersion":"2.0.0","downloadUrl":"https://example.com/sample-extension.js"}]""",
            )
        )
        val runner = JsExtensionUpdateRunner(
            repository = repository,
            discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = client,
            ),
            loader = JSExtensionLoader(NoOpHostBridge()),
            updateChecker = ExtensionUpdateChecker(client),
            settings = settings,
            registryEndpoint = "https://example.com/registry.json",
            onUpdateAvailable = { notified.add(it) },
        )
        repository.register(
            JSExtensionLoader(NoOpHostBridge()).load(
                scriptText = SampleExtensionFixture.SCRIPT_TEXT,
                fileName = "sample-extension.js",
                companionManifestJson = null,
            )
        )

        runner.run()

        assertEquals(1, notified.size)
        assertEquals("sample-extension", notified.first().id)
        assertEquals("1.0.0", repository.extensions.value.first().manifest.version)
    }

    @Test
    fun automaticModeReloadsUpdatedExtension() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.AUTOMATIC) }
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"sample-extension","latestVersion":"2.0.0","downloadUrl":"https://example.com/sample-extension.js"}]""",
                "https://example.com/sample-extension.js" to SampleExtensionFixture.SCRIPT_TEXT,
            )
        )
        val runner = JsExtensionUpdateRunner(
            repository = repository,
            discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = client,
            ),
            loader = JSExtensionLoader(NoOpHostBridge()),
            updateChecker = ExtensionUpdateChecker(client),
            settings = settings,
            registryEndpoint = "https://example.com/registry.json",
            onUpdateAvailable = { },
        )
        repository.register(
            JSExtensionLoader(NoOpHostBridge()).load(
                scriptText = SampleExtensionFixture.SCRIPT_TEXT,
                fileName = "sample-extension.js",
                companionManifestJson = null,
            )
        )

        runner.run()

        assertEquals(1, repository.extensions.value.size)
    }
}
