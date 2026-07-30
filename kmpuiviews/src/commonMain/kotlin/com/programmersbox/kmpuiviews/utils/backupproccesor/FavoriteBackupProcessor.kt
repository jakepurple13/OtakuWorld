package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class FavoriteBackupProcessor(
    private val favoritesRepository: FavoritesRepository,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "favorites.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Favorites"
    override val description: String? get() = "Favorited items"
    override val icon get() = Icons.Default.Favorite

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val favorites = favoritesRepository.getAllFavorites()
        favorites.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = favorites.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<DbModel>>().restoreEachCatching(idOf = { it.title }) {
            favoritesRepository.addFavorite(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = favoritesRepository.getAllFavorites().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<DbModel>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
