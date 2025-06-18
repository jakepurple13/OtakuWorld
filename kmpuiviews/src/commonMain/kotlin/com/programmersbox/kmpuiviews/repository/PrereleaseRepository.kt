package com.programmersbox.kmpuiviews.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PrereleaseRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    suspend fun getReleases() = client
        .get("https://api.github.com/repos/jakepurple13/OtakuWorld/releases")
        .body<List<GitHubPrerelease>>()
}

@Serializable
data class GitHubPrerelease(
    val url: String,
    val prerelease: Boolean,
    val assets: List<GitHubAssets>,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("published_at")
    val publishedAt: Instant,
    val name: String,
) {
    fun getUpdatedTime() = assets
        .maxByOrNull { it.updatedAt }
        ?.updatedAt
        ?: createdAt
}

@Serializable
data class GitHubAssets(
    @SerialName("browser_download_url")
    val url: String,
    val name: String,
    @SerialName("updated_at")
    val updatedAt: Instant,
)