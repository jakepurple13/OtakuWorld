package com.programmersbox.kmpuiviews

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpSources
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
) : KmpExternalApiServicesCatalog, KoinComponent {

    val genericInfo: KmpGenericInfo by inject()

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

    private suspend fun remoteSources() = runCatching {
        client.get("${REPO_URL_PREFIX}index.min.json")
            .bodyAsText()
            .let { json.decodeFromString<List<ExtensionJsonObject>>(it) }
    }
        .getOrNull()
        .orEmpty()

    override suspend fun initialize() {}

    override fun getSources(): List<KmpSourceInformation> = listOf(
        //TODO: Gotta think about this....Not a fan of it...
        KmpSourceInformation(
            apiService = object : KmpApiService {
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

    override suspend fun getRemoteSources(): List<KmpRemoteSources> = remoteSources()
        .filter { genericInfo.sourceType == it.feature }
        .map {
            KmpRemoteSources(
                name = it.name,
                packageName = it.pkg,
                version = it.version,
                iconUrl = "${REPO_URL_PREFIX}icon/${it.pkg}.png",
                downloadLink = "${REPO_URL_PREFIX}apk/${it.apk}",
                sources = it.sources
                    ?.map { j ->
                        KmpSources(
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
