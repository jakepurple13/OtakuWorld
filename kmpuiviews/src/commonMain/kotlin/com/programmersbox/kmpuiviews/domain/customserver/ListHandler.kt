package com.programmersbox.kmpuiviews.domain.customserver

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface ListHandler {
    suspend fun getAllLists(): List<CustomList>
    suspend fun addList(customList: CustomList)
    suspend fun removeList(customList: CustomList)
    suspend fun addItem(customListInfo: CustomListInfo)
    suspend fun removeItem(customListInfo: CustomListInfo)
}

internal class FakeListHandler : ListHandler {
    override suspend fun getAllLists(): List<CustomList> = emptyList()
    override suspend fun addList(customList: CustomList) = Unit
    override suspend fun removeList(customList: CustomList) = Unit
    override suspend fun addItem(customListInfo: CustomListInfo) = Unit
    override suspend fun removeItem(customListInfo: CustomListInfo) = Unit
}

internal class ListHandlerImpl(
    val client: HttpClient,
) : ListHandler {
    override suspend fun getAllLists(): List<CustomList> = runCatchLog(emptyList()) {
        client.get("/otaku/lists").body<List<CustomList>>()
    }
    override suspend fun addList(customList: CustomList) {
        runCatchLog("") {
            client.post("/otaku/lists") {
                contentType(ContentType.Application.Json)
                setBody(customList)
            }.bodyAsText()
        }
    }

    override suspend fun removeList(customList: CustomList) {
        runCatchLog("") {
            client.delete("/otaku/lists/all/${customList.item.uuid}")
                .bodyAsText()
        }
    }

    override suspend fun addItem(customListInfo: CustomListInfo) {
        runCatchLog("") {
            client.post("/otaku/lists/item") {
                contentType(ContentType.Application.Json)
                setBody(customListInfo)
            }.bodyAsText()
        }
    }

    override suspend fun removeItem(customListInfo: CustomListInfo) {
        runCatchLog("") {
            client.delete("/otaku/lists/${customListInfo.uniqueId}")
                .bodyAsText()
        }
    }
}
