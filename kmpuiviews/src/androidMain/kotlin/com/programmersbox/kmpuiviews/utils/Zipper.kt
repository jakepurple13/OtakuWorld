package com.programmersbox.kmpuiviews.utils

import android.content.Context
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
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.ListRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.measureTime

actual open class Zipper(
    private val context: Context,
    private val favoritesRepository: FavoritesRepository,
    private val listDao: ListDao,
    private val listRepository: ListRepository,
    private val itemDao: ItemDao,
    private val heatMapDao: HeatMapDao,
    private val historyDao: HistoryDao,
    private val newSettingsHandling: NewSettingsHandling,
    protected val exceptionDao: ExceptionDao,
) {
    private val handlers = mapOf<String, ZipHandler>(
        "settings" to ZipHandler(
            input = { stream ->
                newSettingsHandling
                    .preferences
                    .updateData { Settings.ADAPTER.decode(stream) }
            },
            output = { out ->
                newSettingsHandling
                    .preferences
                    .data
                    .firstOrNull()
                    ?.encode(out)
            }
        ),
        "favorites.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<DbModel>>(stream.reader().readText())
                    .forEach { favoritesRepository.addFavorite(it) }
            },
            output = {
                dataToOutputStream(favoritesRepository.getAllFavorites(), it)
            }
        ),
        "lists.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<CustomList>>(stream.reader().readText())
                    .forEach {
                        listRepository.createList(it.item)
                        it.list.forEach { listItem -> listRepository.addItem(listItem) }
                    }
            },
            output = { dataToOutputStream(listDao.getAllListsSync(), it) }
        ),
        "history.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<HistoryItem>>(stream.reader().readText())
                    .forEach { historyDao.insertHistory(it) }
            },
            output = { dataToOutputStream(historyDao.getAllHistorySync(), it) }
        ),
        "heat_map.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<HeatMapItem>>(stream.reader().readText())
                    .forEach { heatMapDao.insertHeatMap(it) }
            },
            output = { dataToOutputStream(heatMapDao.getAllHeatMapsSync(), it) }
        ),
        "chapter_watched.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<ChapterWatched>>(stream.reader().readText())
                    .forEach { itemDao.insertChapter(it) }
            },
            output = { dataToOutputStream(itemDao.getAllChaptersSync(), it) }
        ),
        "source_order.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<SourceOrder>>(stream.reader().readText())
                    .forEach { itemDao.insertSourceOrder(it) }
            },
            output = { dataToOutputStream(itemDao.getSourceOrderSync(), it) }
        ),
        "incognito_sources.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<IncognitoSource>>(stream.reader().readText())
                    .forEach { itemDao.insertIncognitoSource(it) }
            },
            output = { dataToOutputStream(itemDao.getAllIncognitoSourcesSync(), it) }
        ),
        "notifications.json" to ZipHandler(
            input = { stream ->
                Json.decodeFromString<List<NotificationItem>>(stream.reader().readText())
                    .forEach { itemDao.insertNotification(it) }
            },
            output = { dataToOutputStream(itemDao.getAllNotifications(), it) }
        ),
        "backupsettings.json" to ZipHandler(
            input = { stream ->
                runCatching {
                    val backupSettings = Json.decodeFromString<BackupSettings>(
                        stream
                            .reader()
                            .readText()
                    )
                    with(backupSettings) {
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
                }
            },
            output = {
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
                }.getOrNull()
            }
        ),
        *additionalHandlers().toList().toTypedArray()
    )

    protected open fun additionalHandlers(): Map<String, ZipHandler> = emptyMap()

    actual suspend fun zipFile(platformFile: PlatformFile) {
        val f = platformFile.uri
        withContext(Dispatchers.IO) {
            val pfd = context
                .contentResolver
                .openFileDescriptor(f, "w")!!
            ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
                handlers.forEach { (name, handler) ->
                    val duration = measureTime {
                        zip.putNextEntry(ZipEntry(name))
                        runCatching { handler.output(zip) }
                            .logFailureToDatabase()
                    }

                    logFirebaseMessage("Zipped $name in $duration")
                }
            }
        }
    }

    actual suspend fun readZip(platformFile: PlatformFile) {
        withContext(Dispatchers.IO) {
            val pfd = context
                .contentResolver
                .openFileDescriptor(platformFile.uri, "r")!!
            pfd.use {
                FileInputStream(it.fileDescriptor).use { inStream ->
                    ZipInputStream(inStream).use { zipIs ->
                        var entry: ZipEntry?
                        while (true) {
                            entry = zipIs.nextEntry
                            if (entry == null) break
                            val duration = measureTime {
                                runCatching { handlers[entry.name]?.input(zipIs) }
                                    .logFailureToDatabase()
                            }
                            logFirebaseMessage("Unzipped ${entry.name} in $duration")
                        }
                    }
                }
            }
        }
    }

    protected suspend fun <T> Result<T>.logFailureToDatabase() = onFailure {
        it.printStackTrace()
        exceptionDao.insertException(it)
    }

    protected inline fun <reified T> dataToOutputStream(data: T, outputStream: OutputStream) {
        Json.encodeToString(data)
            .byteInputStream()
            .copyTo(outputStream)
    }

    protected class ZipHandler(
        val output: suspend (OutputStream) -> Unit,
        val input: suspend (InputStream) -> Unit,
    )
}