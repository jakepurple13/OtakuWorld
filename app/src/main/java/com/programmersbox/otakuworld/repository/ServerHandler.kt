package com.programmersbox.otakuworld.repository

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.DbModel

interface ServerHandler {
    suspend fun getFavorites(): List<DbModel>
    suspend fun upsertFavorite(model: DbModel)

    suspend fun getLists(): List<CustomList>
    suspend fun upsertList(list: CustomList)
}

class ServerHandling : ServerHandler {
    override suspend fun getFavorites(): List<DbModel> {
        return emptyList()
    }

    override suspend fun upsertFavorite(model: DbModel) {

    }

    override suspend fun getLists(): List<CustomList> {
        return emptyList()
    }

    override suspend fun upsertList(list: CustomList) {

    }
}