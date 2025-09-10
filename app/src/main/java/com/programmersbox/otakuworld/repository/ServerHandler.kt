package com.programmersbox.otakuworld.repository

import com.programmersbox.favoritesdatabase.DbModel

interface ServerHandler {
    suspend fun getFavorites(): List<DbModel>
    suspend fun upsertFavorite(model: DbModel)
}

class MockServerHandler : ServerHandler {
    override suspend fun getFavorites(): List<DbModel> = emptyList()

    override suspend fun upsertFavorite(model: DbModel) {

    }
}