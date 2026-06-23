package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class FavoriteBackupProcessor(
    private val favoritesRepository: FavoritesRepository,
) : BackupProcessor() {
    override val fileName: String
        get() = "favorites.json"

    override suspend fun backup(sink: BufferedSink) {
        favoritesRepository
            .getAllFavorites()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<DbModel>>().forEach { favoritesRepository.addFavorite(it) }
    }
}