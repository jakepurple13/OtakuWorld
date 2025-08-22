package com.programmersbox.mangaworld

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.BackupItem
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MangaWorldBackup(
    favoritesRepository: FavoritesRepository,
    listDao: ListDao,
    itemDao: ItemDao,
    heatMapDao: HeatMapDao,
    historyDao: HistoryDao,
    newSettingsHandling: NewSettingsHandling,
    exceptionDao: ExceptionDao,
    private val mangaNewSettingsHandling: MangaNewSettingsHandling,
) : Backup(favoritesRepository, listDao, itemDao, heatMapDao, historyDao, newSettingsHandling, exceptionDao) {
    override suspend fun writeTo(document: PlatformFile, backupItem: BackupItem) {
        document.writeString(
            Json.encodeToString(
                MangaWorldBackupItem(
                    backup = backupItem,
                    mangaSettings = mangaNewSettingsHandling
                        .preferences
                        .data
                        .firstOrNull()
                        ?.let { MangaSettings.ADAPTER.encode(it) }
                )
            )
        )
    }

    override suspend fun readFrom(document: PlatformFile): BackupItem {
        val backupItem = Json.decodeFromString<MangaWorldBackupItem>(document.readString())

        runCatching {
            mangaNewSettingsHandling.preferences.updateData {
                MangaSettings.ADAPTER.decode(backupItem.mangaSettings!!)
            }
        }.logFailureToDatabase()

        return backupItem.backup
    }
}

@Serializable
data class MangaWorldBackupItem(
    val backup: BackupItem,
    val mangaSettings: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MangaWorldBackupItem

        if (backup != other.backup) return false
        if (!mangaSettings.contentEquals(other.mangaSettings)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backup.hashCode()
        result = 31 * result + (mangaSettings?.contentHashCode() ?: 0)
        return result
    }
}