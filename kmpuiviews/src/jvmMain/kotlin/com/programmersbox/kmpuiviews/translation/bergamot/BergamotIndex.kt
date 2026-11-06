package com.programmersbox.kmpuiviews.translation.bergamot

import com.programmersbox.kmpuiviews.translation.LanguagePair
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val INDEX_TIMEOUT_MS = 15_000L

/**
 * Bergamot HttpClient config. Model files are 30–60 MB, so there is no whole-request limit (a stalled socket
 * still fails after 60 s); the index fetch sets its own 15 s limit. Overrides engine defaults such as CIO's 15 s.
 */
fun HttpClientConfig<*>.installBergamotTimeouts() {
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 60_000
        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
    }
}

/** One model file of a pair. [type] is Mozilla's `fileType`: `model`, `lex`, `vocab`, `srcvocab` or `trgvocab`. */
@Serializable
data class BergamotFile(val pair: String /* "ja-en" */, val type: String, val url: String, val sha256: String, val sizeBytes: Long)

@Serializable
data class BergamotPair(override val from: String, override val to: String, val files: List<BergamotFile>) : LanguagePair {
    val key: String get() = "$from-$to"
    val sizeBytes: Long get() = files.sumOf { it.sizeBytes }
}

/**
 * Firefox's Bergamot models from Mozilla Remote Settings, one record per file. Keeps the latest version per pair;
 * the last good response is cached in [cacheFile] and used when offline.
 */
class BergamotIndex(
    private val client: HttpClient,
    private val cacheFile: File? = null,
    private val recordsUrl: String = RECORDS_URL,
    private val attachmentBase: String = ATTACHMENT_BASE,
) {
    suspend fun pairs(): List<BergamotPair> = try {
        val response = client.get(recordsUrl) { timeout { requestTimeoutMillis = INDEX_TIMEOUT_MS } }
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        val text = response.bodyAsText()
        parse(text).also { // parsed first so a body we can't read never replaces the cache
            withContext(Dispatchers.IO) { cacheFile?.let { runCatching { writeCache(it, text) } } }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val cached = withContext(Dispatchers.IO) { cacheFile?.takeIf { it.isFile }?.readText() } ?: throw e
        parse(cached)
    }

    private fun writeCache(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    internal fun parse(text: String): List<BergamotPair> =
        json.decodeFromString<Records>(text).data
            .groupBy { it.fromLang to it.toLang }
            .mapNotNull { (langs, records) ->
                val releases = records.filter { forDesktopRelease(it.filterExpression) && major(it.version) <= SUPPORTED_MAJOR }
                val latest = releases.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }?.version
                    ?: return@mapNotNull null
                val pair = "${langs.first}-${langs.second}"
                BergamotPair(
                    from = langs.first,
                    to = langs.second,
                    files = releases.filter { it.version == latest }.map {
                        BergamotFile(
                            pair = pair,
                            type = it.fileType,
                            url = attachmentBase + it.attachment.location,
                            sha256 = it.attachment.hash,
                            sizeBytes = it.attachment.size,
                        )
                    },
                )
            }

    @Serializable
    private data class Records(val data: List<Record>)

    @Serializable
    private data class Record(
        val fromLang: String,
        val toLang: String,
        val version: String,
        val fileType: String,
        val attachment: Attachment,
        @SerialName("filter_expression") val filterExpression: String? = null,
    )

    @Serializable
    private data class Attachment(
        val hash: String,
        val size: Long,
        val location: String,
    )

    companion object {
        const val RECORDS_URL = "https://firefox.settings.services.mozilla.com/v1/buckets/main/collections/translations-models/records"
        const val ATTACHMENT_BASE = "https://firefox-settings-attachments.cdn.mozilla.net/"

        private val json = Json { ignoreUnknownKeys = true }

        /** Newest model major version the pinned `translator-cli` (Bergamot v0.6.0) loads; newer majors are skipped. */
        const val SUPPORTED_MAJOR = 2

        /**
         * What desktop Firefox on the release channel gets, by matching the filter forms Remote Settings uses:
         * drops `env.appinfo.OS == 'Android'` records and channel-only ones (`env.channel == 'nightly'` etc.).
         * `env.appinfo.OS != 'Android' || env.channel != 'release'` and empty filters stay.
         */
        private fun forDesktopRelease(filter: String?): Boolean {
            val f = filter.orEmpty()
            return !f.contains("env.appinfo.OS == 'Android'") && !f.contains("env.channel ==")
        }

        private fun numbers(version: String) = version.split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

        private fun major(version: String) = numbers(version).first()

        /** Numeric by part (`2.10` > `2.9`). */
        private fun compareVersions(a: String, b: String): Int {
            val x = numbers(a)
            val y = numbers(b)
            for (i in 0 until maxOf(x.size, y.size)) {
                val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
                if (c != 0) return c
            }
            return 0
        }
    }
}
