package com.programmersbox.uiviews

import android.app.Application
import com.programmersbox.models.ApiService
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.Sources
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OtakuWorldCatalog(
    override val name: String,
) : ExternalApiServicesCatalog, KoinComponent {

    val genericInfo: GenericInfo by inject()

    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
        }
    }

    private suspend fun remoteSources() = client.get("${REPO_URL_PREFIX}index.min.json")
        .bodyAsText()
        .let { json.decodeFromString<List<ExtensionJsonObject>>(it) }

    override suspend fun initialize(app: Application) {}

    override fun getSources(): List<SourceInformation> = listOf(
        //TODO: Gotta think about this....Not a fan of it...
        SourceInformation(
            apiService = object : ApiService {
                override val baseUrl: String get() = "https://github.com/jakepurple13/OtakuWorld"
                override val serviceName: String get() = name
                override val notWorking: Boolean get() = true
            },
            name = name,
            icon = null,
            packageName = "",
            catalog = this
        )
    )

    override suspend fun getRemoteSources(): List<RemoteSources> = remoteSources()
        .filter { genericInfo.sourceType == it.feature }
        .map {
            RemoteSources(
                name = it.name,
                iconUrl = "${REPO_URL_PREFIX}icon/${it.pkg}.png",
                downloadLink = "${REPO_URL_PREFIX}apk/${it.apk}",
                sources = it.sources
                    ?.map { j ->
                        Sources(
                            name = j.name,
                            baseUrl = j.baseUrl,
                            version = it.version
                        )
                    }
                    .orEmpty()
            )
        }

    override val hasRemoteSources: Boolean = true
}

private fun ExtensionJsonObject.extractLibVersion(): Double {
    return version.substringBeforeLast('.').toDouble()
}

private const val REPO_URL_PREFIX = "https://raw.githubusercontent.com/jakepurple13/OtakuWorldSources/repo/"

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val feature: String,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: String,
    val lang: String,
    val name: String,
    val baseUrl: String,
    val versionId: Int,
)
