# Backup & Restore Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Push backup/restore failure isolation down to individual rows (not just whole categories), and let the user pick specific custom lists to include when backing up or restoring, from inside the existing wizard.

**Architecture:** `BackupProcessor.backup()`/`restore()` return a new `ProcessorResult(successCount, failed)` instead of `Unit`, built via a shared `restoreEachCatching` helper on the base class; `Zipper` turns that into the existing `ItemResult`. `ListBackupProcessor` gains a settable `listIdFilter: Set<String>?` that `Zipper` sets/resets around its calls — no interface change for the other 14 processors. The wizard's `WizardItemState` gains an optional `subItems` list (populated only for the "Custom Lists" row) rendered as a checklist in `WizardItemRow`; selections thread through `BackupWizardViewModel`/`RestoreWizardViewModel` → `BackgroundWorkHandler` → platform workers → `Zipper`.

**Tech Stack:** Kotlin Multiplatform (Android + JVM/Desktop), Compose Multiplatform, Room3, kotlinx.serialization, Koin, Okio, WorkManager (Android), `DesktopTaskScheduler` (JVM).

## Global Constraints

- Zip file format and JSON structure are unchanged — a selective-list zip is a normal backup zip with a filtered `lists.json` entry.
- The `BackupProcessor` abstract interface changes (`Unit` → `ProcessorResult`) apply to all 15 processors; no other processor gains a filtering hook — only `ListBackupProcessor` does.
- `requiresBiometric` in the list checklist is informational only (`CustomListItem.useBiometric`, aggregated per list) — it enforces nothing.
- No new Koin bindings are needed for the restore-side list picker — `Zipper` already holds every `BackupProcessor` instance and can find `ListBackupProcessor` via `filterIsInstance`.
- Spec: `docs/superpowers/specs/2026-07-29-backup-restore-improvements-design.md`.

---

### Task 1: `ProcessorResult` contract + `restoreEachCatching` helper + `Zipper` result-building

**Files:**
- Modify: `sharedtools/src/commonMain/kotlin/com/programmersbox/sharedtools/BackupProcessor.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt`

**Interfaces:**
- Produces: `data class ProcessorResult(val successCount: Int, val failed: List<String> = emptyList())` in `com.programmersbox.sharedtools`
- Produces: `BackupProcessor.backup(sink: BufferedSink): ProcessorResult` and `BackupProcessor.restore(json: String, bufferedSource: BufferedSource): ProcessorResult` (both now abstract with this return type)
- Produces: `protected suspend inline fun <T> Iterable<T>.restoreEachCatching(idOf: (T) -> String, action: (T) -> Unit): ProcessorResult` on `BackupProcessor`, for subclasses to build per-row results
- Consumes (later tasks build on this): every processor subclass in Task 2 and Task 4 implements the new signatures

- [ ] **Step 1: Update `BackupProcessor.kt` with the new contract**

Replace the whole file:

```kotlin
package com.programmersbox.sharedtools

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.core.qualifier.named
import org.koin.dsl.bind

/**
 * Per-processor backup/restore outcome. [successCount] counts rows that succeeded;
 * [failed] holds a human-readable identifier for each row that threw during restore.
 */
data class ProcessorResult(
    val successCount: Int,
    val failed: List<String> = emptyList(),
)

/**
 * Abstract class representing a backup processor that handles the backup and restoration of data.
 * This class provides methods for converting data to and from JSON format and defines abstract
 * operations for performing the backup and restore tasks.
 */
abstract class BackupProcessor {
    /**
     * Represents the name of a file associated with this entity.
     * This value is expected to store only the file name, not the complete file path.
     * It is typically used to identify or reference the corresponding resource.
     */
    abstract val fileName: String

    /**
     * Backs up data to the specified sink.
     *
     * @param sink The `BufferedSink` where the data will be written during the backup operation.
     */
    abstract suspend fun backup(sink: BufferedSink): ProcessorResult

    /**
     * Restores the state or configuration of an object using the provided JSON string and buffered source.
     *
     * @param json A JSON-formatted string used for restoration.
     * @param bufferedSource A buffered source containing additional data necessary for the restore operation.
     */
    abstract suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult

    /**
     * Restores each row independently: a row whose [action] throws is recorded in
     * [ProcessorResult.failed] (via [idOf]) instead of aborting the remaining rows.
     * `CancellationException` is rethrown so coroutine cancellation still propagates.
     */
    protected suspend inline fun <T> Iterable<T>.restoreEachCatching(
        idOf: (T) -> String,
        action: (T) -> Unit,
    ): ProcessorResult {
        var successCount = 0
        val failed = mutableListOf<String>()
        for (row in this) {
            try {
                action(row)
                successCount++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += idOf(row)
            }
        }
        return ProcessorResult(successCount, failed)
    }

    /**
     * Converts an object of type [T] into its JSON string representation.
     *
     * This method leverages Kotlin's serialization library to serialize the object.
     * The type [T] must be annotated with `@Serializable` for the serialization to succeed.
     *
     * @receiver The object of type [T] to be serialized into a JSON string.
     * @return A JSON-formatted string representing the serialized object.
     * @throws SerializationException If the object cannot be serialized.
     */
    protected inline fun <reified T> T.toJson() = Json.encodeToString(this)

    /**
     * Extension function to deserialize a JSON string into an object of the specified type.
     *
     * This function uses the `kotlinx.serialization` library to decode the JSON content
     * into an instance of the provided type [T]. The type is determined at runtime using
     * reified type parameters.
     *
     * @param T The type into which the JSON string will be deserialized.
     * @receiver The JSON string to be deserialized.
     * @return The deserialized object of type [T].
     * @throws SerializationException If the JSON string cannot be deserialized into the specified type.
     */
    protected inline fun <reified T> String.fromJson() = Json.decodeFromString<T>(this)
}

inline fun <reified T : BackupProcessor> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: () -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

inline fun <reified T : BackupProcessor, reified T1> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: (T1) -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

inline fun <reified T : BackupProcessor, reified T1, reified T2> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: (T1, T2) -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class
```

- [ ] **Step 2: Update Android `Zipper.kt` to build `ItemResult` from `ProcessorResult`**

In `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`, replace the `zipFile` and `readZip` bodies' inner `runCatching` blocks:

```kotlin
    actual suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        val pfd = context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "w")!!
        ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
            backupProcessors.filter { it.fileName in selectedKeys }.forEach { backup ->
                logFirebaseMessage("Zipping ${backup.fileName}")
                val duration = measureTime {
                    zip.putNextEntry(ZipEntry(backup.fileName))
                    val result = runCatching {
                        measureTimedValue {
                            val sink = zip.sink().buffer()
                            val processorResult = backup.backup(sink)
                            sink.flush()
                            processorResult
                        }
                    }
                        .onFailure {
                            it.printStackTrace()
                            exceptionDao.insertException(it)
                        }
                        .fold(
                            onSuccess = { timedValue ->
                                val processorResult = timedValue.value
                                ItemResult(
                                    backup.fileName,
                                    timeTaken = timedValue.duration.toString(),
                                    success = processorResult.successCount > 0,
                                    error = processorResult.failed.takeIf { it.isNotEmpty() }
                                        ?.let { "${it.size} failed: ${it.joinToString()}" },
                                )
                            },
                            onFailure = { e ->
                                ItemResult(
                                    backup.fileName,
                                    timeTaken = e.message ?: "Unknown error",
                                    success = false,
                                    error = e.message
                                )
                            },
                        )
                    results += result
                    onItemComplete(result)
                }
                logFirebaseMessage("Zipped ${backup.fileName} in $duration")
            }
        }
        results
    }

    actual suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val processor = backupProcessors.find { it.fileName == name }
                        if (name in selectedKeys && processor != null) {
                            val duration = measureTime {
                                val result = runCatching {
                                    measureTimedValue {
                                        val bytes = zipIs.readBytes()
                                        processor.restore(
                                            json = bytes.decodeToString(),
                                            bufferedSource = Buffer().apply { write(bytes) },
                                        )
                                    }
                                }
                                    .fold(
                                        onSuccess = { timedValue ->
                                            val processorResult = timedValue.value
                                            ItemResult(
                                                name,
                                                timeTaken = timedValue.duration.toString(),
                                                success = processorResult.successCount > 0,
                                                error = processorResult.failed.takeIf { it.isNotEmpty() }
                                                    ?.let { "${it.size} failed: ${it.joinToString()}" },
                                            )
                                        },
                                        onFailure = { e ->
                                            ItemResult(
                                                name,
                                                timeTaken = e.message ?: "Unknown error",
                                                success = false,
                                                error = e.message
                                            )
                                        },
                                    )
                                results += result
                                onItemComplete(result)
                            }
                            logFirebaseMessage("Unzipped $name in $duration")
                        }
                        entry = zipIs.nextEntry
                    }
                }
            }
        }
        results
    }
```

`peekZip` is unchanged — it doesn't call `backup`/`restore`.

- [ ] **Step 3: Apply the same change to JVM `Zipper.jvm.kt`**

Mirror Step 2's `zipFile`/`readZip` restructuring in `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt` (same logic, no `exceptionDao`/`onFailure` call — that file never had one).

- [ ] **Step 4: Update `ZipperTest.kt`'s `RecordingProcessor` and add a partial-success test**

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import okio.BufferedSink
import okio.BufferedSource
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingProcessor(
    override val fileName: String,
    private val payload: String,
) : BackupProcessor(), BackupUiInfo {
    var restoredWith: String? = null
    override val key get() = fileName
    override val displayName get() = fileName
    override val description: String? = null
    override val icon = null
    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        sink.writeUtf8(payload)
        return ProcessorResult(successCount = 1)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        restoredWith = json
        return ProcessorResult(successCount = 1)
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = 1)
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) =
        BackupDataSummary(itemCount = 1, sizeBytes = rawBytes?.size?.toLong())
}

private class PartiallyFailingProcessor : BackupProcessor(), BackupUiInfo {
    override val fileName = "partial.json"
    override val key get() = fileName
    override val displayName get() = fileName
    override val description: String? = null
    override val icon = null
    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        sink.writeUtf8("payload")
        return ProcessorResult(successCount = 1)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) =
        ProcessorResult(successCount = 2, failed = listOf("bad-row"))

    override suspend fun currentSummary() = BackupDataSummary(itemCount = 1)
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary()
}

class ZipperTest {
    private val tempFile = File.createTempFile("zippertest", ".zip")

    @AfterTest
    fun cleanup() { tempFile.delete() }

    @Test
    fun `zipFile only writes selected keys, readZip only restores selected keys`() = runBlocking {
        val a = RecordingProcessor("a.json", "payload-a")
        val b = RecordingProcessor("b.json", "payload-b")
        val zipper = Zipper(listOf(a, b))
        val platformFile = PlatformFile(tempFile.absolutePath)

        val zipResults = zipper.zipFile(platformFile, setOf("a.json")) { }
        assertEquals(listOf(true), zipResults.map { it.success })
        assertEquals("a.json", zipResults.single().key)

        val restoreResults = zipper.readZip(platformFile, setOf("a.json")) { }
        assertEquals("payload-a", a.restoredWith)
        assertEquals(null, b.restoredWith)
        assertEquals(1, restoreResults.size)
    }

    @Test
    fun `peekZip reports summaries without calling restore`() = runBlocking {
        val a = RecordingProcessor("a.json", "payload-a")
        val zipper = Zipper(listOf(a))
        val platformFile = PlatformFile(tempFile.absolutePath)

        zipper.zipFile(platformFile, setOf("a.json")) { }
        val summaries = zipper.peekZip(platformFile, listOf(a))

        assertTrue(summaries["a.json"]?.sizeBytes != null && summaries["a.json"]!!.sizeBytes!! > 0)
        assertEquals(null, a.restoredWith)
    }

    @Test
    fun `readZip reports partial success when a processor's ProcessorResult has failures`() = runBlocking {
        val partial = PartiallyFailingProcessor()
        val zipper = Zipper(listOf(partial))
        val platformFile = PlatformFile(tempFile.absolutePath)

        zipper.zipFile(platformFile, setOf("partial.json")) { }
        val restoreResults = zipper.readZip(platformFile, setOf("partial.json")) { }

        val result = restoreResults.single()
        assertTrue(result.success)
        assertEquals("1 failed: bad-row", result.error)
    }
}
```

- [ ] **Step 5: Run the test suite for this module**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.ZipperTest"`
Expected: This will FAIL to compile until Task 2 updates every processor's overrides — that's expected; proceed to Task 2 before running this again.

- [ ] **Step 6: Commit**

```bash
git add sharedtools/src/commonMain/kotlin/com/programmersbox/sharedtools/BackupProcessor.kt \
  kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt \
  kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt \
  kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt
git commit -m "feat: add ProcessorResult contract for per-row backup/restore failure isolation"
```

---

### Task 2: Migrate the 14 non-list processors to the new contract

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/FavoriteBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/HistoryBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ChaptersWatchedBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/BookmarksBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NotesBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/HeatMapBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/RecommendationsBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NotificationsBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/DictionaryBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/SourceOrderBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/IncognitoBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ActivityBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NewSettingsBackupProcessor.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/BackupSettingsProcessor.kt`

**Interfaces:**
- Consumes: `ProcessorResult` and `restoreEachCatching` from Task 1's `BackupProcessor`
- Produces: nothing new consumed by later tasks (each processor's `backup`/`restore` return type is the only change; behavior for successful runs is identical to before)

11 of these (`Favorite`, `History`, `ChaptersWatched`, `Bookmarks`, `Notes`, `HeatMap`, `Recommendations`, `Notifications`, `Dictionary`, `SourceOrder`, `Incognito`) loop over a `List<T>` in `restore()` — each row now runs through `restoreEachCatching`. `backup()` in every one of these just serializes the whole list in one shot (no per-row loop exists there), so it only needs its return type wrapped. The other 3 (`Activity`, `NewSettings`, `BackupSettings`) are single-blob processors — no loop at all — they just wrap their existing return value.

- [ ] **Step 1: `FavoriteBackupProcessor.kt`**

```kotlin
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
```

- [ ] **Step 2: `HistoryBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class HistoryBackupProcessor(
    private val historyDao: HistoryDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "history.json"

    override val key: String get() = fileName
    override val displayName: String get() = "History"
    override val description: String? get() = "Viewing/reading history"
    override val icon get() = Icons.Default.History

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val history = historyDao.getAllHistorySync()
        history.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = history.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<HistoryItem>>().restoreEachCatching(idOf = { it.searchText }) {
            historyDao.insertHistory(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = historyDao.getAllHistorySync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HistoryItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 3: `ChaptersWatchedBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class ChaptersWatchedBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "chapters_watched.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Chapters Watched"
    override val description: String? get() = "Read/watched chapter markers"
    override val icon get() = Icons.Default.CheckCircle

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val chapters = itemDao.getAllChaptersSync()
        chapters.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = chapters.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<ChapterWatched>>().restoreEachCatching(idOf = { it.name }) {
            itemDao.insertChapter(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllChaptersSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<ChapterWatched>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 4: `BookmarksBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class BookmarksBackupProcessor(
    private val bookmarkDao: BookmarkDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "bookmarks.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Bookmarks"
    override val description: String? get() = "Bookmarked chapters"
    override val icon get() = Icons.Default.Bookmark

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val bookmarks = bookmarkDao.getAllBookmarksSync()
        bookmarks.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = bookmarks.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<BookmarkedChapter>>().restoreEachCatching(idOf = { it.chapterName }) {
            bookmarkDao.insertBookmark(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = bookmarkDao.getAllBookmarksSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<BookmarkedChapter>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 5: `NotesBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class NotesBackupProcessor(
    private val notesDao: NotesDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "notes.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Notes"
    override val description: String? get() = "Per-item notes"
    override val icon get() = Icons.Default.EditNote

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val notes = notesDao.getAllNotesSync()
        notes.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = notes.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        bufferedSource.readUtf8().fromJson<List<NoteItem>>().restoreEachCatching(idOf = { it.itemTitle }) {
            notesDao.upsertNote(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = notesDao.getAllNotesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<NoteItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 6: `HeatMapBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class HeatMapBackupProcessor(
    private val heatMapDao: HeatMapDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "heat_map.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Activity Heat Map"
    override val description: String? get() = "Daily usage activity records"
    override val icon get() = Icons.Default.Whatshot

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val heatMaps = heatMapDao.getAllHeatMapsSync()
        heatMaps.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = heatMaps.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<HeatMapItem>>().restoreEachCatching(idOf = { it.time.toString() }) {
            heatMapDao.insertHeatMap(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = heatMapDao.getAllHeatMapsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HeatMapItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 7: `RecommendationsBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class RecommendationsBackupProcessor(
    private val recommendationDao: RecommendationDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "recommendations.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Recommendations"
    override val description: String? get() = "AI/recommendation cache"
    override val icon get() = Icons.Default.ThumbUp

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val recommendations = recommendationDao.getAllRecommendationsSync()
        recommendations.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = recommendations.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<Recommendation>>().restoreEachCatching(idOf = { it.title }) {
            recommendationDao.insertRecommendation(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = recommendationDao.getAllRecommendationsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<Recommendation>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 8: `NotificationsBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class NotificationsBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "notifications.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Saved Notifications"
    override val description: String? get() = "Notification inbox items"
    override val icon get() = Icons.Default.Notifications

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val notifications = itemDao.getAllNotifications()
        notifications.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = notifications.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<NotificationItem>>().restoreEachCatching(idOf = { it.notiTitle }) {
            itemDao.insertNotification(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllNotifications().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<NotificationItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 9: `DictionaryBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class DictionaryBackupProcessor(
    private val dictionaryDao: DictionaryDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String = "dictionary.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Dictionary"
    override val description: String? get() = "Dictionary Entries"
    override val icon get() = Icons.Default.MenuBook

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val entries = dictionaryDao.getAllSync()
        entries.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = entries.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<DictionaryEntry>>().restoreEachCatching(idOf = { it.term }) {
            dictionaryDao.insert(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = dictionaryDao.getAllSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<DictionaryEntry>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 10: `SourceOrderBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class SourceOrderBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "source_order.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Source Order"
    override val description: String? get() = "Custom source ordering"
    override val icon get() = Icons.Default.Reorder

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val order = itemDao.getSourceOrderSync()
        order.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = order.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<SourceOrder>>().restoreEachCatching(idOf = { it.name }) {
            itemDao.insertSourceOrder(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getSourceOrderSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<SourceOrder>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 11: `IncognitoBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class IncognitoBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "incognito_sources.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Incognito Sources"
    override val description: String? get() = "Sources marked incognito"
    override val icon get() = Icons.Default.VisibilityOff

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val sources = itemDao.getAllIncognitoSourcesSync()
        sources.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = sources.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<IncognitoSource>>().restoreEachCatching(idOf = { it.name }) {
            itemDao.insertIncognitoSource(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllIncognitoSourcesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<IncognitoSource>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 12: `ActivityBackupProcessor.kt` (single-blob, trivial wrap)**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.graphics.vector.ImageVector
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.ActivityTable
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource
import kotlin.time.Instant

class ActivityBackupProcessor(
    private val activityDao: ActivityDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "activity.json"

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val item = activityDao.getActivity()
        item?.toJson()?.let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = if (item != null) 1 else 0)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        json.fromJson<ActivityTable>().let { activityDao.upsertSynced(it.cumulativeSeconds, it.updatedAt) }
        return ProcessorResult(successCount = 1)
    }

    override val key: String
        get() = "activity"
    override val displayName: String
        get() = "Time Spent Doing"
    override val description: String
        get() = "Time spent doing things"
    override val icon: ImageVector
        get() = Icons.Default.AccessTime

    override suspend fun currentSummary(): BackupDataSummary {
        val item = activityDao.getActivity()
        return BackupDataSummary(
            itemCount = item?.cumulativeSeconds?.toInt(),
            lastModified = item?.updatedAt?.let {
                Instant.fromEpochSeconds(it)
            }
        )
    }

    override suspend fun parseSummary(
        json: String?,
        rawBytes: ByteArray?,
    ): BackupDataSummary {
        return BackupDataSummary(
            itemCount = json?.fromJson<ActivityTable>()?.cumulativeSeconds?.toInt(),
            sizeBytes = rawBytes?.size?.toLong()
        )
    }
}
```

- [ ] **Step 13: `NewSettingsBackupProcessor.kt` (single-blob, trivial wrap)**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.Settings
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class NewSettingsBackupProcessor(
    private val newSettingsHandling: NewSettingsHandling,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "settings"

    override val key: String get() = fileName
    override val displayName: String get() = "App Settings"
    override val description: String? get() = "Preferences and app configuration"
    override val icon get() = Icons.Default.Settings

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val settings = newSettingsHandling.preferences.data.firstOrNull()
        settings?.encode(sink)
        return ProcessorResult(successCount = if (settings != null) 1 else 0)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        newSettingsHandling.preferences.updateData { Settings.ADAPTER.decode(bufferedSource) }
        return ProcessorResult(successCount = 1)
    }

    override suspend fun currentSummary() = BackupDataSummary(
        details = listOf("Type" to "App settings"),
    )

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        sizeBytes = rawBytes?.size?.toLong(),
        details = listOf("Type" to "App settings"),
    )
}
```

- [ ] **Step 14: `BackupSettingsProcessor.kt` (single-blob, trivial wrap)**

Only `backup()`/`restore()` change; everything else in the file is unchanged:

```kotlin
    override suspend fun backup(sink: BufferedSink): ProcessorResult {
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
            .toJson()
            .let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = 1)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        val backupSettings = json.fromJson<BackupSettings>()
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
        return ProcessorResult(successCount = 1)
    }
```

Add `import com.programmersbox.sharedtools.ProcessorResult` to this file's imports.

- [ ] **Step 15: Run the full `kmpuiviews` test suite to confirm nothing else broke**

Run: `./gradlew :kmpuiviews:jvmTest`
Expected: PASS (including the `ZipperTest` from Task 1)

- [ ] **Step 16: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/FavoriteBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/HistoryBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ChaptersWatchedBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/BookmarksBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NotesBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/HeatMapBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/RecommendationsBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NotificationsBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/DictionaryBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/SourceOrderBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/IncognitoBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ActivityBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NewSettingsBackupProcessor.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/BackupSettingsProcessor.kt
git commit -m "feat: migrate all backup processors to per-row ProcessorResult contract"
```

---

### Task 3: `ListSubItemState` + `WizardItemState.subItems`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/WizardModels.kt`

**Interfaces:**
- Produces: `data class ListSubItemState(val id: String, val name: String, val coverUrl: String?, val itemCount: Int, val requiresBiometric: Boolean, val selected: Boolean = true)`
- Produces: `WizardItemState.subItems: List<ListSubItemState>? = null` (new field, defaults to null so every non-list category is unaffected)
- Consumes (later tasks): Task 9/10 populate it, Task 11 renders it

- [ ] **Step 1: Add `ListSubItemState` and the `subItems` field**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.compose.runtime.Stable
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo

sealed interface BackupWizardStep {
    data object SelectItems : BackupWizardStep
    data object Review : BackupWizardStep
    data object Executing : BackupWizardStep
    data object Complete : BackupWizardStep
}

sealed interface RestoreWizardStep {
    data object PickFile : RestoreWizardStep
    data object SelectItems : RestoreWizardStep
    data object Review : RestoreWizardStep
    data object Executing : RestoreWizardStep
    data object Complete : RestoreWizardStep
}

@Stable
data class ListSubItemState(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val itemCount: Int,
    val requiresBiometric: Boolean,
    val selected: Boolean = true,
)

@Stable
data class WizardItemState(
    val uiInfo: BackupUiInfo,
    val summary: BackupDataSummary? = null,
    val expanded: Boolean = false,
    val selected: Boolean = true,
    val subItems: List<ListSubItemState>? = null,
)
```

- [ ] **Step 2: Compile-check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: PASS (no existing code constructs `WizardItemState` positionally with all fields, so the new trailing default param is source-compatible)

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/WizardModels.kt
git commit -m "feat: add ListSubItemState model for per-list backup/restore selection"
```

---

### Task 4: `ListBackupProcessor` — per-row restore, `listIdFilter`, `parseLists`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ListBackupProcessor.kt`
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ListBackupProcessorTest.kt`

**Interfaces:**
- Consumes: `ProcessorResult`/`restoreEachCatching` (Task 1), `CustomList`/`CustomListItem`/`CustomListInfo`/`ListDao` (existing), `ListRepository` (existing)
- Produces: `ListBackupProcessor.listIdFilter: Set<String>? = null` (settable var, list uuids) — read by Task 5's `Zipper`
- Produces: `ListBackupProcessor.parseLists(json: String): List<CustomList>` (public) — used by Task 5's `Zipper.peekListContents`

- [ ] **Step 1: Write the failing test**

Create `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ListBackupProcessorTest.kt`:

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Buffer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class ThrowingOnNameListDao(
    private val delegate: ListDao,
    private val throwForName: String,
) : ListDao by delegate {
    override suspend fun createList(listItem: CustomListItem): Long {
        if (listItem.name == throwForName) throw RuntimeException("boom: ${listItem.name}")
        return delegate.createList(listItem)
    }
}

class ListBackupProcessorTest {

    private lateinit var dbFile: File
    private lateinit var database: ListDatabase

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("list-backup-processor-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    private fun customList(name: String) = CustomList(
        item = CustomListItem(uuid = name, name = name),
        list = listOf(
            CustomListInfo(
                uuid = name,
                title = "Title-$name",
                description = "Description",
                url = "https://example.com/$name",
                imageUrl = "https://example.com/$name.jpg",
                source = "ExampleService",
            )
        ),
    )

    @Test
    fun `restore skips a failing list and imports the rest`() = runTest {
        val throwingDao = ThrowingOnNameListDao(database.listDao(), throwForName = "bad-list")
        val repository = ListRepository(throwingDao, SystemAlerter(), FakeAuthManager())
        val processor = ListBackupProcessor(repository, database.listDao())

        val json = Json.encodeToString(listOf(customList("good-list"), customList("bad-list")))

        val result = processor.restore(json, Buffer())

        assertEquals(ProcessorResult(successCount = 1, failed = listOf("bad-list")), result)
        val stored = database.listDao().getAllListsSync()
        assertEquals(listOf("good-list"), stored.map { it.item.name })
    }

    @Test
    fun `backup only includes lists whose uuid is in listIdFilter`() = runTest {
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        repository.create("keep-me")
        repository.create("drop-me")
        val keepUuid = database.listDao().getAllListsSync().first { it.item.name == "keep-me" }.item.uuid

        val processor = ListBackupProcessor(repository, database.listDao())
        processor.listIdFilter = setOf(keepUuid)

        val sink = Buffer()
        val result = processor.backup(sink)

        assertEquals(1, result.successCount)
        assertTrue(sink.readUtf8().contains("keep-me"))
    }

    @Test
    fun `backup includes every list when listIdFilter is null`() = runTest {
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        repository.create("list-a")
        repository.create("list-b")

        val processor = ListBackupProcessor(repository, database.listDao())

        val result = processor.backup(Buffer())

        assertEquals(2, result.successCount)
    }

    @Test
    fun `parseLists deserializes a raw lists json blob`() = runTest {
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        val processor = ListBackupProcessor(repository, database.listDao())
        val json = Json.encodeToString(listOf(customList("some-list")))

        val parsed = processor.parseLists(json)

        assertEquals(listOf("some-list"), parsed.map { it.item.name })
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.backupproccesor.ListBackupProcessorTest"`
Expected: FAIL to compile — `listIdFilter` and `parseLists` don't exist yet, and `restore`/`backup` still return `Unit`.

- [ ] **Step 3: Update `ListBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class ListBackupProcessor(
    private val listRepository: ListRepository,
    private val listDao: ListDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "lists.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Custom Lists"
    override val description: String? get() = "User-created custom lists"
    override val icon get() = Icons.Default.FormatListBulleted

    /** When non-null, only lists whose [com.programmersbox.favoritesdatabase.CustomListItem.uuid] is in this set are backed up/restored. */
    var listIdFilter: Set<String>? = null

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val lists = listDao.getAllListsSync().let { all -> filterByListId(all) }
        lists.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = lists.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        val lists = filterByListId(json.fromJson<List<CustomList>>())
        return lists.restoreEachCatching(idOf = { it.item.name }) {
            listRepository.createList(it.item)
            it.list.forEach { listItem -> listRepository.addItem(listItem) }
        }
    }

    /** Parses a raw `lists.json` entry's contents, for previewing a zip's lists before restoring. */
    fun parseLists(json: String): List<CustomList> = json.fromJson()

    private fun filterByListId(lists: List<CustomList>): List<CustomList> =
        listIdFilter?.let { ids -> lists.filter { it.item.uuid in ids } } ?: lists

    override suspend fun currentSummary() = BackupDataSummary(itemCount = listDao.getAllListsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<CustomList>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.backupproccesor.ListBackupProcessorTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ListBackupProcessor.kt \
  kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/ListBackupProcessorTest.kt
git commit -m "feat: per-row list restore isolation and list-id filtering in ListBackupProcessor"
```

---

### Task 5: `Zipper` — thread `selectedListIds` + `peekListContents`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt`

**Interfaces:**
- Produces: `Zipper.zipFile(platformFile, selectedKeys, selectedListIds: Set<String>? = null, onItemComplete)` and `Zipper.readZip(platformFile, selectedKeys, selectedListIds: Set<String>? = null, onItemComplete)` — new optional param inserted before the trailing lambda, default `null` keeps every existing call site source-compatible
- Produces: `Zipper.peekListContents(platformFile: PlatformFile): List<CustomList>`
- Consumes: `ListBackupProcessor.listIdFilter`/`parseLists` (Task 4)

- [ ] **Step 1: Write the failing test**

Add to `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt` (new imports: `com.programmersbox.favoritesdatabase.CustomList`, `com.programmersbox.favoritesdatabase.CustomListInfo`, `com.programmersbox.favoritesdatabase.CustomListItem`, `com.programmersbox.kmpuiviews.utils.backupproccesor.ListBackupProcessor`, `com.programmersbox.kmpuiviews.repository.ListRepository`, `com.programmersbox.kmpuiviews.SystemAlerter`, `com.programmersbox.kmpuiviews.testing.FakeAuthManager`, `androidx.room3.Room`, `androidx.sqlite.driver.bundled.BundledSQLiteDriver`, `com.programmersbox.favoritesdatabase.ListDatabase`, `kotlinx.serialization.encodeToString`, `kotlinx.serialization.json.Json`):

```kotlin
    @Test
    fun `zipFile filters ListBackupProcessor by selectedListIds and resets the filter afterward`() = runBlocking {
        val dbFile = File.createTempFile("zipper-list-test", ".db").also { it.deleteOnExit() }
        val database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        repository.create("keep-me")
        repository.create("drop-me")
        val keepUuid = database.listDao().getAllListsSync().first { it.item.name == "keep-me" }.item.uuid
        val listProcessor = ListBackupProcessor(repository, database.listDao())
        val zipper = Zipper(listOf(listProcessor))
        val platformFile = PlatformFile(tempFile.absolutePath)

        zipper.zipFile(platformFile, setOf("lists.json"), setOf(keepUuid)) { }

        assertEquals(null, listProcessor.listIdFilter)
        val restored = zipper.peekListContents(platformFile)
        assertEquals(listOf("keep-me"), restored.map { it.item.name })

        database.close()
        dbFile.delete()
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.ZipperTest"`
Expected: FAIL to compile — `zipFile` doesn't accept a third positional `Set<String>?` yet, `peekListContents` doesn't exist.

- [ ] **Step 3: Update the commonMain `expect class Zipper`**

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile

expect class Zipper {
    suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult>

    suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult>

    suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary>

    suspend fun peekListContents(platformFile: PlatformFile): List<CustomList>
}
```

- [ ] **Step 4: Update Android `Zipper.kt`**

Add the `selectedListIds` param to `zipFile`/`readZip`, set/reset `ListBackupProcessor.listIdFilter` around each processor's call, and add `peekListContents`:

```kotlin
package com.programmersbox.kmpuiviews.utils

import android.content.Context
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.utils.backupproccesor.ListBackupProcessor
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

actual open class Zipper(
    private val context: Context,
    private val backupProcessors: List<BackupProcessor>,
    protected val exceptionDao: ExceptionDao,
) {

    init {
        val processors = backupProcessors.map { it.fileName }
        println("Backup processors: $processors")
    }

    actual suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>?,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        val pfd = context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "w")!!
        ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
            backupProcessors.filter { it.fileName in selectedKeys }.forEach { backup ->
                logFirebaseMessage("Zipping ${backup.fileName}")
                val duration = measureTime {
                    zip.putNextEntry(ZipEntry(backup.fileName))
                    if (backup is ListBackupProcessor) backup.listIdFilter = selectedListIds
                    val result = try {
                        runCatching {
                            measureTimedValue {
                                val sink = zip.sink().buffer()
                                val processorResult = backup.backup(sink)
                                sink.flush()
                                processorResult
                            }
                        }
                            .onFailure {
                                it.printStackTrace()
                                exceptionDao.insertException(it)
                            }
                            .fold(
                                onSuccess = { timedValue ->
                                    val processorResult = timedValue.value
                                    ItemResult(
                                        backup.fileName,
                                        timeTaken = timedValue.duration.toString(),
                                        success = processorResult.successCount > 0,
                                        error = processorResult.failed.takeIf { it.isNotEmpty() }
                                            ?.let { "${it.size} failed: ${it.joinToString()}" },
                                    )
                                },
                                onFailure = { e ->
                                    ItemResult(
                                        backup.fileName,
                                        timeTaken = e.message ?: "Unknown error",
                                        success = false,
                                        error = e.message
                                    )
                                },
                            )
                    } finally {
                        if (backup is ListBackupProcessor) backup.listIdFilter = null
                    }
                    results += result
                    onItemComplete(result)
                }
                logFirebaseMessage("Zipped ${backup.fileName} in $duration")
            }
        }
        results
    }

    actual suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>?,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val processor = backupProcessors.find { it.fileName == name }
                        if (name in selectedKeys && processor != null) {
                            if (processor is ListBackupProcessor) processor.listIdFilter = selectedListIds
                            val duration = measureTime {
                                val result = try {
                                    runCatching {
                                        measureTimedValue {
                                            val bytes = zipIs.readBytes()
                                            processor.restore(
                                                json = bytes.decodeToString(),
                                                bufferedSource = Buffer().apply { write(bytes) },
                                            )
                                        }
                                    }
                                        .fold(
                                            onSuccess = { timedValue ->
                                                val processorResult = timedValue.value
                                                ItemResult(
                                                    name,
                                                    timeTaken = timedValue.duration.toString(),
                                                    success = processorResult.successCount > 0,
                                                    error = processorResult.failed.takeIf { it.isNotEmpty() }
                                                        ?.let { "${it.size} failed: ${it.joinToString()}" },
                                                )
                                            },
                                            onFailure = { e ->
                                                ItemResult(
                                                    name,
                                                    timeTaken = e.message ?: "Unknown error",
                                                    success = false,
                                                    error = e.message
                                                )
                                            },
                                        )
                                } finally {
                                    if (processor is ListBackupProcessor) processor.listIdFilter = null
                                }
                                results += result
                                onItemComplete(result)
                            }
                            logFirebaseMessage("Unzipped $name in $duration")
                        }
                        entry = zipIs.nextEntry
                    }
                }
            }
        }
        results
    }

    actual suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary> = withContext(Dispatchers.IO) {
        val summaries = mutableMapOf<String, BackupDataSummary>()
        context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val uiInfo = uiInfos.find { it.key == name }
                        if (uiInfo != null) {
                            runCatching {
                                val bytes = zipIs.readBytes()
                                uiInfo.parseSummary(json = bytes.decodeToString(), rawBytes = bytes)
                            }
                                .onSuccess { summaries[name] = it }
                                .onFailure { it.printStackTrace(); exceptionDao.insertException(it) }
                        }
                        entry = zipIs.nextEntry
                    }
                }
            }
        }
        summaries
    }

    actual suspend fun peekListContents(platformFile: PlatformFile): List<CustomList> = withContext(Dispatchers.IO) {
        val processor = backupProcessors.filterIsInstance<ListBackupProcessor>().firstOrNull()
        var result: List<CustomList> = emptyList()
        if (processor != null) {
            context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "r")!!.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { inStream ->
                    ZipInputStream(inStream).use { zipIs ->
                        var entry: ZipEntry? = zipIs.nextEntry
                        while (entry != null) {
                            if (entry.name == processor.fileName) {
                                result = runCatching { processor.parseLists(zipIs.readBytes().decodeToString()) }
                                    .getOrDefault(emptyList())
                            }
                            entry = zipIs.nextEntry
                        }
                    }
                }
            }
        }
        result
    }
}
```

- [ ] **Step 5: Update JVM `Zipper.jvm.kt`**

Mirror Step 4's changes (no `context`/`ContentResolver`, no `exceptionDao`; use `FileInputStream(platformFile.absolutePath())` like the existing `peekZip`):

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.utils.backupproccesor.ListBackupProcessor
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

actual class Zipper(
    private val backupProcessors: List<BackupProcessor>,
) {
    init {
        val processors = backupProcessors.map { it.fileName }
        println("Backup processors: $processors")
    }

    actual suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>?,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        ZipOutputStream(FileOutputStream(platformFile.absolutePath())).use { zip ->
            backupProcessors.filter { it.fileName in selectedKeys }.forEach { processor ->
                println("Zipping ${processor.fileName}")
                val duration = measureTime {
                    zip.putNextEntry(ZipEntry(processor.fileName))
                    if (processor is ListBackupProcessor) processor.listIdFilter = selectedListIds
                    val result = try {
                        runCatching {
                            measureTimedValue {
                                val sink = zip.sink().buffer()
                                val processorResult = processor.backup(sink)
                                sink.flush()
                                processorResult
                            }
                        }
                            .fold(
                                onSuccess = { timedValue ->
                                    val processorResult = timedValue.value
                                    ItemResult(
                                        processor.fileName,
                                        timeTaken = timedValue.duration.toString(),
                                        success = processorResult.successCount > 0,
                                        error = processorResult.failed.takeIf { it.isNotEmpty() }
                                            ?.let { "${it.size} failed: ${it.joinToString()}" },
                                    )
                                },
                                onFailure = { e ->
                                    ItemResult(
                                        processor.fileName,
                                        timeTaken = e.message ?: "Unknown Error",
                                        success = false,
                                        error = e.message
                                    )
                                },
                            )
                    } finally {
                        if (processor is ListBackupProcessor) processor.listIdFilter = null
                    }
                    results += result
                    onItemComplete(result)
                }
                println("Zipped ${processor.fileName} in $duration")
            }
        }
        results
    }

    actual suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>?,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        FileInputStream(platformFile.absolutePath()).use { inStream ->
            ZipInputStream(inStream).use { zipIs ->
                var entry: ZipEntry? = zipIs.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val processor = backupProcessors.find { it.fileName == name }
                    if (name in selectedKeys && processor != null) {
                        if (processor is ListBackupProcessor) processor.listIdFilter = selectedListIds
                        val duration = measureTime {
                            val result = try {
                                runCatching {
                                    measureTimedValue {
                                        val bytes = zipIs.readBytes()
                                        processor.restore(
                                            json = bytes.decodeToString(),
                                            bufferedSource = Buffer().apply { write(bytes) },
                                        )
                                    }
                                }
                                    .fold(
                                        onSuccess = { timedValue ->
                                            val processorResult = timedValue.value
                                            ItemResult(
                                                name,
                                                timeTaken = timedValue.duration.toString(),
                                                success = processorResult.successCount > 0,
                                                error = processorResult.failed.takeIf { it.isNotEmpty() }
                                                    ?.let { "${it.size} failed: ${it.joinToString()}" },
                                            )
                                        },
                                        onFailure = { e ->
                                            ItemResult(
                                                name,
                                                timeTaken = e.message ?: "Unknown Error",
                                                success = false,
                                                error = e.message
                                            )
                                        },
                                    )
                            } finally {
                                if (processor is ListBackupProcessor) processor.listIdFilter = null
                            }
                            results += result
                            onItemComplete(result)
                        }
                        println("Unzipped $name in $duration")
                    }
                    entry = zipIs.nextEntry
                }
            }
        }
        results
    }

    actual suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary> = withContext(Dispatchers.IO) {
        val summaries = mutableMapOf<String, BackupDataSummary>()
        FileInputStream(platformFile.absolutePath()).use { inStream ->
            ZipInputStream(inStream).use { zipIs ->
                var entry: ZipEntry? = zipIs.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val uiInfo = uiInfos.find { it.key == name }
                    if (uiInfo != null) {
                        runCatching {
                            val bytes = zipIs.readBytes()
                            uiInfo.parseSummary(json = bytes.decodeToString(), rawBytes = bytes)
                        }
                            .onSuccess { summaries[name] = it }
                    }
                    entry = zipIs.nextEntry
                }
            }
        }
        summaries
    }

    actual suspend fun peekListContents(platformFile: PlatformFile): List<CustomList> = withContext(Dispatchers.IO) {
        val processor = backupProcessors.filterIsInstance<ListBackupProcessor>().firstOrNull()
        var result: List<CustomList> = emptyList()
        if (processor != null) {
            FileInputStream(platformFile.absolutePath()).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        if (entry.name == processor.fileName) {
                            result = runCatching { processor.parseLists(zipIs.readBytes().decodeToString()) }
                                .getOrDefault(emptyList())
                        }
                        entry = zipIs.nextEntry
                    }
                }
            }
        }
        result
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.ZipperTest"`
Expected: PASS

- [ ] **Step 7: Update existing call sites that used positional args ending in the lambda**

`ZipperTest.kt`'s earlier tests (`zipper.zipFile(platformFile, setOf("a.json")) { }`) still compile unchanged because `selectedListIds` now sits between `selectedKeys` and the trailing lambda with a default of `null` — Kotlin resolves the trailing lambda to the last parameter regardless. No edits needed there.

- [ ] **Step 8: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt \
  kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt \
  kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt \
  kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt
git commit -m "feat: thread selectedListIds through Zipper and add peekListContents"
```

---

### Task 6: `Backup.kt` — thread `selectedListIds` + `peekListContents` wrapper

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Backup.kt`
- Verify only (no edit expected): `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/BackupTest.kt`

**Interfaces:**
- Produces: `Backup.createBackup(document, selectedKeys, selectedListIds: Set<String>? = null, onItemComplete)`, `Backup.restoreBackup(document, selectedKeys, selectedListIds: Set<String>? = null, onItemComplete)`, `Backup.peekListContents(document): List<CustomList>`
- Consumes: `Zipper.zipFile`/`readZip`/`peekListContents` (Task 5)

- [ ] **Step 1: Update `Backup.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable
import kotlin.time.measureTime

class Backup(
    private val exceptionDao: ExceptionDao,
    private val zipper: Zipper,
) {
    suspend fun createBackup(
        document: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> {
        var output: List<ItemResult> = emptyList()
        val time = measureTime {
            output = runCatching { zipper.zipFile(document, selectedKeys, selectedListIds, onItemComplete) }
                .logFailureToDatabase()
                .getOrThrow()
        }
        println("Took $time to zip file")
        return output
    }

    suspend fun restoreBackup(
        document: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> =
        runCatching { zipper.readZip(document, selectedKeys, selectedListIds, onItemComplete) }
            .logFailureToDatabase()
            .getOrThrow()

    suspend fun peekBackup(document: PlatformFile, uiInfos: List<BackupUiInfo>): Map<String, BackupDataSummary> =
        runCatching { zipper.peekZip(document, uiInfos) }
            .logFailureToDatabase()
            .getOrThrow()

    suspend fun peekListContents(document: PlatformFile): List<CustomList> =
        runCatching { zipper.peekListContents(document) }
            .logFailureToDatabase()
            .getOrElse { emptyList() }

    private suspend fun <T> Result<T>.logFailureToDatabase() = onFailure {
        it.printStackTrace()
        exceptionDao.insertException(it)
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
```

- [ ] **Step 2: Run `BackupTest.kt` to confirm the existing call site still compiles and passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.BackupTest"`
Expected: PASS — `backup.createBackup(badFile, setOf("a.json")) { }` still resolves via the new default `selectedListIds = null`.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Backup.kt
git commit -m "feat: thread selectedListIds and add peekListContents through Backup"
```

---

### Task 7: `BackgroundWorkHandler` + Android worker plumbing

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt` (`FakeBackgroundWorkHandler`)

**Interfaces:**
- Produces: `BackgroundWorkHandler.startBackup(file, selectedKeys, selectedListIds: Set<String>? = null)`, `BackgroundWorkHandler.startRestore(file, selectedKeys, selectedListIds: Set<String>? = null)`
- Consumes: `Backup.createBackup`/`restoreBackup` (Task 6)

- [ ] **Step 1: Update the `BackgroundWorkHandler` interface**

In `WorkRepository.kt`, change the two method signatures:

```kotlin
interface BackgroundWorkHandler {
    fun localToCloudListener(): Flow<List<WorkInfoKmp>>
    fun cloudToLocalListener(): Flow<List<WorkInfoKmp>>
    fun syncLocalToCloud()
    fun syncCloudToLocal()
    fun setupPeriodicCheckers()
    fun workerInfoFlow(): Flow<List<WorkerInfoModel>>
    fun sourceUpdate()
    fun cancel(uuid: String)
    fun startBackup(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>? = null)
    fun startRestore(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>? = null)
    fun backupResultsFlow(): Flow<List<ItemResult>>
    fun restoreResultsFlow(): Flow<List<ItemResult>>
}
```

- [ ] **Step 2: Update Android `BackgroundWorkHandlerImpl.startBackup`/`startRestore`**

Use a boolean marker plus a `StringArray` since `workDataOf` needs a concrete (non-null) value per key:

```kotlin
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {
        workManager.enqueueUniqueWork(
            "backup",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setInputData(
                    workDataOf(
                        "uri" to file.toAndroidUri("").toString(),
                        "selectedKeys" to selectedKeys.toTypedArray(),
                        "hasListFilter" to (selectedListIds != null),
                        "selectedListIds" to (selectedListIds ?: emptySet()).toTypedArray(),
                    )
                )
                .build()
        )
    }

    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {
        workManager.enqueueUniqueWork(
            "restore",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RestoreWorker>()
                .setInputData(
                    workDataOf(
                        "uri" to file.toAndroidUri("").toString(),
                        "selectedKeys" to selectedKeys.toTypedArray(),
                        "hasListFilter" to (selectedListIds != null),
                        "selectedListIds" to (selectedListIds ?: emptySet()).toTypedArray(),
                    )
                )
                .build()
        )
    }
```

- [ ] **Step 3: Update `BackupWorker.kt` to read the filter and pass it through**

```kotlin
    override suspend fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        val selectedKeys = inputData.getStringArray("selectedKeys")?.toSet() ?: return Result.failure()
        val selectedListIds = if (inputData.getBoolean("hasListFilter", false)) {
            inputData.getStringArray("selectedListIds")?.toSet().orEmpty()
        } else {
            null
        }
        setForeground(getForegroundInfo())
        val results = mutableListOf<ItemResult>()
        return runCatching {
            backup.createBackup(readPlatformFile(uri), selectedKeys, selectedListIds) { result ->
                results += result
                setProgress(workDataOf("results" to Json.encodeToString(results.toList())))
            }
        }.fold(
            onSuccess = { finalResults ->
                postCompletionNotification("Backup complete", timeoutAfter = 3000L)
                Result.success(workDataOf("results" to Json.encodeToString(finalResults)))
            },
            onFailure = { e ->
                recordFirebaseException(e)
                postCompletionNotification("Backup failed", timeoutAfter = null)
                Result.failure(workDataOf("results" to Json.encodeToString(results.toList())))
            }
        )
    }
```

- [ ] **Step 4: Apply the same read + pass-through to `RestoreWorker.kt`**

```kotlin
    override suspend fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        val selectedKeys = inputData.getStringArray("selectedKeys")?.toSet() ?: return Result.failure()
        val selectedListIds = if (inputData.getBoolean("hasListFilter", false)) {
            inputData.getStringArray("selectedListIds")?.toSet().orEmpty()
        } else {
            null
        }
        setForeground(getForegroundInfo())
        val results = mutableListOf<ItemResult>()
        return runCatching {
            backup.restoreBackup(readPlatformFile(uri), selectedKeys, selectedListIds) { result ->
                results += result
                setProgress(workDataOf("results" to Json.encodeToString(results.toList())))
            }
        }.fold(
            onSuccess = { finalResults ->
                postCompletionNotification("Restore complete", timeoutAfter = 3000L)
                Result.success(workDataOf("results" to Json.encodeToString(finalResults)))
            },
            onFailure = { e ->
                recordFirebaseException(e)
                postCompletionNotification("Restore failed", timeoutAfter = null)
                Result.failure(workDataOf("results" to Json.encodeToString(results.toList())))
            }
        )
    }
```

- [ ] **Step 5: Update `FakeBackgroundWorkHandler` in `Fakes.kt`**

```kotlin
class FakeBackgroundWorkHandler : BackgroundWorkHandler {
    override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
    override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
    override fun syncLocalToCloud() {}
    override fun syncCloudToLocal() {}
    override fun setupPeriodicCheckers() {}
    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())
    override fun sourceUpdate() {}
    override fun cancel(uuid: String) {}
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {}
    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {}
    override fun backupResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
    override fun restoreResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
}
```

- [ ] **Step 6: Compile-check**

Run: `./gradlew :kmpuiviews:compileDebugKotlinAndroid :kmpuiviews:jvmTestClasses`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt \
  kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt \
  kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt \
  kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt \
  kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt
git commit -m "feat: thread selectedListIds through BackgroundWorkHandler and Android workers"
```

---

### Task 8: JVM/Desktop worker plumbing

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`

**Interfaces:**
- Consumes: `BackgroundWorkHandler` (Task 7), `Backup.createBackup`/`restoreBackup` (Task 6)

- [ ] **Step 1: Update `BackupRestoreData`, `startBackup`/`startRestore`, and the two `DesktopTask` workers**

```kotlin
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {
        scope.launch {
            TestTaskRunner.runTask(
                BackupWorker(),
                BackupId,
                inputData = TaskData.of(BackupRestoreData(file, selectedKeys, selectedListIds))
            )
        }
    }

    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>, selectedListIds: Set<String>?) {
        scope.launch {
            TestTaskRunner.runTask(
                RestoreWorker(),
                RestoreId,
                inputData = TaskData.of(BackupRestoreData(file, selectedKeys, selectedListIds))
            )
        }
    }
```

```kotlin
class BackupWorker : DesktopTask, KoinComponent {
    private val backup: Backup by inject()
    private val resultsHolder: BackupResultsHolder by inject()

    override suspend fun doWork(context: TaskContext): TaskResult {
        val duration = measureTimedValue {
            runCatching {
                val data = context.inputData<BackupRestoreData>() ?: return@runCatching
                val results = backup.createBackup(data.file, data.selectedKeys, data.selectedListIds) { }
                resultsHolder.backupResults.value = results
            }
                .fold(
                    onSuccess = { TaskResult.Success },
                    onFailure = { TaskResult.Failure(it.message.orEmpty()) }
                )
        }

        println("Took ${duration.duration} to backup")
        return duration.value
    }
}

class RestoreWorker : DesktopTask, KoinComponent {
    private val backup: Backup by inject()
    private val resultsHolder: BackupResultsHolder by inject()

    override suspend fun doWork(context: TaskContext): TaskResult {
        val duration = measureTimedValue {
            runCatching {
                val data = context.inputData<BackupRestoreData>() ?: return@runCatching
                val results = backup.restoreBackup(data.file, data.selectedKeys, data.selectedListIds) { }
                resultsHolder.restoreResults.value = results
            }
                .fold(
                    onSuccess = { TaskResult.Success },
                    onFailure = { TaskResult.Failure(it.message.orEmpty()) }
                )
        }

        println("Took ${duration.duration} to backup")
        return duration.value
    }
}
```

```kotlin
@Serializable
data class BackupRestoreData(
    val file: PlatformFile,
    val selectedKeys: Set<String>,
    val selectedListIds: Set<String>? = null,
)
```

- [ ] **Step 2: Compile-check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt
git commit -m "feat: thread selectedListIds through JVM/Desktop backup and restore workers"
```

---

### Task 9: `BackupWizardViewModel` — list sub-selection

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/BackupWizardViewModel.kt`
- Modify: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/backup/BackupWizardViewModelTest.kt`

**Interfaces:**
- Consumes: `ListSubItemState`/`WizardItemState.subItems` (Task 3), `ListDao`/`CustomList` (existing)
- Produces: `BackupWizardViewModel` constructor gains `listDao: ListDao? = null`; `toggleListSelected(listId: String)`; `startBackup: (F, Set<String>, Set<String>?) -> Unit` (was 2-arg)

`listDao` is nullable with a default so the "Lists" row is the only one that ever needs it — every other `WizardItemState` never touches it. (`ListDao` lives in `favoritesdatabase`, already a transitive dependency of `kmpuiviews`.)

- [ ] **Step 1: Write the failing tests**

Replace `BackupWizardViewModelTest.kt`'s `startBackup` lambdas (now 3-arg) and add two new tests. Full updated file:

```kotlin
package com.programmersbox.kmpuiviews.backup

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardStep
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardViewModel
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeUiInfo(override val key: String) : BackupUiInfo {
    override val displayName = key
    override val description: String? = null
    override val icon = null
    override suspend fun currentSummary() = BackupDataSummary(itemCount = 1)
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary()
}

private fun fakeListDao(lists: List<CustomList>): ListDao = object : ListDao by UnimplementedListDao {
    override suspend fun getAllListsSync(): List<CustomList> = lists
}

/** Every method is unimplemented — these tests only ever call `getAllListsSync`, overridden per-fake above via delegation. */
private object UnimplementedListDao : ListDao {
    private fun unsupported(): Nothing = throw NotImplementedError("not used in these tests")
    override fun getAllLists() = throw NotImplementedError()
    override fun getAllListsCount() = throw NotImplementedError()
    override fun getAllListItemsCount() = throw NotImplementedError()
    override suspend fun getAllListsSync(): List<CustomList> = unsupported()
    override suspend fun getCustomListItem(uuid: String) = unsupported()
    override fun getCustomListItemFlow(uuid: String) = throw NotImplementedError()
    override suspend fun createList(listItem: CustomListItem) = unsupported()
    override suspend fun addItem(listItem: CustomListInfo) = unsupported()
    override suspend fun removeItem(listItem: CustomListInfo) = unsupported()
    override suspend fun removeItem(uuid: String) = unsupported()
    override suspend fun updateList(listItem: CustomListItem) = unsupported()
    override suspend fun removeList(item: CustomListItem) = unsupported()
    override suspend fun updateBiometric(uuid: String, useBiometric: Boolean) = unsupported()
    override suspend fun getDirtyCustomListItems() = unsupported()
    override fun observeDirtyCustomListItemCount() = throw NotImplementedError()
    override suspend fun getDirtyCustomListInfo() = unsupported()
    override fun observeDirtyCustomListInfoCount() = throw NotImplementedError()
    override suspend fun getCustomListItemByUuid(uuid: String) = unsupported()
    override suspend fun getCustomListInfoByUniqueId(uniqueId: String) = unsupported()
    override suspend fun updateCustomListItem(item: CustomListItem) = unsupported()
    override suspend fun updateCustomListInfo(info: CustomListInfo) = unsupported()
    override suspend fun softDeleteCustomListItem(uuid: String, timestamp: Long) = unsupported()
    override suspend fun softDeleteCustomListInfo(uniqueId: String, timestamp: Long) = unsupported()
    override suspend fun markCustomListItemSynced(uuid: String, timestamp: Long) = unsupported()
    override suspend fun markCustomListInfoSynced(uniqueId: String, timestamp: Long) = unsupported()
    override suspend fun getAllCustomListItemsSync() = unsupported()
    override suspend fun resetAllCustomListItemsIsDeleted() = unsupported()
    override suspend fun deleteAllDeletedCustomListItems() = unsupported()
    override suspend fun getAllCustomListInfoSync() = unsupported()
    override suspend fun resetAllCustomListInfoIsDeleted() = unsupported()
    override suspend fun deleteAllDeletedCustomListInfo() = unsupported()
}

private fun customList(name: String) = CustomList(
    item = CustomListItem(uuid = name, name = name),
    list = listOf(CustomListInfo(uuid = name, title = "T", description = "D", url = "https://example.com/$name", imageUrl = "https://example.com/$name.jpg", source = "Src")),
)

class BackupWizardViewModelTest {

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) { while (!condition()) delay(10) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on SelectItems with all items selected`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _, _ -> })
        val state = vm.state.value
        assertEquals(BackupWizardStep.SelectItems, state.step)
        assertTrue(state.items.all { it.selected })
    }

    @Test
    fun `deselectAll clears selection, selectAll restores it`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _, _ -> })
        vm.deselectAll()
        assertTrue(vm.state.value.items.none { it.selected })
        vm.selectAll()
        assertTrue(vm.state.value.items.all { it.selected })
    }

    @Test
    fun `toggleSelected flips a single item`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _, _ -> })
        vm.toggleSelected("a")
        assertEquals(false, vm.state.value.items.first { it.uiInfo.key == "a" }.selected)
        assertEquals(true, vm.state.value.items.first { it.uiInfo.key == "b" }.selected)
    }

    @Test
    fun `goToReview only carries selected items, confirm calls startBackup with the file, keys, and null list filter`() = runTest {
        var startedWith: Triple<String, Set<String>, Set<String>?>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys, listIds -> startedWith = Triple(file, keys, listIds) },
        )
        vm.toggleSelected("b")
        vm.goToReview()
        assertEquals(BackupWizardStep.Review, vm.state.value.step)
        assertEquals(listOf("a"), vm.state.value.items.map { it.uiInfo.key })

        vm.confirm("file.zip")
        assertEquals(Triple("file.zip", setOf("a"), null), startedWith)
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = results,
            startBackup = { _, _, _ -> },
        )
        vm.goToReview()
        vm.confirm("file.zip")
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", timeTaken = "100ms", success = true))
        awaitCondition { vm.state.value.results.size == 1 }
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", timeTaken = "100ms", success = true), ItemResult("b", timeTaken = "100ms", success = true))
        awaitCondition { vm.state.value.step == BackupWizardStep.Complete }
        assertEquals(BackupWizardStep.Complete, vm.state.value.step)
        assertEquals(2, vm.state.value.results.size)
    }

    @Test
    fun `lists.json row loads subItems from listDao`() = runTest {
        val lists = listOf(customList("list-a"), customList("list-b"))
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("lists.json")),
            listDao = fakeListDao(lists),
            startBackup = { _, _, _ -> },
        )
        awaitCondition { vm.state.value.items.single().subItems != null }
        val subItems = vm.state.value.items.single().subItems!!
        assertEquals(setOf("list-a", "list-b"), subItems.map { it.name }.toSet())
        assertTrue(subItems.all { it.selected })
    }

    @Test
    fun `toggleListSelected flips one sub-item, confirm sends only the selected list ids`() = runTest {
        val lists = listOf(customList("list-a"), customList("list-b"))
        var startedWith: Triple<String, Set<String>, Set<String>?>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("lists.json")),
            listDao = fakeListDao(lists),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys, listIds -> startedWith = Triple(file, keys, listIds) },
        )
        awaitCondition { vm.state.value.items.single().subItems != null }

        vm.toggleListSelected("list-b")
        assertEquals(
            setOf("list-a"),
            vm.state.value.items.single().subItems!!.filter { it.selected }.map { it.id }.toSet(),
        )

        vm.goToReview()
        vm.confirm("file.zip")
        assertEquals("file.zip", startedWith?.first)
        assertEquals(setOf("list-a"), startedWith?.third)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.backup.BackupWizardViewModelTest"`
Expected: FAIL to compile — `listDao` param, `toggleListSelected`, and the 3-arg `startBackup` don't exist yet.

- [ ] **Step 3: Update `BackupWizardViewModel.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupWizardUiState(
    val step: BackupWizardStep = BackupWizardStep.SelectItems,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

private const val LISTS_KEY = "lists.json"

class BackupWizardViewModel<F>(
    uiInfos: List<BackupUiInfo>,
    private val listDao: ListDao? = null,
    private val resultsFlow: Flow<List<ItemResult>> = emptyFlow(),
    private val startBackup: (F, Set<String>, Set<String>?) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BackupWizardUiState(items = uiInfos.map { WizardItemState(uiInfo = it) })
    )
    val state: StateFlow<BackupWizardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            uiInfos.forEach { loadSummaryIfNeeded(it.key) }
        }
    }

    fun toggleSelected(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(selected = !it.selected) else it })
        }
    }

    fun toggleExpanded(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(expanded = !it.expanded) else it })
        }
        loadSummaryIfNeeded(key)
    }

    fun toggleListSelected(listId: String) {
        _state.update { s ->
            s.copy(items = s.items.map { item ->
                if (item.uiInfo.key != LISTS_KEY) item
                else item.copy(subItems = item.subItems?.map { if (it.id == listId) it.copy(selected = !it.selected) else it })
            })
        }
    }

    fun selectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = true) }) }
    }

    fun deselectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = false) }) }
    }

    fun goToReview() {
        _state.update { s ->
            s.copy(step = BackupWizardStep.Review, items = s.items.filter { it.selected })
        }
        _state.value.items.forEach { if (it.summary == null) loadSummaryIfNeeded(it.uiInfo.key) }
    }

    fun goToSelectItems() {
        _state.update { s ->
            s.copy(step = BackupWizardStep.SelectItems)
        }
    }

    fun confirm(file: F) {
        val keys = _state.value.items.map { it.uiInfo.key }.toSet()
        val selectedListIds = _state.value.items
            .find { it.uiInfo.key == LISTS_KEY }
            ?.subItems
            ?.filter { it.selected }
            ?.map { it.id }
            ?.toSet()
        _state.update { it.copy(step = BackupWizardStep.Executing) }
        startBackup(file, keys, selectedListIds)
        viewModelScope.launch {
            resultsFlow.collect { results ->
                _state.update { it.copy(results = results) }
                if (results.map { r -> r.key }.toSet() == keys) {
                    _state.update { it.copy(step = BackupWizardStep.Complete) }
                }
            }
        }
    }

    private fun loadSummaryIfNeeded(key: String) {
        val current = _state.value.items.find { it.uiInfo.key == key } ?: return
        if (current.summary != null) return
        viewModelScope.launch {
            val summary = current.uiInfo.currentSummary()
            val subItems = if (key == LISTS_KEY && listDao != null) {
                listDao.getAllListsSync().map {
                    ListSubItemState(
                        id = it.item.uuid,
                        name = it.item.name,
                        coverUrl = it.list.firstOrNull()?.imageUrl,
                        itemCount = it.list.size,
                        requiresBiometric = it.item.useBiometric,
                    )
                }
            } else {
                null
            }
            _state.update { s ->
                s.copy(items = s.items.map {
                    if (it.uiInfo.key == key) it.copy(summary = summary, subItems = subItems) else it
                })
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.backup.BackupWizardViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/BackupWizardViewModel.kt \
  kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/backup/BackupWizardViewModelTest.kt
git commit -m "feat: per-list selection in BackupWizardViewModel"
```

---

### Task 10: `RestoreWizardViewModel` — list sub-selection from the zip's contents

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/RestoreWizardViewModel.kt`
- Modify: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/backup/RestoreWizardViewModelTest.kt`

**Interfaces:**
- Consumes: `ListSubItemState`/`WizardItemState.subItems` (Task 3), `CustomList` (existing)
- Produces: `RestoreWizardViewModel` constructor gains `peekListContents: suspend (F) -> List<CustomList> = { emptyList() }`; `toggleListSelected(listId: String)`; `startRestore: (F, Set<String>, Set<String>?) -> Unit` (was 2-arg)

- [ ] **Step 1: Write the failing tests**

Replace `RestoreWizardViewModelTest.kt` in full:

```kotlin
package com.programmersbox.kmpuiviews.backup

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.RestoreWizardStep
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.RestoreWizardViewModel
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class RestoreFakeUiInfo(override val key: String) : BackupUiInfo {
    override val displayName = key
    override val description: String? = null
    override val icon = null
    override suspend fun currentSummary() = BackupDataSummary()
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(itemCount = 5)
}

private fun customList(name: String) = CustomList(
    item = CustomListItem(uuid = name, name = name),
    list = listOf(CustomListInfo(uuid = name, title = "T", description = "D", url = "https://example.com/$name", imageUrl = "https://example.com/$name.jpg", source = "Src")),
)

class RestoreWizardViewModelTest {

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) { while (!condition()) delay(10) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pickFile runs the peek pass and moves to SelectItems with only matched entries`() = runTest {
        val a = RestoreFakeUiInfo("a")
        val b = RestoreFakeUiInfo("b")
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(a, b),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 5)) },
            startRestore = { _, _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }

        val state = vm.state.value
        assertEquals(RestoreWizardStep.SelectItems, state.step)
        assertEquals(listOf("a"), state.items.map { it.uiInfo.key })
        assertEquals(5, state.items.single().summary?.itemCount)
    }

    @Test
    fun `confirm calls startRestore with the picked file, selected keys, and null list filter`() = runTest {
        var called: Triple<String, Set<String>, Set<String>?>? = null
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys, listIds -> called = Triple(file, keys, listIds) },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }
        vm.goToReview()
        vm.confirm()

        assertEquals(Triple("file.zip", setOf("a"), null), called)
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = results,
            startRestore = { _, _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.step == RestoreWizardStep.SelectItems }
        vm.goToReview()
        vm.confirm()
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", timeTaken = "100ms", success = true))
        awaitCondition { vm.state.value.step == RestoreWizardStep.Complete }
        assertEquals(RestoreWizardStep.Complete, vm.state.value.step)
        assertEquals(1, vm.state.value.results.size)
    }

    @Test
    fun `pickFile loads subItems for the lists row from peekListContents, not the local db`() = runTest {
        val listUiInfo = RestoreFakeUiInfo("lists.json")
        val zipLists = listOf(customList("zip-list-a"), customList("zip-list-b"))
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(listUiInfo),
            peekZip = { mapOf("lists.json" to BackupDataSummary(itemCount = 2)) },
            peekListContents = { zipLists },
            startRestore = { _, _, _ -> },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.items.singleOrNull()?.subItems != null }

        val subItems = vm.state.value.items.single().subItems!!
        assertEquals(setOf("zip-list-a", "zip-list-b"), subItems.map { it.name }.toSet())
    }

    @Test
    fun `toggleListSelected flips one sub-item, confirm sends only the selected list ids`() = runTest {
        val listUiInfo = RestoreFakeUiInfo("lists.json")
        val zipLists = listOf(customList("zip-list-a"), customList("zip-list-b"))
        var called: Triple<String, Set<String>, Set<String>?>? = null
        val vm = RestoreWizardViewModel<String>(
            uiInfos = listOf(listUiInfo),
            peekZip = { mapOf("lists.json" to BackupDataSummary(itemCount = 2)) },
            peekListContents = { zipLists },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys, listIds -> called = Triple(file, keys, listIds) },
        )

        vm.pickFile("file.zip")
        awaitCondition { vm.state.value.items.singleOrNull()?.subItems != null }

        vm.toggleListSelected("zip-list-b")
        vm.goToReview()
        vm.confirm()

        assertEquals(setOf("zip-list-a"), called?.third)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.backup.RestoreWizardViewModelTest"`
Expected: FAIL to compile — `peekListContents` param, `toggleListSelected`, and the 3-arg `startRestore` don't exist yet.

- [ ] **Step 3: Update `RestoreWizardViewModel.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestoreWizardUiState<F>(
    val step: RestoreWizardStep = RestoreWizardStep.PickFile,
    val file: F? = null,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

private const val LISTS_KEY = "lists.json"

class RestoreWizardViewModel<F>(
    private val uiInfos: List<BackupUiInfo>,
    private val peekZip: suspend (F) -> Map<String, BackupDataSummary>,
    private val peekListContents: suspend (F) -> List<CustomList> = { emptyList() },
    private val resultsFlow: Flow<List<ItemResult>> = emptyFlow(),
    private val startRestore: (F, Set<String>, Set<String>?) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreWizardUiState<F>())
    val state: StateFlow<RestoreWizardUiState<F>> = _state.asStateFlow()

    fun pickFile(file: F) {
        viewModelScope.launch {
            val summaries = peekZip(file)
            val items = uiInfos
                .filter { summaries.containsKey(it.key) }
                .map { WizardItemState(uiInfo = it, summary = summaries[it.key]) }
            _state.update { it.copy(file = file, step = RestoreWizardStep.SelectItems, items = items) }

            if (items.any { it.uiInfo.key == LISTS_KEY }) {
                val subItems = peekListContents(file).map {
                    ListSubItemState(
                        id = it.item.uuid,
                        name = it.item.name,
                        coverUrl = it.list.firstOrNull()?.imageUrl,
                        itemCount = it.list.size,
                        requiresBiometric = it.item.useBiometric,
                    )
                }
                _state.update { s ->
                    s.copy(items = s.items.map { item -> if (item.uiInfo.key == LISTS_KEY) item.copy(subItems = subItems) else item })
                }
            }
        }
    }

    fun toggleSelected(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(selected = !it.selected) else it })
        }
    }

    fun toggleExpanded(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(expanded = !it.expanded) else it })
        }
    }

    fun toggleListSelected(listId: String) {
        _state.update { s ->
            s.copy(items = s.items.map { item ->
                if (item.uiInfo.key != LISTS_KEY) item
                else item.copy(subItems = item.subItems?.map { if (it.id == listId) it.copy(selected = !it.selected) else it })
            })
        }
    }

    fun selectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = true) }) }
    }

    fun deselectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = false) }) }
    }

    fun goToReview() {
        _state.update { s ->
            s.copy(step = RestoreWizardStep.Review, items = s.items.filter { it.selected })
        }
    }

    fun goToSelectItems() {
        _state.update { s ->
            s.copy(step = RestoreWizardStep.SelectItems)
        }
    }

    fun goToChooseFile() {
        _state.update { s ->
            s.copy(step = RestoreWizardStep.PickFile)
        }
    }

    fun confirm() {
        val file = _state.value.file ?: return
        val keys = _state.value.items.map { it.uiInfo.key }.toSet()
        val selectedListIds = _state.value.items
            .find { it.uiInfo.key == LISTS_KEY }
            ?.subItems
            ?.filter { it.selected }
            ?.map { it.id }
            ?.toSet()
        _state.update { it.copy(step = RestoreWizardStep.Executing) }
        startRestore(file, keys, selectedListIds)
        viewModelScope.launch {
            resultsFlow.collect { results ->
                _state.update { it.copy(results = results) }
                if (results.map { r -> r.key }.toSet() == keys) {
                    _state.update { it.copy(step = RestoreWizardStep.Complete) }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.backup.RestoreWizardViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/RestoreWizardViewModel.kt \
  kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/backup/RestoreWizardViewModelTest.kt
git commit -m "feat: per-list selection in RestoreWizardViewModel from zip contents"
```

---

### Task 11: `WizardItemRow` — render the list checklist

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/WizardItemRow.kt`

**Interfaces:**
- Consumes: `ListSubItemState`/`WizardItemState.subItems` (Task 3)
- Produces: `WizardItemRow(item, onToggleSelected, onToggleExpanded, onToggleListSelected: (String) -> Unit = {}, modifier)` — new optional callback, default no-op keeps every other call site source-compatible until Task 12 wires the real ones

- [ ] **Step 1: Update `WizardItemRow.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice

@Composable
fun WizardItemRow(
    item: WizardItemState,
    onToggleSelected: () -> Unit,
    onToggleExpanded: () -> Unit,
    onToggleListSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        ElevatedCard(
            onClick = onToggleExpanded,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = item.selected, onCheckedChange = { onToggleSelected() })
                item.uiInfo.icon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.uiInfo.displayName, style = MaterialTheme.typography.bodyLarge)
                    item.summary?.itemCount?.let {
                        Text("$it items", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(if (item.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
            }
            AnimatedVisibility(visible = item.expanded) {
                Column(modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp)) {
                    val subItems = item.subItems
                    if (subItems != null) {
                        subItems.forEach { subItem ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = subItem.selected,
                                    onCheckedChange = { onToggleListSelected(subItem.id) },
                                )
                                ImageLoaderChoice(
                                    imageUrl = subItem.coverUrl.orEmpty(),
                                    name = subItem.name,
                                    placeHolder = { painterLogo() },
                                    modifier = Modifier.size(40.dp).padding(end = 8.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subItem.name, style = MaterialTheme.typography.bodyMedium)
                                    Text("${subItem.itemCount} items", style = MaterialTheme.typography.bodySmall)
                                }
                                if (subItem.requiresBiometric) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Requires biometric unlock",
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        item.uiInfo.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        item.summary?.let { summary ->
                            summary.itemCount?.let { Text("Records: $it", style = MaterialTheme.typography.bodySmall) }
                            summary.sizeBytes?.let { Text("Size: $it bytes", style = MaterialTheme.typography.bodySmall) }
                            summary.details.forEach { (k, v) ->
                                Text("$k: $v", style = MaterialTheme.typography.bodySmall)
                            }
                        } ?: Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Compile-check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/WizardItemRow.kt
git commit -m "feat: render per-list checklist in WizardItemRow when subItems is present"
```

---

### Task 12: Wire `onToggleListSelected` into the wizard screens + DI

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/BackupWizardScreen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/RestoreWizardScreen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt`

**Interfaces:**
- Consumes: `BackupWizardViewModel.toggleListSelected`/`RestoreWizardViewModel.toggleListSelected` (Tasks 9, 10), `WizardItemRow.onToggleListSelected` (Task 11), `Backup.peekListContents` (Task 6)

- [ ] **Step 1: Wire `onToggleListSelected` in `BackupWizardScreen.kt`**

In the `SelectItems` step's `WizardItemRow` call:

```kotlin
                        items(
                            items = state.items,
                            key = { it.uiInfo.key }
                        ) { item ->
                            WizardItemRow(
                                item = item,
                                onToggleSelected = { viewModel.toggleSelected(item.uiInfo.key) },
                                onToggleExpanded = { viewModel.toggleExpanded(item.uiInfo.key) },
                                onToggleListSelected = { listId -> viewModel.toggleListSelected(listId) },
                            )
                        }
```

The `Review` step's `WizardItemRow` call stays read-only (`onToggleListSelected = {}` — already the default, no change needed there).

- [ ] **Step 2: Wire `onToggleListSelected` in `RestoreWizardScreen.kt`**

Same change in the `SelectItems` step's `WizardItemRow` call:

```kotlin
                        items(
                            items = state.items,
                            key = { it.uiInfo.key }
                        ) { item ->
                            WizardItemRow(
                                item = item,
                                onToggleSelected = { viewModel.toggleSelected(item.uiInfo.key) },
                                onToggleExpanded = { viewModel.toggleExpanded(item.uiInfo.key) },
                                onToggleListSelected = { listId -> viewModel.toggleListSelected(listId) },
                            )
                        }
```

- [ ] **Step 3: Update DI wiring in `ViewModelModule.kt`**

```kotlin
    viewModel {
        BackupWizardViewModel<PlatformFile>(
            uiInfos = getAll(),
            listDao = get(),
            resultsFlow = get<BackgroundWorkHandler>().backupResultsFlow(),
            startBackup = { file, keys, listIds -> get<BackgroundWorkHandler>().startBackup(file, keys, listIds) },
        )
    }
    viewModel {
        RestoreWizardViewModel<PlatformFile>(
            uiInfos = getAll(),
            peekZip = { file -> get<Backup>().peekBackup(file, getAll()) },
            peekListContents = { file -> get<Backup>().peekListContents(file) },
            resultsFlow = get<BackgroundWorkHandler>().restoreResultsFlow(),
            startRestore = { file, keys, listIds -> get<BackgroundWorkHandler>().startRestore(file, keys, listIds) },
        )
    }
```

`get()` for `ListDao` resolves via the existing Koin registration backing `ListRepository`/`ListDatabase` (already registered elsewhere in the graph — `ListBackupProcessor` itself already receives a `ListDao` the same way).

- [ ] **Step 4: Full module compile + test run**

Run: `./gradlew :kmpuiviews:compileDebugKotlinAndroid :kmpuiviews:compileKotlinJvm :kmpuiviews:jvmTest`
Expected: PASS

- [ ] **Step 5: Manual smoke test on the desktop app**

Run: `./gradlew :mangaworld:desktop:run`
- Open Backup wizard, create at least 2 custom lists first if none exist, expand "Custom Lists" in Select step, uncheck one list, confirm backup, verify Complete screen shows the "Custom Lists" result.
- Open Restore wizard against that backup file, expand "Custom Lists" in Select step, verify it shows only the list(s) that were included in the backup (with cover thumbnail, item count, and lock icon if the list has biometric enabled), confirm restore.
- Trigger a full backup/restore once and confirm the Complete screen still looks unchanged for every non-list category.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/BackupWizardScreen.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/backuprestore/RestoreWizardScreen.kt \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt
git commit -m "feat: wire per-list selection into the backup/restore wizard screens and DI"
```
