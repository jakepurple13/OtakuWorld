package com.programmersbox.kmpuiviews.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.Settings
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.measureTime

open class Backup(
    private val favoritesRepository: FavoritesRepository,
    private val listDao: ListDao,
    private val itemDao: ItemDao,
    private val heatMapDao: HeatMapDao,
    private val historyDao: HistoryDao,
    private val newSettingsHandling: NewSettingsHandling,
    protected val exceptionDao: ExceptionDao,
) {
    suspend fun createBackup(document: PlatformFile) = coroutineScope {
        measureTime {
            val preferences = async {
                runCatching {
                    val map = otakuDataStore.data.firstOrNull()?.asMap()!!

                    BackupSettings(
                        map
                            .filter { it.value is String }
                            .mapKeys { it.key.name }
                            .mapValues { it.value.toString() },
                        map
                            .filter { it.value is Int }
                            .mapKeys { it.key.name }
                            .mapValues { it.value as Int },
                        map
                            .filter { it.value is Long }
                            .mapKeys { it.key.name }
                            .mapValues { it.value as Long },
                        map
                            .filter { it.value is Boolean }
                            .mapKeys { it.key.name }
                            .mapValues { it.value as Boolean },
                        map
                            .filter { it.value is Double }
                            .mapKeys { it.key.name }
                            .mapValues { it.value as Double },
                        map
                            .filter { it.value is ByteArray }
                            .mapKeys { it.key.name }
                            .mapValues { it.value as ByteArray },
                    )
                }
                    .logFailureToDatabase()
                    .getOrNull()
            }

            val newSettings = async {
                newSettingsHandling
                    .preferences
                    .data
                    .firstOrNull()
                    ?.let { Settings.ADAPTER.encode(it) }
            }

            val favorites = async {
                favoritesRepository.getAllFavorites()
            }

            val history = async {
                historyDao.getAllHistorySync()
            }

            val lists = async {
                listDao.getAllListsSync()
            }

            val heatMap = async {
                heatMapDao.getAllHeatMapsSync()
            }

            val notificationItem = async {
                itemDao.getAllNotifications()
            }

            val incognitoSources = async {
                itemDao.getAllIncognitoSourcesSync()
            }

            val chapters = async {
                itemDao.getAllChaptersSync()
            }

            val sourceOrder = async {
                itemDao.getSourceOrderSync()
            }

            val backupItem = BackupItem(
                lists = lists.await(),
                favorites = favorites.await(),
                notifications = notificationItem.await(),
                incognitoSources = incognitoSources.await(),
                chapters = chapters.await(),
                sourceOrder = sourceOrder.await(),
                history = history.await(),
                heatMap = heatMap.await(),
                newSettingsHandling = newSettings.await(),
                backupSettings = preferences.await(),
            )

            writeTo(document, backupItem)
        }
    }

    suspend fun restoreBackup(document: PlatformFile) = coroutineScope {
        measureTime {
            val backupItem = readFrom(document)

            listOf(
                async {
                    backupItem.lists.forEach {
                        listDao.createList(it.item)
                        it.list.forEach { listItem -> listDao.addItem(listItem) }
                    }
                },
                async {
                    backupItem.favorites.forEach {
                        favoritesRepository.addFavorite(it)
                    }
                },
                async {
                    backupItem.notifications.forEach {
                        itemDao.insertNotification(it)
                    }
                },
                async {
                    backupItem.incognitoSources.forEach {
                        itemDao.insertIncognitoSource(it)
                    }
                },
                async {
                    backupItem.chapters.forEach {
                        itemDao.insertChapter(it)
                    }
                },
                async {
                    backupItem.sourceOrder.forEach {
                        itemDao.insertSourceOrder(it)
                    }
                },
                async {
                    backupItem.history.forEach {
                        historyDao.insertHistory(it)
                    }
                },
                async {
                    backupItem.heatMap.forEach {
                        heatMapDao.insertHeatMap(it)
                    }
                },
                async {
                    runCatching {
                        newSettingsHandling.preferences.updateData {
                            Settings.ADAPTER.decode(backupItem.newSettingsHandling!!)
                        }
                    }.logFailureToDatabase()
                },
                async {
                    runCatching {
                        with(backupItem.backupSettings!!) {
                            otakuDataStore.edit { p ->
                                stringSettings.forEach {
                                    p[stringPreferencesKey(it.key)] = it.value
                                }
                                intSettings.forEach {
                                    p[intPreferencesKey(it.key)] = it.value
                                }
                                longSettings.forEach {
                                    p[longPreferencesKey(it.key)] = it.value
                                }
                                booleanSettings.forEach {
                                    p[booleanPreferencesKey(it.key)] = it.value
                                }
                                doubleSettings.forEach {
                                    p[doublePreferencesKey(it.key)] = it.value
                                }
                                byteArraySettings.forEach {
                                    p[byteArrayPreferencesKey(it.key)] = it.value
                                }
                            }
                        }
                    }.logFailureToDatabase()
                }
            ).awaitAll()
        }
    }

    open suspend fun readFrom(document: PlatformFile): BackupItem =
        Json.decodeFromString<BackupItem>(document.readString())

    open suspend fun writeTo(document: PlatformFile, backupItem: BackupItem) =
        document.writeString(Json.encodeToString(backupItem))

    protected suspend fun <T> Result<T>.logFailureToDatabase() = onFailure {
        it.printStackTrace()
        exceptionDao.insertException(it)
    }
}

@Serializable
data class BackupItem(
    val newSettingsHandling: ByteArray?,
    val backupSettings: BackupSettings?,
    val lists: List<CustomList>,
    val favorites: List<DbModel>,
    val notifications: List<NotificationItem>,
    val incognitoSources: List<IncognitoSource>,
    val chapters: List<ChapterWatched>,
    val sourceOrder: List<SourceOrder>,
    val history: List<HistoryItem>,
    val heatMap: List<HeatMapItem>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BackupItem

        if (!newSettingsHandling.contentEquals(other.newSettingsHandling)) return false
        if (backupSettings != other.backupSettings) return false
        if (lists != other.lists) return false
        if (favorites != other.favorites) return false
        if (notifications != other.notifications) return false
        if (incognitoSources != other.incognitoSources) return false
        if (chapters != other.chapters) return false
        if (sourceOrder != other.sourceOrder) return false
        if (history != other.history) return false
        if (heatMap != other.heatMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = newSettingsHandling?.contentHashCode() ?: 0
        result = 31 * result + (backupSettings?.hashCode() ?: 0)
        result = 31 * result + lists.hashCode()
        result = 31 * result + favorites.hashCode()
        result = 31 * result + notifications.hashCode()
        result = 31 * result + incognitoSources.hashCode()
        result = 31 * result + chapters.hashCode()
        result = 31 * result + sourceOrder.hashCode()
        result = 31 * result + history.hashCode()
        result = 31 * result + heatMap.hashCode()
        return result
    }
}

@Serializable
data class BackupSettings(
    val stringSettings: Map<String, String>,
    val intSettings: Map<String, Int>,
    val longSettings: Map<String, Long>,
    val booleanSettings: Map<String, Boolean>,
    val doubleSettings: Map<String, Double>,
    val byteArraySettings: Map<String, ByteArray>,
)