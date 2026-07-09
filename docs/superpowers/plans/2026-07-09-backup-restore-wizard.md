# Backup & Restore Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat "Create Full Backup" / "Restore Full Backup" rows in `MoreSettingsScreen.kt` with a multi-step selective backup/restore wizard, driven by a new `BackupUiInfo` interface that every `BackupProcessor` implements.

**Architecture:** `BackupUiInfo` + `BackupDataSummary` + `ItemResult` live in `:sharedcomponents` (commonMain). All 13 existing `BackupProcessor` subclasses (+ mangaworld's variant) additionally implement `BackupUiInfo`. `Zipper` (kmpuiviews expect/actual) gains selective `zipFile`/`readZip` (filtered by a `Set<String>` of keys, with a per-item progress callback) and a read-only `peekZip` for restore-side summaries. Android execution stays on WorkManager with live per-item progress; Desktop/JVM runs the whole job in one shot via the existing nucleus task system and reports only a final result list. The wizard UI (stepper, checklist, review, executing, complete) is new Compose code in `:sharedcomponents`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, kotlinx.serialization, kotlinx-datetime, okio, WorkManager (Android), `io.github.kdroidfilter.nucleus.scheduler` (Desktop), FileKit (file picking, already wired).

## Global Constraints

- Do not modify the `BackupProcessor` abstract class itself (`sharedtools/.../BackupProcessor.kt` lines 16-69) — spec requirement.
- `BackupUiInfo.parseSummary` takes `rawBytes: ByteArray?`, not `okio.BufferedSource` — avoids adding an `okio` dependency to `:sharedcomponents` (it has none today). `okio` types are only used inside `kmpuiviews`'s `Zipper` actuals, which already depend on it transitively.
- iOS: UI renders fully; execution (`Zipper`/`BackgroundWorkHandler`) has no iOS actual and is not added in this plan. The Review step's confirm action must be gated for iOS (see Task 19).
- Every `ItemResult` and Koin-crossing type must be `@Serializable` (kotlinx.serialization) since Android threads it through `WorkManager` `Data`.
- Follow existing package conventions exactly: new sharedcomponents backup code goes in `com.programmersbox.sharedcomponents.backup`; kmpuiviews changes stay in their current packages.

---

## Task 1: `BackupUiInfo`, `BackupDataSummary`, `ItemResult`

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupUiInfo.kt`
- Modify: `sharedcomponents/build.gradle.kts`
- Test: `sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/backup/BackupDataSummaryTest.kt`

**Interfaces:**
- Produces: `BackupUiInfo` (key, displayName, description, icon, `currentSummary()`, `parseSummary(json, rawBytes)`), `BackupDataSummary(itemCount, sizeBytes, lastModified, details)`, `ItemResult(key, success, error)`. All later tasks consume these exact names/shapes.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.sharedcomponents.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupDataSummaryTest {
    @Test
    fun `default summary has no fields set`() {
        val summary = BackupDataSummary()
        assertNull(summary.itemCount)
        assertNull(summary.sizeBytes)
        assertNull(summary.lastModified)
        assertEquals(emptyList(), summary.details)
    }

    @Test
    fun `item result defaults error to null`() {
        val result = ItemResult(key = "favorites.json", success = true)
        assertNull(result.error)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sharedcomponents:jvmTest --tests "com.programmersbox.sharedcomponents.backup.BackupDataSummaryTest"`
Expected: FAIL — `BackupDataSummary`/`ItemResult` unresolved references.

- [ ] **Step 3: Write the interface and data classes**

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface BackupUiInfo {
    val key: String
    val displayName: String
    val description: String?
    val icon: ImageVector?
    suspend fun currentSummary(): BackupDataSummary
    suspend fun parseSummary(json: String?, rawBytes: ByteArray?): BackupDataSummary
}

data class BackupDataSummary(
    val itemCount: Int? = null,
    val sizeBytes: Long? = null,
    val lastModified: Instant? = null,
    val details: List<Pair<String, String>> = emptyList(),
)

@Serializable
data class ItemResult(
    val key: String,
    val success: Boolean,
    val error: String? = null,
)
```

- [ ] **Step 4: Add dependencies to `:sharedcomponents`**

In `sharedcomponents/build.gradle.kts`, inside the `commonMain { dependencies { ... } }` block, add (next to the existing `implementation(commonLibs.kotlinxSerialization)` line):

```kotlin
                implementation(commonLibs.kotlinx.datetime)
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :sharedcomponents:jvmTest --tests "com.programmersbox.sharedcomponents.backup.BackupDataSummaryTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add sharedcomponents/build.gradle.kts sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupUiInfo.kt sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/backup/BackupDataSummaryTest.kt
git commit -m "feat(sharedcomponents): add BackupUiInfo, BackupDataSummary, ItemResult"
```

---

## Task 2: Koin helper that binds both `BackupProcessor` and `BackupUiInfo`

Full reflection (`kotlin.reflect.full.isSuperclassOf`) is JVM-only and unavailable in this KMP `commonMain` — so a runtime type check inside the existing `sharedtools` helper is not viable for iOS. Instead this task adds a **new, separate** helper in `kmpuiviews` (which already depends on both `:sharedtools` and `:sharedcomponents`) with a compile-time `where T : BackupProcessor, T : BackupUiInfo` bound. The existing `sharedtools/.../BackupProcessor.kt` helpers are untouched.

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/BackupUiInfoKoin.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/di/BackupUiInfoKoinTest.kt`

**Interfaces:**
- Consumes: `BackupProcessor` (`sharedtools`), `BackupUiInfo` (`sharedcomponents.backup`).
- Produces: `Module.backupProcessorWithUiInfo(named: String, factoryBlock: () -> T)` where `T : BackupProcessor, T : BackupUiInfo`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.kmpuiviews.di

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeProcessor : BackupProcessor(), BackupUiInfo {
    override val fileName = "fake.json"
    override val key = "fake.json"
    override val displayName = "Fake"
    override val description: String? = null
    override val icon = null
    override suspend fun backup(sink: BufferedSink) {}
    override suspend fun restore(json: String, bufferedSource: BufferedSource) {}
    override suspend fun currentSummary() = BackupDataSummary()
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary()
}

class BackupUiInfoKoinTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun `binds both BackupProcessor and BackupUiInfo`() {
        val koin = koinApplication {
            modules(module { backupProcessorWithUiInfo("fake", ::FakeProcessor) })
        }.koin

        val asProcessor = koin.getAll<BackupProcessor>()
        val asUiInfo = koin.getAll<BackupUiInfo>()

        assertEquals(1, asProcessor.size)
        assertEquals(1, asUiInfo.size)
        assertTrue(asProcessor.single().fileName == "fake.json")
        assertTrue(asUiInfo.single().key == "fake.json")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.di.BackupUiInfoKoinTest"`
Expected: FAIL — `backupProcessorWithUiInfo` unresolved reference.

- [ ] **Step 3: Write the helper**

```kotlin
package com.programmersbox.kmpuiviews.di

import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.core.qualifier.named
import org.koin.dsl.bind

inline fun <reified T> Module.backupProcessorWithUiInfo(
    named: String,
    crossinline factoryBlock: () -> T,
) where T : BackupProcessor, T : BackupUiInfo {
    val definition = factory(named(named)) { new(factoryBlock) }
    definition.bind(BackupProcessor::class)
    definition.bind(BackupUiInfo::class)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.di.BackupUiInfoKoinTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/BackupUiInfoKoin.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/di/BackupUiInfoKoinTest.kt
git commit -m "feat(kmpuiviews): add Koin helper binding BackupProcessor + BackupUiInfo together"
```

---

## Task 3: Implement `BackupUiInfo` on the two proto/binary processors

These have no natural item count — summary is descriptive only, plus size once the raw bytes are known (restore side).

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NewSettingsBackupProcessor.kt`
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/MangaNewSettingsBackupProcessor.kt`

**Interfaces:**
- Consumes: `BackupUiInfo`, `BackupDataSummary` (Task 1).
- Produces: both classes now also satisfy `BackupUiInfo`, consumed by Task 6's Koin registration switch.

- [ ] **Step 1: Edit `NewSettingsBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.Settings
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        newSettingsHandling
            .preferences
            .data
            .firstOrNull()
            ?.encode(sink)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        newSettingsHandling
            .preferences
            .updateData { Settings.ADAPTER.decode(bufferedSource) }
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

- [ ] **Step 2: Edit `MangaNewSettingsBackupProcessor.kt`**

```kotlin
package com.programmersbox.manga.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class MangaNewSettingsBackupProcessor(
    private val mangaNewSettingsHandling: MangaNewSettingsHandling,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "manga_settings"

    override val key: String get() = fileName
    override val displayName: String get() = "Manga Settings"
    override val description: String? get() = "MangaWorld-specific preferences"
    override val icon get() = Icons.Default.Settings

    override suspend fun backup(sink: BufferedSink) {
        mangaNewSettingsHandling
            .preferences
            .data
            .firstOrNull()
            ?.encode(sink)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        mangaNewSettingsHandling
            .preferences
            .updateData { MangaSettings.ADAPTER.decode(bufferedSource) }
    }

    override suspend fun currentSummary() = BackupDataSummary(
        details = listOf("Type" to "Manga settings"),
    )

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        sizeBytes = rawBytes?.size?.toLong(),
        details = listOf("Type" to "Manga settings"),
    )
}
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:shared:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (these two classes don't yet satisfy any new Koin bound, so nothing else can break here).

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/NewSettingsBackupProcessor.kt mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/MangaNewSettingsBackupProcessor.kt
git commit -m "feat(backup): implement BackupUiInfo on proto/binary settings processors"
```

---

## Task 4: Implement `BackupUiInfo` on `BackupSettingsProcessor`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/BackupSettingsProcessor.kt`

- [ ] **Step 1: Edit the file**

Add these imports (alongside existing ones) and the three new members. Full file:

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.kmpuiviews.utils.BackupSettings
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class BackupSettingsProcessor : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "backupsettings.json"

    override val key: String get() = fileName
    override val displayName: String get() = "General Preferences"
    override val description: String? get() = "Raw app preference key-value pairs"
    override val icon get() = Icons.Default.Settings

    override suspend fun backup(sink: BufferedSink) {
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
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
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
    }

    private fun BackupSettings.entryCount() =
        stringSettings.size + intSettings.size + longSettings.size +
            booleanSettings.size + doubleSettings.size + byteArraySettings.size

    override suspend fun currentSummary(): BackupDataSummary {
        val map = otakuDataStore.data.firstOrNull()?.asMap().orEmpty()
        return BackupDataSummary(details = listOf("Preferences" to "${map.size} entries"))
    }

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?): BackupDataSummary {
        val count = json?.let { runCatching { it.fromJson<BackupSettings>().entryCount() }.getOrNull() }
        return BackupDataSummary(
            sizeBytes = rawBytes?.size?.toLong(),
            details = listOf("Preferences" to "${count ?: 0} entries"),
        )
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/BackupSettingsProcessor.kt
git commit -m "feat(backup): implement BackupUiInfo on BackupSettingsProcessor"
```

---

## Task 5: Implement `BackupUiInfo` on the 11 DB-table processors

Same mechanical pattern for all 11: `currentSummary()` reports the live row count; `parseSummary()` decodes the JSON list and reports its size plus the raw byte size. Each file below is shown in full — apply each edit to its own file.

**Files (all in `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/`):**
- Modify: `BookmarksBackupProcessor.kt`, `ChaptersWatchedBackupProcessor.kt`, `FavoriteBackupProcessor.kt`, `HeatMapBackupProcessor.kt`, `HistoryBackupProcessor.kt`, `IncognitoBackupProcessor.kt`, `ListBackupProcessor.kt`, `NotesBackupProcessor.kt`, `NotificationsBackupProcessor.kt`, `RecommendationsBackupProcessor.kt`, `SourceOrderBackupProcessor.kt`

- [ ] **Step 1: `BookmarksBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        bookmarkDao
            .getAllBookmarksSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<BookmarkedChapter>>().forEach { bookmarkDao.insertBookmark(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = bookmarkDao.getAllBookmarksSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<BookmarkedChapter>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 2: `ChaptersWatchedBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getAllChaptersSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<ChapterWatched>>().forEach { itemDao.insertChapter(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllChaptersSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<ChapterWatched>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 3: `FavoriteBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        favoritesRepository
            .getAllFavorites()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<DbModel>>().forEach { favoritesRepository.addFavorite(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = favoritesRepository.getAllFavorites().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<DbModel>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 4: `HeatMapBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        heatMapDao
            .getAllHeatMapsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<HeatMapItem>>().forEach { heatMapDao.insertHeatMap(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = heatMapDao.getAllHeatMapsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HeatMapItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 5: `HistoryBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        historyDao
            .getAllHistorySync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<HistoryItem>>().forEach { historyDao.insertHistory(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = historyDao.getAllHistorySync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HistoryItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 6: `IncognitoBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getAllIncognitoSourcesSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<IncognitoSource>>()
            .forEach { itemDao.insertIncognitoSource(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllIncognitoSourcesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<IncognitoSource>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 7: `ListBackupProcessor.kt`**

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

    override suspend fun backup(sink: BufferedSink) {
        listDao
            .getAllListsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<CustomList>>()
            .forEach {
                listRepository.createList(it.item)
                it.list.forEach { listItem -> listRepository.addItem(listItem) }
            }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = listDao.getAllListsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<CustomList>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 8: `NotesBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        notesDao
            .getAllNotesSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        bufferedSource
            .readUtf8()
            .fromJson<List<NoteItem>>()
            .forEach { notesDao.upsertNote(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = notesDao.getAllNotesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<NoteItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 9: `NotificationsBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getAllNotifications()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<NotificationItem>>()
            .forEach { itemDao.insertNotification(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllNotifications().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<NotificationItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 10: `RecommendationsBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        recommendationDao
            .getAllRecommendationsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<Recommendation>>()
            .forEach { recommendationDao.insertRecommendation(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = recommendationDao.getAllRecommendationsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<Recommendation>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 11: `SourceOrderBackupProcessor.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getSourceOrderSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<SourceOrder>>()
            .forEach { itemDao.insertSourceOrder(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getSourceOrderSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<SourceOrder>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
```

- [ ] **Step 12: Compile check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: BUILD SUCCESSFUL. If any `Icons.Default.X` name doesn't resolve, swap it for another icon already used elsewhere in the codebase (`grep -rn "Icons.Default\." kmpuiviews/src/commonMain | sort -u` for known-good names) — icon choice is cosmetic, not load-bearing.

- [ ] **Step 13: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/backupproccesor/
git commit -m "feat(backup): implement BackupUiInfo on the 11 DB-table processors"
```

---

## Task 6: Switch Koin registration to the combined helper

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt:107-121`
- Modify: mangaworld's app module that registers `MangaNewSettingsBackupProcessor` (locate via `grep -rn "MangaNewSettingsBackupProcessor" mangaworld/`)

- [ ] **Step 1: Update `AppModule.kt`**

Replace the `backupProcessors()` function (lines 107-121):

```kotlin
private fun Module.backupProcessors() {
    backupProcessorWithUiInfo("backupSettings", ::BackupSettingsProcessor)
    backupProcessorWithUiInfo("bookmarks", ::BookmarksBackupProcessor)
    backupProcessorWithUiInfo("chaptersWatched", ::ChaptersWatchedBackupProcessor)
    backupProcessorWithUiInfo("favorite", ::FavoriteBackupProcessor)
    backupProcessorWithUiInfo("heatMap", ::HeatMapBackupProcessor)
    backupProcessorWithUiInfo("history", ::HistoryBackupProcessor)
    backupProcessorWithUiInfo("incognito", ::IncognitoBackupProcessor)
    backupProcessorWithUiInfo("list", ::ListBackupProcessor)
    backupProcessorWithUiInfo("newSettings", ::NewSettingsBackupProcessor)
    backupProcessorWithUiInfo("notifications", ::NotificationsBackupProcessor)
    backupProcessorWithUiInfo("sourceOrder", ::SourceOrderBackupProcessor)
    backupProcessorWithUiInfo("notes", ::NotesBackupProcessor)
    backupProcessorWithUiInfo("recommendations", ::RecommendationsBackupProcessor)
}
```

Add the import `com.programmersbox.kmpuiviews.di.backupProcessorWithUiInfo` (same package as the function's file — check if an explicit import is even needed since it's already in package `com.programmersbox.kmpuiviews.di`; if `AppModule.kt` is in that same package, no import is needed).

- [ ] **Step 2: Update mangaworld's registration**

Find the exact line via:

```bash
grep -rn "MangaNewSettingsBackupProcessor" mangaworld/
```

Change its `backupProcessor("...", ::MangaNewSettingsBackupProcessor)` call to `backupProcessorWithUiInfo(...)`, adding the same import as above.

- [ ] **Step 3: Compile check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:compileNoFirebaseDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. If a processor fails the `where T : BackupProcessor, T : BackupUiInfo` bound, it means Task 3/4/5 missed that file — go back and fix it.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt
git commit -am "feat(backup): register all processors with combined BackupProcessor+BackupUiInfo binding"
```

---

## Task 7: Selective `Zipper` — common expect + Android actual

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`

**Interfaces:**
- Produces: `Zipper.zipFile(platformFile, selectedKeys, onItemComplete)`, `Zipper.readZip(platformFile, selectedKeys, onItemComplete)`, `Zipper.peekZip(platformFile, uiInfos)` — all consumed by Task 9 (`Backup`).

- [ ] **Step 1: Rewrite the common expect declaration**

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile

expect class Zipper {
    suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult>

    suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult>

    suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary>
}
```

- [ ] **Step 2: Rewrite the Android actual**

This also fixes a pre-existing latent bug: the old `readZip` read the zip entry twice — once via `zipIs.bufferedReader().readText()` (fully draining the entry) and again via `zipIs.source().buffer()` (now empty). Reading the entry into a `ByteArray` once and wrapping it in an okio `Buffer` for both the text and source views fixes this.

```kotlin
package com.programmersbox.kmpuiviews.utils

import android.content.Context
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.logFirebaseMessage
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
                        val sink = zip.sink().buffer()
                        backup.backup(sink)
                        sink.flush()
                    }
                        .fold(
                            onSuccess = { ItemResult(backup.fileName, success = true) },
                            onFailure = { e -> ItemResult(backup.fileName, success = false, error = e.message) },
                        )
                    results += result
                    onItemComplete(result)
                    if (!result.success) {
                        runCatching { }.also { exceptionDao }
                    }
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
                                val bytes = zipIs.readBytes()
                                val result = runCatching {
                                    processor.restore(
                                        json = bytes.decodeToString(),
                                        bufferedSource = Buffer().apply { write(bytes) },
                                    )
                                }
                                    .fold(
                                        onSuccess = { ItemResult(name, success = true) },
                                        onFailure = { e -> ItemResult(name, success = false, error = e.message) },
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
                            val bytes = zipIs.readBytes()
                            runCatching { uiInfo.parseSummary(json = bytes.decodeToString(), rawBytes = bytes) }
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
}
```

Note: the `if (!result.success) { runCatching { }.also { exceptionDao } }` line in `zipFile` above is a no-op placeholder for logging — replace it with a real call before moving on:

- [ ] **Step 2b: Fix per-item failure logging in `zipFile`**

Replace this block inside `zipFile`'s `forEach`:

```kotlin
                    val result = runCatching {
                        val sink = zip.sink().buffer()
                        backup.backup(sink)
                        sink.flush()
                    }
                        .onFailure { it.printStackTrace(); exceptionDao.insertException(it) }
                        .fold(
                            onSuccess = { ItemResult(backup.fileName, success = true) },
                            onFailure = { e -> ItemResult(backup.fileName, success = false, error = e.message) },
                        )
                    results += result
                    onItemComplete(result)
```

(This removes the placeholder no-op line entirely.)

- [ ] **Step 3: Compile check**

Run: `./gradlew :kmpuiviews:compileNoFirebaseDebugKotlinAndroid`
Expected: FAIL at this point — `Backup.kt`, `BackupWorker.kt`, `RestoreWorker.kt` still call the old 1-arg `zipFile`/`readZip` signatures. This is expected; Tasks 9 and 12 fix those call sites. Confirm the failure is ONLY in those three files (not in `Zipper.kt` itself).

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt
git commit -m "feat(backup): selective + peek Zipper API (common expect + Android actual)"
```

---

## Task 8: Selective `Zipper` — JVM actual

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt`

- [ ] **Step 1: Rewrite the JVM actual**

```kotlin
package com.programmersbox.kmpuiviews.utils

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
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        ZipOutputStream(FileOutputStream(platformFile.absolutePath())).use { zip ->
            backupProcessors.filter { it.fileName in selectedKeys }.forEach { processor ->
                println("Zipping ${processor.fileName}")
                val duration = measureTime {
                    zip.putNextEntry(ZipEntry(processor.fileName))
                    val result = runCatching { processor.backup(zip.sink().buffer()) }
                        .fold(
                            onSuccess = { ItemResult(processor.fileName, success = true) },
                            onFailure = { e -> ItemResult(processor.fileName, success = false, error = e.message) },
                        )
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
                        val duration = measureTime {
                            val bytes = zipIs.readBytes()
                            val result = runCatching {
                                processor.restore(
                                    json = bytes.decodeToString(),
                                    bufferedSource = Buffer().apply { write(bytes) },
                                )
                            }
                                .fold(
                                    onSuccess = { ItemResult(name, success = true) },
                                    onFailure = { e -> ItemResult(name, success = false, error = e.message) },
                                )
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
                        val bytes = zipIs.readBytes()
                        runCatching { uiInfo.parseSummary(json = bytes.decodeToString(), rawBytes = bytes) }
                            .onSuccess { summaries[name] = it }
                    }
                    entry = zipIs.nextEntry
                }
            }
        }
        summaries
    }
}
```

- [ ] **Step 2: Write a JVM test using real temp files (round-trip)**

**Test:** `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt`

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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
    override suspend fun backup(sink: BufferedSink) { sink.writeUtf8(payload) }
    override suspend fun restore(json: String, bufferedSource: BufferedSource) { restoredWith = json }
    override suspend fun currentSummary() = BackupDataSummary(itemCount = 1)
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) =
        BackupDataSummary(itemCount = 1, sizeBytes = rawBytes?.size?.toLong())
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
}
```

- [ ] **Step 3: Run the test**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.ZipperTest"`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.jvm.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/ZipperTest.kt
git commit -m "feat(backup): selective + peek Zipper JVM actual, with round-trip test"
```

---

## Task 9: `Backup.kt` — selective create/restore/peek

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Backup.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/BackupTest.kt`

**Interfaces:**
- Consumes: `Zipper` (Task 7/8).
- Produces: `Backup.createBackup(document, selectedKeys, onItemComplete): List<ItemResult>`, `Backup.restoreBackup(document, selectedKeys, onItemComplete): List<ItemResult>`, `Backup.peekBackup(document, uiInfos): Map<String, BackupDataSummary>`. Consumed by Tasks 11, 12, 14.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.ExceptionDao
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class NoOpExceptionDao : ExceptionDao by ThrowingExceptionDaoStub()
private object ThrowingExceptionDaoStub

class BackupTest {
    private class FakeZipper : Zipper(backupProcessors = emptyList()) {
        var lastSelectedKeys: Set<String>? = null
        var shouldThrow = false
    }

    @Test
    fun `createBackup rethrows and logs on zipper failure`() = runBlocking {
        // Uses a real Zipper with no registered processors and a bogus path to force failure.
        val exceptionDao = com.programmersbox.kmpuiviews.testing.FakeExceptionDao()
        val zipper = Zipper(backupProcessors = emptyList())
        val backup = Backup(exceptionDao, zipper)
        val badFile = PlatformFile("/nonexistent/path/backup.zip")

        assertFailsWith<Exception> {
            backup.createBackup(badFile, setOf("a.json")) { }
        }
        assertEquals(1, exceptionDao.insertedExceptions.size)
    }
}
```

This test needs a `FakeExceptionDao`; check first whether one already exists.

- [ ] **Step 2: Check for an existing `FakeExceptionDao`**

Run: `grep -rn "class FakeExceptionDao" kmpuiviews/src/jvmTest/`

If found, note its package/import and use it directly (skip Step 2b). If not found, do Step 2b.

- [ ] **Step 2b: Add a minimal `FakeExceptionDao` to `Fakes.kt`**

Only if Step 2 found nothing. Add to `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt` (check the real `ExceptionDao` interface shape first via `grep -n "interface ExceptionDao" -A 10 -r favoritesdatabase/` and implement exactly that; do not guess signatures — read the interface before writing the fake).

- [ ] **Step 3: Run test to verify current behavior / adjust to real `ExceptionDao` shape**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.BackupTest"`
Expected: This step is exploratory — fix the test to match whatever `FakeExceptionDao`/`ExceptionDao` actually look like, then confirm it FAILS because `createBackup`/`Zipper` don't yet accept `selectedKeys`.

- [ ] **Step 4: Rewrite `Backup.kt`**

```kotlin
package com.programmersbox.kmpuiviews.utils

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
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> {
        var output: List<ItemResult> = emptyList()
        val time = measureTime {
            output = runCatching { zipper.zipFile(document, selectedKeys, onItemComplete) }
                .logFailureToDatabase()
                .getOrThrow()
        }
        println("Took $time to zip file")
        return output
    }

    suspend fun restoreBackup(
        document: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> =
        runCatching { zipper.readZip(document, selectedKeys, onItemComplete) }
            .logFailureToDatabase()
            .getOrThrow()

    suspend fun peekBackup(document: PlatformFile, uiInfos: List<BackupUiInfo>): Map<String, BackupDataSummary> =
        runCatching { zipper.peekZip(document, uiInfos) }
            .logFailureToDatabase()
            .getOrThrow()

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

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.utils.BackupTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/Backup.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/utils/BackupTest.kt
git commit -m "feat(backup): Backup.kt selective create/restore + peek, returning ItemResult lists"
```

---

## Task 10: `BackgroundWorkHandler` interface change + fix broken test fixtures

This is a breaking signature change (`startBackup`/`startRestore` gain a `selectedKeys` param; two new methods added). Two existing test doubles implement this interface and must be updated in the same commit or the build won't compile.

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt` (lines 156-167, `FakeBackgroundWorkHandler`)
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModelTest.kt`

- [ ] **Step 1: Update the interface**

In `WorkRepository.kt`, add the import and change the interface:

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface WorkRepository {
    val manualCheck: Flow<List<WorkInfoKmp>>
    val allWorkCheck: Flow<List<WorkInfoKmp>>
    fun pruneWork()
    fun checkManually()
}

data class WorkInfoKmp(
    val state: String,
    val source: String,
    val progress: Int?,
    val max: Int?,
    val nextScheduleTimeMillis: LocalDateTime,
)

interface BackgroundWorkHandler {
    fun localToCloudListener(): Flow<List<WorkInfoKmp>>
    fun cloudToLocalListener(): Flow<List<WorkInfoKmp>>
    fun syncLocalToCloud()
    fun syncCloudToLocal()
    fun setupPeriodicCheckers()
    fun workerInfoFlow(): Flow<List<WorkerInfoModel>>
    fun sourceUpdate()
    fun cancel(uuid: String)
    fun startBackup(file: PlatformFile, selectedKeys: Set<String>)
    fun startRestore(file: PlatformFile, selectedKeys: Set<String>)
    fun backupResultsFlow(): Flow<List<ItemResult>>
    fun restoreResultsFlow(): Flow<List<ItemResult>>
}
```

- [ ] **Step 2: Fix `FakeBackgroundWorkHandler` in `Fakes.kt`**

Replace lines 156-167:

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
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>) {}
    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>) {}
    override fun backupResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
    override fun restoreResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
}
```

Add the import `com.programmersbox.sharedcomponents.backup.ItemResult` at the top of `Fakes.kt` if not already present.

- [ ] **Step 3: Fix `MoreSettingsViewModelTest.kt`**

Update the local `RecordingBackgroundWorkHandler` and both delegation tests:

```kotlin
    private class RecordingBackgroundWorkHandler : BackgroundWorkHandler {
        var backupCalledWith: Pair<PlatformFile, Set<String>>? = null
        var restoreCalledWith: Pair<PlatformFile, Set<String>>? = null

        override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
        override fun syncLocalToCloud() {}
        override fun syncCloudToLocal() {}
        override fun setupPeriodicCheckers() {}
        override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())
        override fun sourceUpdate() {}
        override fun cancel(uuid: String) {}
        override fun startBackup(file: PlatformFile, selectedKeys: Set<String>) {
            backupCalledWith = file to selectedKeys
        }
        override fun startRestore(file: PlatformFile, selectedKeys: Set<String>) {
            restoreCalledWith = file to selectedKeys
        }
        override fun backupResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
        override fun restoreResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
    }
```

The `viewModel(...)` factory function and both delegation tests are updated in Task 11 (they also need the new `backupProcessors: List<BackupProcessor>` constructor param) — leave `viewModel(...)` broken for now, this task only needs to compile the fixture class itself; Task 11 finishes the job in the same file.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModelTest.kt
git commit -m "feat(backup): BackgroundWorkHandler gains selectedKeys + results flows"
```

---

## Task 11: `MoreSettingsViewModel` — thread `selectedKeys` for the existing full-restore/backup call sites

`AccountContent.kt` (onboarding) calls `importFullBackup` for a "restore everything" quick action — this must keep working. `MoreSettingsScreen.kt`'s own two rows (Task 21) will stop using this VM for backup/restore entirely, but the VM itself and its two methods stay because onboarding still needs them.

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModel.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModelTest.kt`

- [ ] **Step 1: Rewrite `MoreSettingsViewModel.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile

class MoreSettingsViewModel(
    private val backgroundWorkHandler: BackgroundWorkHandler,
    private val backupProcessors: List<BackupProcessor>,
) : ViewModel() {
    var importExportListStatus: ImportExportListStatus by mutableStateOf(ImportExportListStatus.Idle)

    fun exportFullBackup(document: PlatformFile) {
        backgroundWorkHandler.startBackup(document, backupProcessors.map { it.fileName }.toSet())
    }

    fun importFullBackup(document: PlatformFile) {
        backgroundWorkHandler.startRestore(document, backupProcessors.map { it.fileName }.toSet())
    }
}

sealed class ImportExportListStatus {
    data object Idle : ImportExportListStatus()
    data object Loading : ImportExportListStatus()
    class Error(val throwable: Throwable) : ImportExportListStatus()
    data object Success : ImportExportListStatus()
}
```

- [ ] **Step 2: Fix `MoreSettingsViewModelTest.kt`'s `viewModel(...)` factory and two delegation tests**

```kotlin
    private fun viewModel(
        backgroundWorkHandler: BackgroundWorkHandler = FakeBackgroundWorkHandler(),
        backupProcessors: List<BackupProcessor> = emptyList(),
    ) = MoreSettingsViewModel(
        backgroundWorkHandler = backgroundWorkHandler,
        backupProcessors = backupProcessors,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }
```

```kotlin
    @Test fun `exportFullBackup delegates to backgroundWorkHandler startBackup with all processor keys`() = runTest {
        val handler = RecordingBackgroundWorkHandler()
        val processors = listOf(FakeBackupProcessor("a.json"), FakeBackupProcessor("b.json"))
        val vm = viewModel(handler, processors)
        val file = PlatformFile("backup.zip")

        vm.exportFullBackup(file)

        assertEquals(file, handler.backupCalledWith?.first)
        assertEquals(setOf("a.json", "b.json"), handler.backupCalledWith?.second)
        assertTrue(handler.restoreCalledWith == null)
    }

    @Test fun `importFullBackup delegates to backgroundWorkHandler startRestore with all processor keys`() = runTest {
        val handler = RecordingBackgroundWorkHandler()
        val processors = listOf(FakeBackupProcessor("a.json"))
        val vm = viewModel(handler, processors)
        val file = PlatformFile("backup.zip")

        vm.importFullBackup(file)

        assertEquals(file, handler.restoreCalledWith?.first)
        assertEquals(setOf("a.json"), handler.restoreCalledWith?.second)
        assertTrue(handler.backupCalledWith == null)
    }
```

Add a tiny local fake at the bottom of the test file (or in `Fakes.kt` if preferred):

```kotlin
private class FakeBackupProcessor(name: String) : BackupProcessor() {
    override val fileName: String = name
    override suspend fun backup(sink: okio.BufferedSink) {}
    override suspend fun restore(json: String, bufferedSource: okio.BufferedSource) {}
}
```

Add `import com.programmersbox.sharedtools.BackupProcessor` to the test file's imports.

- [ ] **Step 3: Run the full test file**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsViewModelTest"`
Expected: PASS (all 5 tests, including the two untouched `idle status` / `importExportListStatus can be updated directly` tests).

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModel.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModelTest.kt
git commit -m "feat(backup): thread selectedKeys through MoreSettingsViewModel's full backup/restore"
```

---

## Task 12: Android `BackupWorker` / `RestoreWorker` — selected keys + live progress

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt`

- [ ] **Step 1: Rewrite `BackupWorker.kt`**

```kotlin
package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val BACKUP_NOTIFICATION_ID = 200

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val backup: Backup,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        val selectedKeys = inputData.getStringArray("selectedKeys")?.toSet() ?: return Result.failure()
        setForeground(getForegroundInfo())
        val results = mutableListOf<ItemResult>()
        return runCatching {
            backup.createBackup(readPlatformFile(uri), selectedKeys) { result ->
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

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            title = "Backing up…"
            onlyAlertOnce = true
            ongoing = true
            progress {
                max = 0
                progress = 0
                indeterminate = true
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification)
        }
    }

    private fun postCompletionNotification(title: String, timeoutAfter: Long?) {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            this.title = title
            if (timeoutAfter != null) this.timeoutAfter = timeoutAfter
        }
        applicationContext.getSystemService<NotificationManager>()
            ?.notify(BACKUP_NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 2: Rewrite `RestoreWorker.kt`** (identical shape, restore call instead of backup)

```kotlin
package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val RESTORE_NOTIFICATION_ID = 201

class RestoreWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val backup: Backup,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        val selectedKeys = inputData.getStringArray("selectedKeys")?.toSet() ?: return Result.failure()
        setForeground(getForegroundInfo())
        val results = mutableListOf<ItemResult>()
        return runCatching {
            backup.restoreBackup(readPlatformFile(uri), selectedKeys) { result ->
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

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            title = "Restoring…"
            onlyAlertOnce = true
            ongoing = true
            progress {
                max = 0
                progress = 0
                indeterminate = true
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(RESTORE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(RESTORE_NOTIFICATION_ID, notification)
        }
    }

    private fun postCompletionNotification(title: String, timeoutAfter: Long?) {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            this.title = title
            if (timeoutAfter != null) this.timeoutAfter = timeoutAfter
        }
        applicationContext.getSystemService<NotificationManager>()
            ?.notify(RESTORE_NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt
git commit -m "feat(backup): Android workers accept selectedKeys and stream per-item progress"
```

---

## Task 13: Android `BackgroundWorkHandlerImpl` — pass keys, expose result flows

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`

- [ ] **Step 1: Add imports**

At the top of the file, add:

```kotlin
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
```

(`kotlinx.coroutines.flow.map` may already be imported — check first and don't duplicate.)

- [ ] **Step 2: Replace `startBackup`/`startRestore` and add the two result-flow overrides**

```kotlin
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>) {
        workManager.enqueueUniqueWork(
            "backup",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setInputData(
                    workDataOf(
                        "uri" to file.toAndroidUri("").toString(),
                        "selectedKeys" to selectedKeys.toTypedArray(),
                    )
                )
                .build()
        )
    }

    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>) {
        workManager.enqueueUniqueWork(
            "restore",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RestoreWorker>()
                .setInputData(
                    workDataOf(
                        "uri" to file.toAndroidUri("").toString(),
                        "selectedKeys" to selectedKeys.toTypedArray(),
                    )
                )
                .build()
        )
    }

    override fun backupResultsFlow(): Flow<List<ItemResult>> = workManager
        .getWorkInfosForUniqueWorkFlow("backup")
        .map { infos ->
            infos.firstOrNull()
                ?.let { it.outputData.getString("results") ?: it.progress.getString("results") }
                ?.let { Json.decodeFromString<List<ItemResult>>(it) }
                .orEmpty()
        }

    override fun restoreResultsFlow(): Flow<List<ItemResult>> = workManager
        .getWorkInfosForUniqueWorkFlow("restore")
        .map { infos ->
            infos.firstOrNull()
                ?.let { it.outputData.getString("results") ?: it.progress.getString("results") }
                ?.let { Json.decodeFromString<List<ItemResult>>(it) }
                .orEmpty()
        }
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :kmpuiviews:compileNoFirebaseDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL for the Android source set (JVM source set still fails until Task 14).

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt
git commit -m "feat(backup): Android BackgroundWorkHandlerImpl threads selectedKeys, exposes result flows"
```

---

## Task 14: JVM/Desktop `BackgroundWorkHandlerImpl` — selected keys + final-result flows

No incremental progress on Desktop (per design decision) — a `BackupResultsHolder` singleton is written to once, when the nucleus task finishes.

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.jvm.kt`

- [ ] **Step 1: Add imports to `BackgroundWorkHandlerImpl.kt` (jvmMain)**

```kotlin
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.flow.MutableStateFlow
```

- [ ] **Step 2: Add `BackupResultsHolder` and update `BackupRestoreData`**

Add near the bottom of the file, next to the existing `BackupRestoreData`:

```kotlin
class BackupResultsHolder {
    val backupResults = MutableStateFlow<List<ItemResult>>(emptyList())
    val restoreResults = MutableStateFlow<List<ItemResult>>(emptyList())
}

@Serializable
data class BackupRestoreData(
    val file: PlatformFile,
    val selectedKeys: Set<String>,
)
```

(This replaces the old two-field-less `BackupRestoreData` — same class, new field.)

- [ ] **Step 3: Update `BackgroundWorkHandlerImpl`'s constructor and methods**

```kotlin
class BackgroundWorkHandlerImpl(
    private val settingsHandling: NewSettingsHandling,
    private val resultsHolder: BackupResultsHolder,
) : BackgroundWorkHandler {
    // ... unchanged methods above startBackup ...

    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>) {
        scope.launch {
            TestTaskRunner.runTask(
                BackupWorker(),
                BackupId,
                inputData = TaskData.of(BackupRestoreData(file, selectedKeys))
            )
        }
    }

    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>) {
        scope.launch {
            TestTaskRunner.runTask(
                RestoreWorker(),
                RestoreId,
                inputData = TaskData.of(BackupRestoreData(file, selectedKeys))
            )
        }
    }

    override fun backupResultsFlow(): Flow<List<ItemResult>> = resultsHolder.backupResults
    override fun restoreResultsFlow(): Flow<List<ItemResult>> = resultsHolder.restoreResults

    // ... companion object unchanged ...
}
```

- [ ] **Step 4: Update the `BackupWorker`/`RestoreWorker` `DesktopTask` classes to write into the holder**

```kotlin
class BackupWorker : DesktopTask, KoinComponent {
    private val backup: Backup by inject()
    private val resultsHolder: BackupResultsHolder by inject()

    override suspend fun doWork(context: TaskContext): TaskResult {
        val duration = measureTimedValue {
            runCatching {
                val data = context.inputData<BackupRestoreData>() ?: return@runCatching
                val results = backup.createBackup(data.file, data.selectedKeys) { }
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
                val results = backup.restoreBackup(data.file, data.selectedKeys) { }
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

- [ ] **Step 5: Register `BackupResultsHolder` in `AppModule.jvm.kt`**

Near line 43 (`singleOf(::BackgroundWorkHandlerImpl) { bind<BackgroundWorkHandler>() }`), add above it:

```kotlin
    singleOf(::BackupResultsHolder)
```

Add the import `com.programmersbox.kmpuiviews.repository.BackupResultsHolder` if the file doesn't already import everything from that package with a wildcard.

- [ ] **Step 6: Full compile check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :kmpuiviews:compileNoFirebaseDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL for both source sets. This is the point where the entire backend (Zipper → Backup → Worker → BackgroundWorkHandler) compiles cleanly on both platforms.

- [ ] **Step 7: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.jvm.kt
git commit -m "feat(backup): JVM BackgroundWorkHandlerImpl threads selectedKeys via BackupResultsHolder"
```

---

## Task 15: Wizard step models + `WizardStepper` composable

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/WizardModels.kt`
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/WizardStepper.kt`

**Interfaces:**
- Produces: `BackupWizardStep`, `RestoreWizardStep`, `WizardItemState`. Consumed by Tasks 16-19.

- [ ] **Step 1: `WizardModels.kt`**

```kotlin
package com.programmersbox.sharedcomponents.backup

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

data class WizardItemState(
    val uiInfo: BackupUiInfo,
    val summary: BackupDataSummary? = null,
    val expanded: Boolean = false,
    val selected: Boolean = true,
)
```

- [ ] **Step 2: `WizardStepper.kt`**

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

@Composable
fun WizardStepper(
    steps: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index <= currentIndex
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        textAlign = TextAlign.Center,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        steps.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

- [ ] **Step 2b: Simplify — the above has an unused `SolidColor` import and a redundant nested `Box`/`Row`.** Clean it up:

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WizardStepper(
    steps: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        steps.forEachIndexed { index, label ->
            val isActive = index <= currentIndex
            Text(
                text = "${index + 1}",
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                textAlign = TextAlign.Center,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        steps.forEach { label ->
            Text(text = label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
```

(Use this Step 2b version — it replaces Step 2 in full, don't keep both.)

- [ ] **Step 3: Compile check**

Run: `./gradlew :sharedcomponents:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/WizardModels.kt sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/WizardStepper.kt
git commit -m "feat(sharedcomponents): wizard step models + WizardStepper composable"
```

---

## Task 16: `WizardItemRow` composable

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/WizardItemRow.kt`

**Interfaces:**
- Consumes: `WizardItemState` (Task 15).
- Produces: `WizardItemRow(item, onToggleSelected, onToggleExpanded)` composable, consumed by Task 19.

- [ ] **Step 1: Write the composable**

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WizardItemRow(
    item: WizardItemState,
    onToggleSelected: () -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :sharedcomponents:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/WizardItemRow.kt
git commit -m "feat(sharedcomponents): WizardItemRow composable with expandable summary"
```

---

## Task 17: `BackupWizardViewModel`

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupWizardViewModel.kt`
- Test: `sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/backup/BackupWizardViewModelTest.kt`

**Interfaces:**
- Consumes: `BackupUiInfo`, `WizardItemState`, `BackupWizardStep`, `ItemResult` (Tasks 1, 15).
- Produces: `BackupWizardViewModel<F>(uiInfos: List<BackupUiInfo>, resultsFlow: Flow<List<ItemResult>>, startBackup: (F, Set<String>) -> Unit)` with `state: StateFlow<BackupWizardUiState>` (now including `results: List<ItemResult>`), `toggleSelected(key)`, `toggleExpanded(key)`, `selectAll()`, `deselectAll()`, `goToReview()`, `confirm(file: F)`. `confirm` also starts collecting `resultsFlow` and advances `step` to `Complete` once every selected key has a result — this is the piece that makes the Executing→Complete transition actually happen; Task 19's screen only renders `state`, it doesn't drive this itself. Generic over the file type `F` so this file never needs to import a platform file type — Task 20 resolves `F` to `PlatformFile` only at Koin-registration time, mirroring `RestoreWizardViewModel<F>` (Task 18) exactly. Consumed by Task 19 and wired for real in Task 20.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.sharedcomponents.backup

import kotlinx.coroutines.test.runTest
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

class BackupWizardViewModelTest {
    @Test
    fun `starts on SelectItems with all items selected`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _ -> })
        val state = vm.state.value
        assertEquals(BackupWizardStep.SelectItems, state.step)
        assertTrue(state.items.all { it.selected })
    }

    @Test
    fun `deselectAll clears selection, selectAll restores it`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _ -> })
        vm.deselectAll()
        assertTrue(vm.state.value.items.none { it.selected })
        vm.selectAll()
        assertTrue(vm.state.value.items.all { it.selected })
    }

    @Test
    fun `toggleSelected flips a single item`() = runTest {
        val vm = BackupWizardViewModel<String>(listOf(FakeUiInfo("a"), FakeUiInfo("b")), startBackup = { _, _ -> })
        vm.toggleSelected("a")
        assertEquals(false, vm.state.value.items.first { it.uiInfo.key == "a" }.selected)
        assertEquals(true, vm.state.value.items.first { it.uiInfo.key == "b" }.selected)
    }

    @Test
    fun `goToReview only carries selected items, confirm calls startBackup with the file and their keys`() = runTest {
        var startedWith: Pair<String, Set<String>>? = null
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = flowOf(emptyList()),
            startBackup = { file, keys -> startedWith = file to keys },
        )
        vm.toggleSelected("b")
        vm.goToReview()
        assertEquals(BackupWizardStep.Review, vm.state.value.step)
        assertEquals(listOf("a"), vm.state.value.items.map { it.uiInfo.key })

        vm.confirm("file.zip")
        assertEquals("file.zip" to setOf("a"), startedWith)
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = BackupWizardViewModel<String>(
            listOf(FakeUiInfo("a"), FakeUiInfo("b")),
            resultsFlow = results,
            startBackup = { _, _ -> },
        )
        vm.goToReview()
        vm.confirm("file.zip")
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", success = true))
        assertEquals(BackupWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", success = true), ItemResult("b", success = true))
        assertEquals(BackupWizardStep.Complete, vm.state.value.step)
        assertEquals(2, vm.state.value.results.size)
    }
}
```

Add `import kotlinx.coroutines.flow.MutableStateFlow` and `import kotlinx.coroutines.flow.flowOf` to this test file's imports.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sharedcomponents:jvmTest --tests "com.programmersbox.sharedcomponents.backup.BackupWizardViewModelTest"`
Expected: FAIL — `BackupWizardViewModel`/`BackupWizardUiState` unresolved.

- [ ] **Step 3: Write the ViewModel**

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BackupWizardUiState(
    val step: BackupWizardStep = BackupWizardStep.SelectItems,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

class BackupWizardViewModel<F>(
    uiInfos: List<BackupUiInfo>,
    private val resultsFlow: Flow<List<ItemResult>>,
    private val startBackup: (F, Set<String>) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BackupWizardUiState(items = uiInfos.map { WizardItemState(uiInfo = it) })
    )
    val state: StateFlow<BackupWizardUiState> = _state.asStateFlow()

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

    fun confirm(file: F) {
        val keys = _state.value.items.map { it.uiInfo.key }.toSet()
        _state.update { it.copy(step = BackupWizardStep.Executing) }
        startBackup(file, keys)
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
            _state.update { s ->
                s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(summary = summary) else it })
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sharedcomponents:jvmTest --tests "com.programmersbox.sharedcomponents.backup.BackupWizardViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupWizardViewModel.kt sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/backup/BackupWizardViewModelTest.kt
git commit -m "feat(sharedcomponents): BackupWizardViewModel"
```

---

## Task 18: `RestoreWizardViewModel`

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/RestoreWizardViewModel.kt`
- Test: `sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/backup/RestoreWizardViewModelTest.kt`

**Interfaces:**
- Consumes: `BackupUiInfo`, `WizardItemState`, `RestoreWizardStep`, `ItemResult` (Tasks 1, 15).
- Produces: `RestoreWizardViewModel<F>(uiInfos, peekZip: suspend (F) -> Map<String, BackupDataSummary>, resultsFlow: Flow<List<ItemResult>>, startRestore: (F, Set<String>) -> Unit)` with `pickFile(file)`, `toggleSelected`, `toggleExpanded`, `selectAll`, `deselectAll`, `goToReview`, `confirm()`. Like Task 17, `confirm()` collects `resultsFlow` and advances `step` to `Complete` once every selected key has a result.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.sharedcomponents.backup

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class RestoreFakeUiInfo(override val key: String) : BackupUiInfo {
    override val displayName = key
    override val description: String? = null
    override val icon = null
    override suspend fun currentSummary() = BackupDataSummary()
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(itemCount = 5)
}

class RestoreWizardViewModelTest {
    @Test
    fun `pickFile runs the peek pass and moves to SelectItems with only matched entries`() = runTest {
        val a = RestoreFakeUiInfo("a")
        val b = RestoreFakeUiInfo("b")
        val vm = RestoreWizardViewModel(
            uiInfos = listOf(a, b),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 5)) },
            startRestore = { _, _ -> },
        )

        vm.pickFile("file.zip")

        val state = vm.state.value
        assertEquals(RestoreWizardStep.SelectItems, state.step)
        assertEquals(listOf("a"), state.items.map { it.uiInfo.key })
        assertEquals(5, state.items.single().summary?.itemCount)
    }

    @Test
    fun `confirm calls startRestore with the picked file and selected keys`() = runTest {
        var called: Pair<String, Set<String>>? = null
        val vm = RestoreWizardViewModel(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = flowOf(emptyList()),
            startRestore = { file, keys -> called = file to keys },
        )

        vm.pickFile("file.zip")
        vm.goToReview()
        vm.confirm()

        assertEquals("file.zip" to setOf("a"), called)
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)
    }

    @Test
    fun `confirm advances to Complete once resultsFlow reports every selected key`() = runTest {
        val results = MutableStateFlow<List<ItemResult>>(emptyList())
        val vm = RestoreWizardViewModel(
            uiInfos = listOf(RestoreFakeUiInfo("a")),
            peekZip = { mapOf("a" to BackupDataSummary(itemCount = 1)) },
            resultsFlow = results,
            startRestore = { _, _ -> },
        )

        vm.pickFile("file.zip")
        vm.goToReview()
        vm.confirm()
        assertEquals(RestoreWizardStep.Executing, vm.state.value.step)

        results.value = listOf(ItemResult("a", success = true))
        assertEquals(RestoreWizardStep.Complete, vm.state.value.step)
        assertEquals(1, vm.state.value.results.size)
    }
}
```

Add `import kotlinx.coroutines.flow.MutableStateFlow` and `import kotlinx.coroutines.flow.flowOf` to this test file's imports.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sharedcomponents:jvmTest --tests "com.programmersbox.sharedcomponents.backup.RestoreWizardViewModelTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Write the ViewModel**

Generic over the file type `F` (tests use plain `String`; the real screen passes FileKit's `PlatformFile` — the ViewModel itself never needs to know which):

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RestoreWizardUiState<F>(
    val step: RestoreWizardStep = RestoreWizardStep.PickFile,
    val file: F? = null,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

class RestoreWizardViewModel<F>(
    private val uiInfos: List<BackupUiInfo>,
    private val peekZip: suspend (F) -> Map<String, BackupDataSummary>,
    private val resultsFlow: Flow<List<ItemResult>>,
    private val startRestore: (F, Set<String>) -> Unit,
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

    fun confirm() {
        val file = _state.value.file ?: return
        val keys = _state.value.items.map { it.uiInfo.key }.toSet()
        _state.update { it.copy(step = RestoreWizardStep.Executing) }
        startRestore(file, keys)
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

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sharedcomponents:jvmTest --tests "com.programmersbox.sharedcomponents.backup.RestoreWizardViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/RestoreWizardViewModel.kt sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/backup/RestoreWizardViewModelTest.kt
git commit -m "feat(sharedcomponents): RestoreWizardViewModel"
```

---

## Task 19: `BackupWizardScreen` + `RestoreWizardScreen`

This is the largest remaining task — split into two steps of work, one per screen, since they're independently reviewable even though they share `WizardStepper`/`WizardItemRow`.

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupWizardScreen.kt`
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/RestoreWizardScreen.kt`
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/Platform.kt` (only if a platform capability check doesn't already exist — see Step 1)

**Interfaces:**
- Consumes: `BackupWizardViewModel`/`RestoreWizardViewModel` (Tasks 17, 18), `WizardStepper`/`WizardItemRow` (Tasks 15, 16).
- Produces: `BackupWizardScreen()`, `RestoreWizardScreen()` composables, consumed by Task 20's nav registration.

- [ ] **Step 1: Check for an existing platform-capability signal**

The Review step's confirm action needs to know "does this platform support execution" (Android/JVM: yes; iOS: no). Check whether `:sharedcomponents`'s existing `Platform.kt`/`Platform.android.kt`/`Platform.ios.kt` (mentioned in earlier research) already exposes something usable:

```bash
cat sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/Platform.kt
```

If it exposes an enum/sealed type identifying the platform (Android/JVM/iOS), reuse it directly in Step 3 below instead of adding anything new. If not, add a minimal expect/actual:

```kotlin
// sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupCapability.kt
package com.programmersbox.sharedcomponents.backup

expect val backupRestoreSupported: Boolean
```
Android actual (`sharedcomponents/src/androidMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupCapability.android.kt`): `actual val backupRestoreSupported: Boolean = true`
JVM actual (`sharedcomponents/src/jvmMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupCapability.jvm.kt`): `actual val backupRestoreSupported: Boolean = true`
iOS actual (`sharedcomponents/src/iosMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupCapability.ios.kt`): `actual val backupRestoreSupported: Boolean = false`

- [ ] **Step 2: `BackupWizardScreen.kt`**

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupWizardScreen(
    onDone: () -> Unit,
    viewModel: BackupWizardViewModel<PlatformFile> = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val stepLabels = listOf("Select", "Review", "Backup", "Done")
    val currentIndex = when (state.step) {
        BackupWizardStep.SelectItems -> 0
        BackupWizardStep.Review -> 1
        BackupWizardStep.Executing -> 2
        BackupWizardStep.Complete -> 3
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            WizardStepper(steps = stepLabels, currentIndex = currentIndex, modifier = Modifier.padding(16.dp))

            when (state.step) {
                BackupWizardStep.SelectItems -> {
                    TextButton(onClick = {
                        if (state.items.all { it.selected }) viewModel.deselectAll() else viewModel.selectAll()
                    }) { Text(if (state.items.all { it.selected }) "Deselect All" else "Select All") }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
                            WizardItemRow(
                                item = item,
                                onToggleSelected = { viewModel.toggleSelected(item.uiInfo.key) },
                                onToggleExpanded = { viewModel.toggleExpanded(item.uiInfo.key) },
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::goToReview,
                        modifier = Modifier.padding(16.dp),
                    ) { Text("Next: Review") }
                }

                BackupWizardStep.Review -> {
                    val saveLauncher = rememberFileSaverLauncher(
                        dialogSettings = FileKitDialogSettings.createDefault()
                    ) { document -> document?.let { viewModel.confirm(it) } }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
                            WizardItemRow(item = item.copy(expanded = true), onToggleSelected = {}, onToggleExpanded = {})
                        }
                    }
                    Button(
                        onClick = { saveLauncher.launch("backup", "zip") },
                        enabled = backupRestoreSupported,
                        modifier = Modifier.padding(16.dp),
                    ) { Text(if (backupRestoreSupported) "Confirm Backup" else "Not supported on this platform yet") }
                }

                BackupWizardStep.Executing -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Backing up… (${state.results.size}/${state.items.size} done)")
                    }
                }

                BackupWizardStep.Complete -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Backup complete")
                        state.results.forEach { result ->
                            Text(if (result.success) "✓ ${result.key}" else "✗ ${result.key}: ${result.error}")
                        }
                        Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
                    }
                }
            }
        }
    }
}
```

`backupRestoreSupported` comes from Step 1 above. The `"backup"` filename literal replaces the app-name-prefixed filename the old flat UI used (`"${appName}_backup"`) since `:sharedcomponents` doesn't import `kmpuiviews`'s `AppConfig` — the user still picks the exact save location/name in the system file-saver dialog, so this is a cosmetic simplification, not a functional gap.

- [ ] **Step 3: `RestoreWizardScreen.kt`**

```kotlin
package com.programmersbox.sharedcomponents.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RestoreWizardScreen(
    onDone: () -> Unit,
    viewModel: RestoreWizardViewModel<PlatformFile> = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val stepLabels = listOf("File", "Select", "Review", "Restore", "Done")
    val currentIndex = when (state.step) {
        RestoreWizardStep.PickFile -> 0
        RestoreWizardStep.SelectItems -> 1
        RestoreWizardStep.Review -> 2
        RestoreWizardStep.Executing -> 3
        RestoreWizardStep.Complete -> 4
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            WizardStepper(steps = stepLabels, currentIndex = currentIndex, modifier = Modifier.padding(16.dp))

            when (state.step) {
                RestoreWizardStep.PickFile -> {
                    val pickLauncher = rememberFilePickerLauncher(type = FileKitType.File("zip")) { file ->
                        file?.let { viewModel.pickFile(it) }
                    }
                    Button(onClick = { pickLauncher.launch() }, modifier = Modifier.padding(16.dp)) {
                        Text("Choose Backup File")
                    }
                }

                RestoreWizardStep.SelectItems -> {
                    TextButton(onClick = {
                        if (state.items.all { it.selected }) viewModel.deselectAll() else viewModel.selectAll()
                    }) { Text(if (state.items.all { it.selected }) "Deselect All" else "Select All") }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
                            WizardItemRow(
                                item = item,
                                onToggleSelected = { viewModel.toggleSelected(item.uiInfo.key) },
                                onToggleExpanded = { viewModel.toggleExpanded(item.uiInfo.key) },
                            )
                        }
                    }

                    Button(onClick = viewModel::goToReview, modifier = Modifier.padding(16.dp)) { Text("Next: Review") }
                }

                RestoreWizardStep.Review -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
                            WizardItemRow(item = item.copy(expanded = true), onToggleSelected = {}, onToggleExpanded = {})
                        }
                    }
                    Button(
                        onClick = viewModel::confirm,
                        enabled = backupRestoreSupported,
                        modifier = Modifier.padding(16.dp),
                    ) { Text(if (backupRestoreSupported) "Confirm Restore" else "Not supported on this platform yet") }
                }

                RestoreWizardStep.Executing -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Restoring… (${state.results.size}/${state.items.size} done)")
                    }
                }

                RestoreWizardStep.Complete -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Restore complete")
                        state.results.forEach { result ->
                            Text(if (result.success) "✓ ${result.key}" else "✗ ${result.key}: ${result.error}")
                        }
                        Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Compile check**

Run: `./gradlew :sharedcomponents:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/BackupWizardScreen.kt sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/backup/RestoreWizardScreen.kt sharedcomponents/src/*Main/kotlin/com/programmersbox/sharedcomponents/backup/BackupCapability*.kt
git commit -m "feat(sharedcomponents): BackupWizardScreen and RestoreWizardScreen"
```

*Note: `confirm()`'s Executing→Complete transition (Tasks 17/18) is already fully implemented and unit-tested against a fake `resultsFlow` — this screen only renders `state`. What's still missing is wiring the ViewModels' `startBackup`/`startRestore`/`resultsFlow` constructor params to the real `BackgroundWorkHandler` via Koin, which is exactly what Task 20 covers.*

---

## Task 20: Wire ViewModels to real `BackgroundWorkHandler`, Koin registration, navigation

`BackupWizardViewModel<F>`/`RestoreWizardViewModel<F>` (Tasks 17, 18) are already generic over the file type and already take `resultsFlow`/`startBackup`/`startRestore`/`peekZip` as constructor params — this task only resolves `F = PlatformFile` and supplies the real Koin-injected implementations for those params. No ViewModel or screen code changes here.

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt`

- [ ] **Step 1: Register both ViewModels in `ViewModelModule.kt`**

Find the existing `viewModelOf(::MoreSettingsViewModel)` line (per Task 11's grounding) and add nearby:

```kotlin
    viewModel {
        BackupWizardViewModel<PlatformFile>(
            uiInfos = getAll(),
            resultsFlow = get<BackgroundWorkHandler>().backupResultsFlow(),
            startBackup = { file, keys -> get<BackgroundWorkHandler>().startBackup(file, keys) },
        )
    }
    viewModel {
        RestoreWizardViewModel<PlatformFile>(
            uiInfos = getAll(),
            peekZip = { file -> get<Backup>().peekBackup(file, getAll()) },
            resultsFlow = get<BackgroundWorkHandler>().restoreResultsFlow(),
            startRestore = { file, keys -> get<BackgroundWorkHandler>().startRestore(file, keys) },
        )
    }
```

Add imports: `com.programmersbox.sharedcomponents.backup.BackupWizardViewModel`, `com.programmersbox.sharedcomponents.backup.RestoreWizardViewModel`, `com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler`, `com.programmersbox.kmpuiviews.utils.Backup`, `io.github.vinceglb.filekit.PlatformFile`.

- [ ] **Step 2: Add `Screen.BackupWizard` / `Screen.RestoreWizard`**

In `Screen.kt`, next to `MoreSettings` (line 57):

```kotlin
    @Serializable
    data object BackupWizard : Screen("backup_wizard")

    @Serializable
    data object RestoreWizard : Screen("restore_wizard")
```

- [ ] **Step 3: Register nav entries in `Nav3Graph.kt`**

Next to the existing `detailEntry<Screen.MoreSettings> { MoreSettingsScreen() }` (around line 199):

```kotlin
    detailEntry<Screen.BackupWizard> {
        BackupWizardScreen(onDone = { LocalNavActions.current.popBackStack() })
    }
    detailEntry<Screen.RestoreWizard> {
        RestoreWizardScreen(onDone = { LocalNavActions.current.popBackStack() })
    }
```

Add imports `com.programmersbox.sharedcomponents.backup.BackupWizardScreen`, `com.programmersbox.sharedcomponents.backup.RestoreWizardScreen`, `com.programmersbox.kmpuiviews.utils.LocalNavActions` (check whether `LocalNavActions` is already imported in this file first — likely yes, given other screens use it).

- [ ] **Step 4: Full compile check**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :kmpuiviews:compileNoFirebaseDebugKotlinAndroid :sharedcomponents:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt
git commit -m "feat(backup): wire wizard ViewModels to BackgroundWorkHandler, register nav entries"
```

---

## Task 21: `MoreSettingsScreen.kt` — navigate to the wizard instead of direct export/import

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsScreen.kt`

- [ ] **Step 1: Replace the two backup `item{}` blocks (lines 91-123)**

```kotlin
            item {
                PreferenceSetting(
                    settingTitle = { Text("Create Full Backup") },
                    settingIcon = { Icon(Icons.Default.Backup, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(Screen.BackupWizard) }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Restore Full Backup") },
                    settingIcon = { Icon(Icons.Default.Restore, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(Screen.RestoreWizard) }
                )
            }
```

- [ ] **Step 2: Remove now-unused imports**

Remove these four lines (no longer referenced in this file):

```kotlin
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
```

Add: `import com.programmersbox.kmpuiviews.presentation.Screen` (if not already imported).

Leave everything else in the file untouched — `viewModel: MoreSettingsViewModel`, the toaster/`LaunchedEffect`/`snapshotFlow` watching `importExportListStatus`, and `AppConfig`/`appName` all stay since `koinViewModel()` injection and other pre-existing behavior in this screen are unrelated to this change (and `appName` may be used elsewhere in the file below what was read — verify with a full read before deleting anything else).

- [ ] **Step 3: Compile check**

Run: `./gradlew :kmpuiviews:compileNoFirebaseDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsScreen.kt
git commit -m "feat(backup): MoreSettingsScreen navigates to the new wizard instead of direct export/import"
```

---

## Task 22: Full multi-target build + manual smoke test

**Files:** none (verification only).

- [ ] **Step 1: Full compile across all touched targets**

```bash
./gradlew :sharedcomponents:compileKotlinJvm :sharedcomponents:compileDebugKotlinAndroid \
  :kmpuiviews:compileKotlinJvm :kmpuiviews:compileNoFirebaseDebugKotlinAndroid \
  :mangaworld:compileNoFirebaseDebugKotlinAndroid :mangaworld:shared:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full test suite for touched modules**

```bash
./gradlew :sharedcomponents:jvmTest :kmpuiviews:jvmTest
```
Expected: all tests PASS (Tasks 1, 2, 8, 9, 10, 11, 17, 18's tests).

- [ ] **Step 3: Manual smoke test on Desktop (fastest iteration loop)**

```bash
./gradlew :mangaworld:desktop:run
```
In the running app: open Settings → More Settings → "Create Full Backup" → confirm the stepper shows Select/Review/Backup/Done, expand at least one item to see its summary, deselect one item, proceed through Review, confirm, and verify the Complete screen lists per-item success. Repeat for "Restore Full Backup" using the zip just created — confirm the Select Items step shows real summaries parsed from the zip (not placeholders), and that deselecting an item during restore actually skips it (check the target data isn't touched, e.g. deselect Notes and confirm existing notes are unchanged after restore).

- [ ] **Step 4: Note any follow-up work discovered during the smoke test**

If icon names failed to resolve (Task 5, Step 12) or any UX rough edge shows up, fix inline rather than deferring — this is the last task in the plan.

---

## Self-Review Notes

- **Spec coverage:** every Main Feature/Use Case item from the spec maps to a task — `BackupUiInfo` (Task 1), wizard stepper/checklist (Tasks 15-16, 19), backup flow (Tasks 17, 19-21), restore flow with zip peek (Tasks 7-9, 18-19), selective execution + per-item failure reporting (Tasks 7-14), iOS gating (Task 19). Testing requirement covered throughout via TDD steps.
- **Deviations from the spec doc, and why:** (1) `parseSummary` takes `ByteArray?` not `BufferedSource?`, to avoid a new `okio` dependency in `:sharedcomponents`. (2) The Koin "bind both interfaces" logic lives in a new `kmpuiviews` helper, not inside the existing `sharedtools` helper, because JVM-only reflection can't be used in KMP `commonMain` and `sharedtools` must not gain a dependency on `:sharedcomponents`. (3) `BackupWizardViewModel`/`RestoreWizardViewModel` are generic over the file type (`<F>`) so `:sharedcomponents` never imports `kmpuiviews` types — resolved to `PlatformFile` only at Koin registration (Task 20).
- **Known test-fixture ripple:** `Fakes.kt`'s `FakeBackgroundWorkHandler` and `MoreSettingsViewModelTest.kt`'s `RecordingBackgroundWorkHandler` both implement `BackgroundWorkHandler` and are fixed in Tasks 10-11 — found by grepping before writing the interface change, not left to break the build.
- **Not done in this plan:** iOS zip/worker actuals (explicit spec follow-up), Desktop incremental progress (explicit spec decision — final-result-only), fixing the pre-existing double-consumption bug in `readZip` was folded into Task 7 as a natural side effect of rewriting that method, not a separate task.
