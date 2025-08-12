package com.programmersbox.kmpuiviews.domain.customserver

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface ListHandler {
    suspend fun getAllLists(): List<CustomList>
    suspend fun addList(customList: CustomList)
    suspend fun removeList(customList: CustomList)
    suspend fun addItem(customListInfo: CustomListInfo)
    suspend fun removeItem(customListInfo: CustomListInfo)
}

internal class ListHandlerImpl(
    val client: HttpClient,
) : ListHandler {
    override suspend fun getAllLists(): List<CustomList> = client.get("/otaku/lists").body()
    override suspend fun addList(customList: CustomList) {
        client.post("/otaku/lists") {
            contentType(ContentType.Application.Json)
            setBody(customList)
        }
    }

    override suspend fun removeList(customList: CustomList) {
        client.delete("/otaku/lists/all/${customList.item.uuid}")
    }

    override suspend fun addItem(customListInfo: CustomListInfo) {
        client.post("/otaku/lists/item") {
            contentType(ContentType.Application.Json)
            setBody(customListInfo)
        }
    }

    override suspend fun removeItem(customListInfo: CustomListInfo) {
        client.delete("/otaku/lists/${customListInfo.uniqueId}")
    }
}