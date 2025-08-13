package com.programmersbox.kmpuiviews.domain.customserver

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.utils.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.providers.digest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface CustomServerHandle : FavoriteHandler, ListHandler {
    val client: HttpClient
    val appConfig: AppConfig

    suspend fun listenToSSE()
}

class CustomServerHandler(
    override val appConfig: AppConfig,
    private val itemDao: ItemDao,
    private val listDao: ListDao,
    override val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json)
        }

        install(SSE) {
            showRetryEvents()
            showCommentEvents()
        }

        install(Auth) {
            basic {

            }

            bearer {

            }

            digest {

            }
        }

        defaultRequest {
            //TODO: Make sure this can change as needed
            url("http://0.0.0.0:8080")
        }
    },
) : CustomServerHandle,
    FavoriteHandler by FakeFavoriteHandler(),
    ListHandler by FakeListHandler()
/*FavoriteHandler by FavoriteHandlerImpl(appConfig, client),
ListHandler by ListHandlerImpl(client)*/ {

    override suspend fun listenToSSE() {
        client.sse("/otaku/sse") {
            incoming.collect { event ->
                logFirebaseMessage("Event: ${event.event}")
                runCatching {
                    val data = Json
                        .decodeFromString<CustomServerEvent>(event.data!!)
                        .id

                    when (EventType.valueOf(event.event!!)) {
                        EventType.NEW_FAVORITE -> {
                            val newFavorites = getFavorite(data)
                            itemDao.insertFavorites(*newFavorites.toTypedArray())
                        }

                        EventType.DELETE_FAVORITE -> {
                            itemDao.getDbModelSync(data)
                                ?.let { model -> itemDao.deleteFavorite(model) }
                        }

                        EventType.NEW_CHAPTER -> {
                            val newChapters = getChapter(data)
                            newChapters.forEach {
                                itemDao.insertChapter(it)
                            }
                        }

                        EventType.DELETE_CHAPTER -> {
                            itemDao.getAllChaptersSync(data)
                                .forEach { chapterWatched -> itemDao.deleteChapter(chapterWatched) }
                        }

                        EventType.ADD_LIST -> {
                            val list = getList(data)
                            list.forEach {
                                listDao.createList(it.item)
                                it.list.forEach { item ->
                                    listDao.addItem(item)
                                }
                            }
                        }

                        EventType.REMOVE_LIST -> {
                            val list = listDao.getCustomListItem(data)
                            listDao.removeList(list)
                        }

                        EventType.ADD_LIST_ITEM -> {
                            val list = getList(data)
                            list.forEach {
                                listDao.createList(it.item)
                                it.list.forEach { item ->
                                    listDao.addItem(item)
                                }
                            }
                        }

                        EventType.REMOVE_LIST_ITEM -> {
                            listDao.removeItem(data)
                        }
                    }
                }
            }
        }
    }
}

@Serializable
data class CustomServerEvent(
    val id: String,
)

enum class EventType {
    NEW_FAVORITE,
    DELETE_FAVORITE,
    NEW_CHAPTER,
    DELETE_CHAPTER,
    ADD_LIST,
    REMOVE_LIST,
    ADD_LIST_ITEM,
    REMOVE_LIST_ITEM,
}

internal suspend fun <T> runCatchLog(defaultValue: T, block: suspend () -> T) = runCatching { block() }
    .onFailure { it.printStackTrace() }
    .getOrDefault(defaultValue)