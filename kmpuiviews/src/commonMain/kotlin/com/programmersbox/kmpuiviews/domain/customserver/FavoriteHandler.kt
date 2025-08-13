package com.programmersbox.kmpuiviews.domain.customserver

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.utils.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

interface FavoriteHandler {
    suspend fun getFavorites(): List<DbModel>
    suspend fun getFavorite(url: String): List<DbModel>
    suspend fun addFavorite(dbModel: DbModel)
    suspend fun removeFavorite(dbModel: DbModel)

    suspend fun getChapter(url: String): List<ChapterWatched>
    suspend fun getChapters(dbModel: DbModel): List<ChapterWatched>
    suspend fun addChapter(chapterWatched: ChapterWatched)
    suspend fun removeChapter(chapterWatched: ChapterWatched)
}

internal class FakeFavoriteHandler : FavoriteHandler {
    override suspend fun getFavorites() = emptyList<DbModel>()
    override suspend fun getFavorite(url: String): List<DbModel> = emptyList()
    override suspend fun addFavorite(dbModel: DbModel) = Unit
    override suspend fun removeFavorite(dbModel: DbModel) = Unit

    override suspend fun getChapters(dbModel: DbModel) = emptyList<ChapterWatched>()
    override suspend fun getChapter(url: String): List<ChapterWatched> = emptyList()
    override suspend fun addChapter(chapterWatched: ChapterWatched) = Unit
    override suspend fun removeChapter(chapterWatched: ChapterWatched) = Unit
}

internal class FavoriteHandlerImpl(
    val appConfig: AppConfig,
    val client: HttpClient,
) : FavoriteHandler {
    override suspend fun getFavorites() = runCatchLog(emptyList()) {
        client.get("/otaku/favorites/${appConfig.appName}").body<List<DbModel>>()
    }

    override suspend fun getFavorite(url: String) = runCatchLog(emptyList()) {
        client.get("/otaku/favorites/item") {
            contentType(ContentType.Application.Json)
            setBody(url)
        }.body<List<DbModel>>()
    }

    override suspend fun addFavorite(dbModel: DbModel) {
        runCatchLog("") {
            client.post("/otaku/favorites") {
                contentType(ContentType.Application.Json)
                setBody(dbModel.toCustomServerDbModel())
            }.bodyAsText()
        }
    }

    override suspend fun removeFavorite(dbModel: DbModel) {
        runCatchLog("") {
            client.delete("/otaku/favorites") {
                contentType(ContentType.Application.Json)
                setBody(dbModel.toCustomServerDbModel())
            }.bodyAsText()
        }
    }

    private fun DbModel.toCustomServerDbModel() = CustomServerDbModel(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        source = source,
        numChapters = numChapters,
        shouldCheckForUpdate = shouldCheckForUpdate,
        type = appConfig.appName
    )

    override suspend fun getChapter(url: String): List<ChapterWatched> = runCatchLog(emptyList()) {
        client.get("/otaku/chapters/item") {
            contentType(ContentType.Application.Json)
            setBody(url)
        }.body<List<ChapterWatched>>()
    }

    override suspend fun getChapters(dbModel: DbModel) = runCatchLog(emptyList()) {
        client.get("/otaku/chapters") {
            contentType(ContentType.Application.Json)
            setBody(dbModel.toCustomServerDbModel())
        }.body<List<ChapterWatched>>()
    }

    override suspend fun addChapter(chapterWatched: ChapterWatched) {
        runCatchLog("") {
            client.post("/otaku/chapters") {
                contentType(ContentType.Application.Json)
                setBody(chapterWatched)
            }.bodyAsText()
        }
    }

    override suspend fun removeChapter(chapterWatched: ChapterWatched) {
        runCatchLog("") {
            client.delete("/otaku/chapters") {
                contentType(ContentType.Application.Json)
                setBody(chapterWatched)
            }.bodyAsText()
        }
    }
}

@Serializable
private data class CustomServerDbModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: String,
    val numChapters: Int,
    val shouldCheckForUpdate: Boolean,
    val type: String,
)