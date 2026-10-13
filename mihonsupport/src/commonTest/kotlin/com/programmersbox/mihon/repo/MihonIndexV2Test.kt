package com.programmersbox.mihon.repo

import com.programmersbox.kmpmodels.KmpRemoteSources
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

internal fun v2Extension(
    name: String,
    pkg: String,
    lib: String,
    warning: V2ContentWarning = V2ContentWarning.CONTENT_WARNING_SAFE,
) = V2Extension(
    name = name,
    packageName = pkg,
    resources = V2Resources(apkUrl = "https://cdn.example/$pkg.apk", iconUrl = "https://cdn.example/$pkg.png"),
    extensionLib = lib,
    versionCode = 7,
    versionName = "$lib.7",
    contentWarning = warning,
    sources = listOf(V2Source(id = 42, name = "$name Source", language = "en", homeUrl = "https://$name.example")),
)

internal val V2_EXTENSIONS = V2ExtensionList(
    listOf(
        v2Extension("Dex", "pkg.dex", "1.4", V2ContentWarning.CONTENT_WARNING_NSFW),
        v2Extension("Six", "pkg.six", "1.6"),
        v2Extension("Five", "pkg.five", "1.5"),
        v2Extension("Three", "pkg.three", "1.3"),
    )
)

internal fun repoJson(indexUrl: String, key: String? = "metaKey"): String {
    val keyField = key?.let { ""","signingKeyFingerprint":"$it"""" } ?: ""
    return """{"index_v2":"$indexUrl","meta":{"name":"Store","website":"https://store.example"$keyField}}"""
}

/** Serves each URL in [routes] (exact match) with a String or ByteArray body; anything else is 404. */
internal fun v2Client(routes: Map<String, () -> Any>) = HttpClient(MockEngine { request ->
    when (val body = routes[request.url.toString()]?.invoke()) {
        is ByteArray -> respond(body)
        is String -> respond(body)
        else -> respondError(HttpStatusCode.NotFound)
    }
})

internal fun assertV2Mapping(result: List<KmpRemoteSources>) {
    assertEquals(listOf("pkg.dex", "pkg.six"), result.map { it.packageName })
    val dex = result.first()
    assertEquals("Dex (18+)", dex.name)
    assertEquals("1.4.7", dex.version)
    assertEquals("https://cdn.example/pkg.dex.png", dex.iconUrl)
    assertEquals("https://cdn.example/pkg.dex.apk", dex.downloadLink)
    val source = dex.sources.single()
    assertEquals("Dex Source", source.name)
    assertEquals("https://Dex.example", source.baseUrl)
    assertEquals("1.4.7", source.version)
    assertEquals("Six", result[1].name)
}

@OptIn(ExperimentalSerializationApi::class)
class MihonIndexV2Test {
    private val base = "https://store.example"
    private val indexUrl = "https://cdn.example/index.pb"

    private fun protoIndex(signingKey: String = "indexKey", extensions: V2Extensions? = V2InlineList(V2_EXTENSIONS)) =
        ProtoBuf.encodeToByteArray(V2Index(name = "Store", signingKey = signingKey, extensions = extensions))

    @Test
    fun protobufInlineIndex() = runTest {
        val repo = MihonRepoClient(v2Client(mapOf("$base/repo.json" to { repoJson(indexUrl) }, indexUrl to { protoIndex() })))
        assertV2Mapping(repo.fetch(listOf(base)))
        assertEquals(setOf("metaKey"), repo.signingKeys)
    }

    @Test
    fun protobufExtensionListUrl() = runTest {
        val listUrl = "https://cdn.example/list.pb"
        val repo = MihonRepoClient(
            v2Client(
                mapOf(
                    "$base/repo.json" to { repoJson(indexUrl) },
                    indexUrl to { protoIndex(extensions = V2ListUrl(listUrl)) },
                    listUrl to { ProtoBuf.encodeToByteArray(V2_EXTENSIONS) },
                )
            )
        )
        assertV2Mapping(repo.fetch(listOf(base)))
    }

    @Test
    fun jsonIndex() = runTest {
        val ext = { name: String, pkg: String, lib: String, warning: String ->
            """{"name":"$name","packageName":"$pkg","resources":{"apkUrl":"https://cdn.example/$pkg.apk","iconUrl":"https://cdn.example/$pkg.png"},
               "extensionLib":"$lib","versionCode":"7","versionName":"$lib.7","contentWarning":"$warning",
               "sources":[{"id":"42","name":"$name Source","language":"en","homeUrl":"https://$name.example"}]}"""
        }
        val index = """
            {"name":"Store","signingKey":"indexKey","contact":{"website":"https://store.example"},
             "extensionList":{"extensions":[
               ${ext("Dex", "pkg.dex", "1.4", "CONTENT_WARNING_NSFW")},
               ${ext("Six", "pkg.six", "1.6", "CONTENT_WARNING_SAFE")},
               ${ext("Five", "pkg.five", "1.5", "CONTENT_WARNING_SAFE")},
               ${ext("Three", "pkg.three", "1.3", "CONTENT_WARNING_SAFE")}
             ]}}
        """.trimIndent()
        val repo = MihonRepoClient(v2Client(mapOf("$base/repo.json" to { repoJson(indexUrl) }, indexUrl to { index })))
        assertV2Mapping(repo.fetch(listOf(base)))
    }

    @Test
    fun jsonExtensionListUrl() = runTest {
        val listUrl = "https://cdn.example/list.json"
        val list = """{"extensions":[{"name":"Six","packageName":"pkg.six","resources":{"apkUrl":"https://cdn.example/pkg.six.apk"},"extensionLib":"1.6","versionName":"1.6.7"}]}"""
        val repo = MihonRepoClient(
            v2Client(
                mapOf(
                    "$base/repo.json" to { repoJson(indexUrl) },
                    indexUrl to { """{"name":"Store","extensionListUrl":"$listUrl"}""" },
                    listUrl to { list },
                )
            )
        )
        assertEquals(listOf("pkg.six"), repo.fetch(listOf(base)).map { it.packageName })
    }

    @Test
    fun signingKeyFallsBackToIndex() = runTest {
        val repo = MihonRepoClient(
            v2Client(mapOf("$base/repo.json" to { repoJson(indexUrl, key = null) }, indexUrl to { protoIndex() }))
        )
        repo.fetch(listOf(base))
        assertEquals(setOf("indexKey"), repo.signingKeys)
    }

    @Test
    fun httpIndexIsRejected() = runTest {
        val httpIndex = "http://cdn.example/index.pb"
        val repo = MihonRepoClient(v2Client(mapOf("$base/repo.json" to { repoJson(httpIndex) }, httpIndex to { protoIndex() })))
        assertEquals(emptyList(), repo.fetch(listOf(base)))
        assertEquals(emptySet(), repo.signingKeys)
    }

    @Test
    fun httpExtensionListUrlIsRejected() = runTest {
        val listUrl = "http://cdn.example/list.pb"
        val repo = MihonRepoClient(
            v2Client(
                mapOf(
                    "$base/repo.json" to { repoJson(indexUrl) },
                    indexUrl to { protoIndex(extensions = V2ListUrl(listUrl)) },
                    listUrl to { ProtoBuf.encodeToByteArray(V2_EXTENSIONS) },
                )
            )
        )
        assertEquals(emptyList(), repo.fetch(listOf(base)))
        assertEquals(emptySet(), repo.signingKeys)
    }
}
