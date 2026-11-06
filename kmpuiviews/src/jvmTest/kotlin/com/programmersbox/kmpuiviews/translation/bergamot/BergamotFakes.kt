package com.programmersbox.kmpuiviews.translation.bergamot

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import java.security.MessageDigest

internal const val TEST_RECORDS_URL = "https://settings.test/records"
internal const val TEST_ATTACHMENT_BASE = "https://cdn.test/"

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** A fake attachment: [bytes] served at `TEST_ATTACHMENT_BASE + location`; [hash] defaults to the real sha256. */
internal class FakeFile(
    val from: String,
    val to: String,
    val type: String,
    val bytes: ByteArray,
    val version: String = "1.0",
    val hash: String = sha256Hex(bytes),
) {
    val location = "loc/$from$to-$version-$type.bin"
}

/** Three files (model, lex, vocab) for one pair, [size] bytes each. */
internal fun fakePair(from: String, to: String, size: Int): List<FakeFile> =
    listOf("model", "lex", "vocab").map { type -> FakeFile(from, to, type, ByteArray(size) { (it + type.length).toByte() }) }

internal fun recordsJson(files: List<FakeFile>): String = files.joinToString(",", prefix = "{\"data\":[", postfix = "]}") {
    """{"fromLang":"${it.from}","toLang":"${it.to}","version":"${it.version}","fileType":"${it.type}","filter_expression":"",""" +
            """"attachment":{"hash":"${it.hash}","size":${it.bytes.size},"location":"${it.location}","filename":"x","mimetype":"application/octet-stream"}}"""
}

/**
 * Serves [files] and their index. [failures] maps a location to how many requests fail with 500 before it succeeds.
 */
internal class FakeServer(
    var files: List<FakeFile>,
    val failures: MutableMap<String, Int> = mutableMapOf(),
    var indexBody: () -> String = { recordsJson(files) },
) {
    val requests = mutableListOf<String>()

    val client = HttpClient(MockEngine { request ->
        val url = request.url.toString()
        synchronized(requests) { requests += url }
        if (url == TEST_RECORDS_URL) return@MockEngine respond(indexBody())
        val file = files.firstOrNull { TEST_ATTACHMENT_BASE + it.location == url }
            ?: return@MockEngine respond("", HttpStatusCode.NotFound)
        val left = synchronized(failures) { failures[file.location] ?: 0 }
        if (left > 0) {
            synchronized(failures) { failures[file.location] = left - 1 }
            return@MockEngine respond("boom", HttpStatusCode.InternalServerError)
        }
        respond(ByteReadChannel(file.bytes), HttpStatusCode.OK)
    })

    fun index(cacheFile: java.io.File? = null) =
        BergamotIndex(client, cacheFile = cacheFile, recordsUrl = TEST_RECORDS_URL, attachmentBase = TEST_ATTACHMENT_BASE)
}

/** Records every translate call; output is "[text|model file name]". */
internal class FakeRuntime(var reason: String? = null) : BergamotRuntime {
    val calls = mutableListOf<Pair<String, BergamotModelFiles>>()
    override suspend fun unavailableReason(): String? = reason
    override suspend fun install(onProgress: (Float) -> Unit) = Unit
    override suspend fun translate(text: String, model: BergamotModelFiles): String {
        calls += text to model
        return "[$text|${model.model.parentFile.name}]"
    }
}
