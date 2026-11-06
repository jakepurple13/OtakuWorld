@file:OptIn(ExperimentalTime::class)

package com.programmersbox.kmpuiviews.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String,
    val name: String,
) {
    fun getUpdatedTime() = runCatching {
        assets
            .maxByOrNull { Instant.parse(it.updatedAt) }
            ?.updatedAt
            ?: createdAt
    }
        .map { Instant.parse(it) }
        .getOrDefault(Clock.System.now())
}

@Serializable
data class GitHubAssets(
    @SerialName("browser_download_url")
    val url: String,
    val name: String,
    @SerialName("updated_at")
    val updatedAt: String,
)