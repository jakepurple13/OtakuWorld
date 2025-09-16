package com.programmersbox.otakuworld.repository

import com.programmersbox.otakuworld.App
import com.programmersbox.otakuworld.CustomList
import com.programmersbox.otakuworld.CustomListInfo
import com.programmersbox.otakuworld.DbModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface ServerHandler {
    suspend fun getFavorites(app: App): FavoritesData
    suspend fun upsertFavorite(app: App, model: DbModel)
    suspend fun deleteFavorite(app: App, model: DbModel)
    suspend fun getLists(app: App): List<CustomList>
    suspend fun upsertList(app: App, list: CustomList)
    suspend fun deleteList(app: App, list: CustomList)
    suspend fun deleteListItem(app: App, listInfo: CustomListInfo)
}

//This is an example using the custom server
class ServerHandling : ServerHandler {
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        defaultRequest {
            url("http://192.168.1.9:8080")
        }

        install(Logging) {
            level = LogLevel.ALL
        }
    }

    override suspend fun getFavorites(app: App): FavoritesData {
        return client.get("/otaku/favorites/${app.toName()}")
            .body<FavoritesData>()
    }

    override suspend fun upsertFavorite(app: App, model: DbModel) {
        client.post("/otaku/favorites") {
            contentType(ContentType.Application.Json)
            setBody(model.toCustomServerDbModel(app.toName()))
        }.bodyAsText()
    }

    override suspend fun deleteFavorite(app: App, model: DbModel) {
        client.delete("/otaku/favorites") {
            contentType(ContentType.Application.Json)
            setBody(model.toCustomServerDbModel(app.toName()))
        }.bodyAsText()
    }

    override suspend fun getLists(app: App): List<CustomList> {
        return client.get("/otaku/lists")
            .body<List<CustomList>>()
    }

    override suspend fun upsertList(app: App, list: CustomList) {
        client.post("/otaku/lists") {
            contentType(ContentType.Application.Json)
            setBody(list.item)
        }.bodyAsText()
        list.list.forEach { listInfo ->
            client.post("/otaku/lists/item") {
                contentType(ContentType.Application.Json)
                setBody(listInfo)
            }.bodyAsText()
        }
    }

    override suspend fun deleteList(app: App, list: CustomList) {
        client.delete("/otaku/lists/all/${list.item.uuid}")
            .bodyAsText()
    }

    override suspend fun deleteListItem(app: App, listInfo: CustomListInfo) {
        client.delete("/otaku/lists/${listInfo.uniqueId}")
            .bodyAsText()
    }

    private fun App.toName() = when (this) {
        App.AnimeWorld -> "anime"
        App.MangaWorld -> "manga"
        App.NovelWorld -> "novel"
    }
}

private fun DbModel.toCustomServerDbModel(type: String) = CustomServerDbModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source,
    numChapters = numChapters,
    shouldCheckForUpdate = shouldCheckForUpdate,
    type = type
)

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

@Serializable
data class FavoritesData(
    val lastTimeUpdated: Long,
    val favorites: List<DbModel>,
)