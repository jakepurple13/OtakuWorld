package com.programmersbox.kmpuiviews.domain.customserver

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo

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

/*
internal class ListHandlerImpl(
    val client: HttpClient,
) : ListHandler {
    override suspend fun getAllLists(): List<CustomList> = runCatchLog(emptyList()) {
        client.get("/otaku/lists").body()
    }
    override suspend fun addList(customList: CustomList) {
        runCatchLog(Unit) {
            client.post("/otaku/lists") {
                contentType(ContentType.Application.Json)
                setBody(customList)
            }
        }
    }

    override suspend fun removeList(customList: CustomList) {
        runCatchLog(Unit) {
            client.delete("/otaku/lists/all/${customList.item.uuid}")
        }
    }

    override suspend fun addItem(customListInfo: CustomListInfo) {
        runCatchLog(Unit) {
            client.post("/otaku/lists/item") {
                contentType(ContentType.Application.Json)
                setBody(customListInfo)
            }
        }
    }

    override suspend fun removeItem(customListInfo: CustomListInfo) {
        runCatchLog(Unit) {
            client.delete("/otaku/lists/${customListInfo.uniqueId}")
        }
    }
}*/
