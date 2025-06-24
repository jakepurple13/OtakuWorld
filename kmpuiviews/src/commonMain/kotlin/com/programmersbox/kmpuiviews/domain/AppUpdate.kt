package com.programmersbox.kmpuiviews.domain

import com.programmersbox.kmpuiviews.utils.printLogs
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object AppUpdate {
    private const val url = "https://raw.githubusercontent.com/jakepurple13/OtakuWorld/master/update.json"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun getUpdate() = runCatching {
        client.get(url)
            .bodyAsText()
            .let { Json.decodeFromString<AppUpdates>(it) }
    }
        .onSuccess { printLogs { it } }
        .onFailure { it.printStackTrace() }
        .getOrNull()

    @Serializable
    data class AppUpdates(
        @SerialName("update_version")
        val updateVersion: Double?,
        @SerialName("update_real_version")
        val updateRealVersion: String?,
        @SerialName("update_url")
        val updateUrl: String?,
        @SerialName("manga_file")
        val mangaFile: String?,
        @SerialName("anime_file")
        val animeFile: String?,
        @SerialName("novel_file")
        val novelFile: String?,
        @SerialName("animetv_file")
        val animetvFile: String?,
        @SerialName("otakumanager_file")
        val otakumanagerFile: String?,
        @SerialName("manga_no_firebase_file")
        val mangaNoFirebaseFile: String?,
        @SerialName("anime_no_firebase_file")
        val animeNoFirebaseFile: String?,
        @SerialName("novel_no_firebase_file")
        val novelNoFirebaseFile: String?,
        @SerialName("animetv_no_firebase_file")
        val animetvNoFirebaseFile: String?,
        @SerialName("manga_no_cloud_file")
        val mangaNoCloudFile: String?,
        @SerialName("anime_no_cloud_file")
        val animeNoCloudFile: String?,
        @SerialName("novel_no_cloud_file")
        val novelNoCloudFile: String?,
        @SerialName("animetv_no_cloud_file")
        val animetvNoCloudFile: String?,
    ) {
        fun downloadUrl(url: AppUpdates.() -> String?) = "$updateUrl${url()}"
    }

    private fun String.removeVariantSuffix() = removeSuffix("-noFirebase")

    fun checkForUpdate(oldVersion: String, newVersion: String): Boolean = try {
        val items = oldVersion.split(".").zip(newVersion.split("."))
        val major = items[0]
        val minor = items[1]
        val patch = items[2]
        /*
         new major > old major
         new major == old major && new minor > old minor
         new major == old major && new minor == old minor && new patch > old patch
         else false
         */
        when {
            major.second.toInt() > major.first.toInt() -> true
            major.second.toInt() == major.first.toInt() && minor.second.toInt() > minor.first.toInt() -> true
            major.second.toInt() == major.first.toInt()
                    && minor.second.toInt() == minor.first.toInt()
                    && patch.second.removeVariantSuffix().toInt() > patch.first.removeVariantSuffix().toInt() -> true

            else -> false
        }
    } catch (e: Exception) {
        false
    }
}

class AppUpdateCheck {
    val updateAppCheck = MutableStateFlow<AppUpdate.AppUpdates?>(null)
}