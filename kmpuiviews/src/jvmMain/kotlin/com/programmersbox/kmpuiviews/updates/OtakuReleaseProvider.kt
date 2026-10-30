package com.programmersbox.kmpuiviews.updates

import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.updater.provider.UpdateProvider
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Picks the GitHub release to update from for the app's [track]:
 * stable tracks take the newest `vX.Y.Z` release, nightly tracks take whichever of the
 * newest nightly and newest stable has the higher version.
 */
class OtakuReleaseProvider(
    private val track: UpdateTrack,
    private val owner: String = "jakepurple13",
    private val repo: String = "OtakuWorld",
    private val apiBase: String = "https://api.github.com",
    private val webBase: String = "https://github.com",
) : UpdateProvider {

    /** The release the last [resolveMetadataUrl] call picked. */
    @Volatile
    var resolvedRelease: GhRelease? = null
        private set

    override fun getUpdateMetadataUrl(channel: String, platform: Platform): String =
        throw UnsupportedOperationException("Resolved dynamically via resolveMetadataUrl")

    override fun resolveMetadataUrl(channel: String, platform: Platform, httpClient: HttpClient): String {
        val ymlName = metadataFileName(channel, platform)
        val releases = fetchReleases(httpClient)
        val stable = selectStable(releases, ymlName)
        val chosen = when (track) {
            UpdateTrack.Stable -> stable
            UpdateTrack.Nightly -> {
                val nightly = selectNightly(releases, ymlName)
                pickHigher(
                    nightly?.let { it to fetchVersion(httpClient, it, ymlName) },
                    stable?.let { it to fetchVersion(httpClient, it, ymlName) },
                )
            }
        } ?: throw NoSuchElementException("No release with $ymlName")
        resolvedRelease = chosen
        return downloadUrl(chosen.tagName, ymlName)
    }

    override fun getDownloadUrl(fileName: String, version: String): String =
        downloadUrl(resolvedRelease!!.tagName, fileName)

    private fun downloadUrl(tag: String, fileName: String) =
        "$webBase/$owner/$repo/releases/download/$tag/$fileName"

    private fun get(httpClient: HttpClient, url: String, accept: String? = null): String {
        val builder = HttpRequest.newBuilder().uri(URI.create(url))
        if (accept != null) builder.header("Accept", accept)
        val response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) throw IOException("HTTP ${response.statusCode()} for $url")
        return response.body()
    }

    private fun fetchReleases(httpClient: HttpClient): List<GhRelease> {
        val all = mutableListOf<GhRelease>()
        for (page in 1..MAX_PAGES) {
            val body = get(
                httpClient,
                "$apiBase/repos/$owner/$repo/releases?per_page=$PER_PAGE&page=$page",
                "application/vnd.github+json",
            )
            val items = json.decodeFromString<List<GhRelease>>(body)
            all += items
            if (items.size < PER_PAGE) break
        }
        return all
    }

    private fun fetchVersion(httpClient: HttpClient, release: GhRelease, ymlName: String): String =
        get(httpClient, downloadUrl(release.tagName, ymlName))
            .lineSequence()
            .firstOrNull { it.startsWith("version:") }
            ?.substringAfter(':')
            ?.trim()
            ?.trim('"', '\'')
            ?.takeIf { it.isNotEmpty() }
            ?: throw IOException("No version in $ymlName of ${release.tagName}")

    private companion object {
        const val PER_PAGE = 100
        const val MAX_PAGES = 5
        val json = Json { ignoreUnknownKeys = true }
    }
}
