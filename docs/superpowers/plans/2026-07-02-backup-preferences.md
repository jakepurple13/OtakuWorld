# Per-Device Backup Preferences Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user toggle, per device, which Supabase-synced tables get pushed/pulled — a
`SyncProcessor` skips `push()`/`pull()` for any table the user has disabled locally, defaulting to
enabled when no preference is stored.

**Architecture:** A new local-only Room database (`SyncPreferences`, in `:favoritesdatabase`) stores
one row per `tableName`. `BackupPreferenceRepository` (in `:favoritesdatabase:supabase-integration`)
wraps its DAO and is constructor-injected into every `SyncProcessor` subclass.
`SyncProcessor.push()`/`pull()` early-return via a new `isBackupEnabled()` guard.
`BackupPreferencesViewModel` combines the repository's preference flow with `AuthManager.authState`
and the Koin-provided `List<SyncProcessor<*, *>>` into UI state; `BackupPreferencesScreen` renders
one `Switch` per processor, disabled when logged out.

**Tech Stack:** Kotlin Multiplatform, Room 3 (`androidx.room3`), Koin, Compose Multiplatform /
Material 3, kotlinx.coroutines.

## Global Constraints

- Preferences are local-only: never synced to Supabase, never cleared on logout (spec sections "Out
  of Scope" #3, #14).
- No preference row for a `tableName` → treat as backup **enabled** (default ON) — this is what
  makes new `SyncProcessor`s appear ON automatically (spec section 12).
- Do not modify `SyncEngine` or `SyncManager` (spec "Out of Scope" #4) — only `SyncProcessor` and
  its subclasses change.
- Do not wire navigation for the new screen (spec "Out of Scope" #1) — produce only the composable +
  ViewModel.
- Toggling OFF never deletes remote data (spec "Out of Scope" #2) — it is purely a guard on`push()`/
  `pull()`.
- Follow this repo's existing test convention: real in-memory Room databases and hand-written fakes
  for interfaces — **no mocking library** (`mockk` is not a dependency anywhere in this repo; do not
  add it).
- Follow this repo's existing convention of bundling an entity + its `@Dao` + its `@Database` class
  in one file (see `NotesDatabase.kt`, `HistoryDatabase.kt`, `BlurHashDatabase.kt`) rather than the
  3-file split a generic template might suggest — this keeps the new code consistent with every
  other Room database in `:favoritesdatabase`.
- Real module path for "`:supabase-integration`" in the spec is *
  *`:favoritesdatabase:supabase-integration`** (it is a nested Gradle module under
  `:favoritesdatabase`, per `settings.gradle.kts`).

---

### Task 1: `SyncPreferences` Room database (entity + DAO + database class)

**Files:**

- Create:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/SyncPreferences.kt`
- Test:
  `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/BackupPreferenceDaoTest.kt`

**Interfaces:**

- Produces: `BackupPreferenceEntity(tableName: String, enabled: Boolean = true)`,
  `BackupPreferenceDao` with `suspend fun upsertPreference(preference: BackupPreferenceEntity)`,
  `suspend fun getPreference(tableName: String): BackupPreferenceEntity?`,
  `fun observeAllPreferences(): Flow<List<BackupPreferenceEntity>>`;`SyncPreferences : RoomDatabase`
  with `abstract fun backupPreferenceDao(): BackupPreferenceDao`and
  `companion object { fun getInstance(databaseBuilder: DatabaseBuilder): SyncPreferences }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupPreferenceDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var dao: BackupPreferenceDao

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("sync-preferences-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.backupPreferenceDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `getPreference returns null when no row exists`() = runTest {
        assertNull(dao.getPreference("notes"))
    }

    @Test
    fun `upsertPreference then getPreference returns stored value`() = runTest {
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = false))

        val result = dao.getPreference("notes")

        assertEquals("notes", result?.tableName)
        assertEquals(false, result?.enabled)
    }

    @Test
    fun `upsertPreference replaces existing row for the same tableName`() = runTest {
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = false))
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = true))

        assertEquals(true, dao.getPreference("notes")?.enabled)
    }

    @Test
    fun `observeAllPreferences emits every stored row`() = runTest {
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = false))
        dao.upsertPreference(BackupPreferenceEntity(tableName = "history", enabled = true))

        val all = dao.observeAllPreferences().first()

        assertEquals(2, all.size)
        assertTrue(all.any { it.tableName == "notes" && !it.enabled })
        assertTrue(all.any { it.tableName == "history" && it.enabled })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
`./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.BackupPreferenceDaoTest"`
Expected: FAIL to compile — `SyncPreferences`, `BackupPreferenceEntity`, `BackupPreferenceDao` are
unresolved references.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "backup_preferences")
data class BackupPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "enabled", defaultValue = "1")
    val enabled: Boolean = true,
)

@Dao
interface BackupPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(preference: BackupPreferenceEntity)

    @Query("SELECT * FROM backup_preferences WHERE table_name = :tableName")
    suspend fun getPreference(tableName: String): BackupPreferenceEntity?

    @Query("SELECT * FROM backup_preferences")
    fun observeAllPreferences(): Flow<List<BackupPreferenceEntity>>
}

@Database(
    entities = [BackupPreferenceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SyncPreferences : RoomDatabase() {
    abstract fun backupPreferenceDao(): BackupPreferenceDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): SyncPreferences =
            databaseBuilder
                .build<SyncPreferences>("sync_preferences.db")
                .build()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
`./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.BackupPreferenceDaoTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/SyncPreferences.kt favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/BackupPreferenceDaoTest.kt
git commit -m "feat: add SyncPreferences Room database for per-device backup toggles"
```

---

### Task 2: `BackupPreferenceRepository`

**Files:**

- Modify: `favoritesdatabase/supabase-integration/build.gradle.kts` (add `jvmTest` dependencies —
  none exist yet)
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/BackupPreferenceRepository.kt`
- Test:
  `favoritesdatabase/supabase-integration/src/jvmTest/kotlin/com/programmersbox/supabaseintegration/sync/BackupPreferenceRepositoryTest.kt`

**Interfaces:**

- Consumes: `BackupPreferenceDao` (Task 1), `BackupPreferenceEntity` (Task 1).
- Produces: `BackupPreferenceRepository(backupPreferenceDao: BackupPreferenceDao)` with
  `suspend fun isBackupEnabled(tableName: String): Boolean`,
  `suspend fun setBackupEnabled(tableName: String, enabled: Boolean)`,
  `fun observeAllPreferences(): Flow<Map<String, Boolean>>`.

- [ ] **Step 1: Add jvmTest dependencies to the module's build file**

`favoritesdatabase/supabase-integration/build.gradle.kts` currently has no `jvmTest.dependencies`
block. Add one inside the existing `sourceSets { ... }` block (after`jvmMain.dependencies { ... }`),
matching the pattern already used in`favoritesdatabase/build.gradle.kts`:

```kotlin
        jvmTest.dependencies {
    implementation(commonLibs.kotlin.test)
    implementation(commonLibs.coroutinesTest)
    implementation(commonLibs.roomRuntime)
    implementation(commonLibs.androidx.room.sqlite)
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.programmersbox.supabaseintegration.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.SyncPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupPreferenceRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var repository: BackupPreferenceRepository

    @BeforeTest
    fun setUp() {
        dbFile =
            File.createTempFile("backup-preference-repo-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = BackupPreferenceRepository(database.backupPreferenceDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `isBackupEnabled defaults to true when no preference stored`() = runTest {
        assertTrue(repository.isBackupEnabled("notes"))
    }

    @Test
    fun `isBackupEnabled reflects a stored disabled preference`() = runTest {
        repository.setBackupEnabled("notes", false)

        assertEquals(false, repository.isBackupEnabled("notes"))
    }

    @Test
    fun `setBackupEnabled can flip a preference back on`() = runTest {
        repository.setBackupEnabled("notes", false)
        repository.setBackupEnabled("notes", true)

        assertTrue(repository.isBackupEnabled("notes"))
    }

    @Test
    fun `observeAllPreferences maps stored rows by tableName`() = runTest {
        repository.setBackupEnabled("notes", false)
        repository.setBackupEnabled("history", true)

        val prefs = repository.observeAllPreferences().first()

        assertEquals(mapOf("notes" to false, "history" to true), prefs)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run:
`./gradlew :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.sync.BackupPreferenceRepositoryTest"`
Expected: FAIL to compile — `BackupPreferenceRepository` is an unresolved reference.

- [ ] **Step 4: Write the implementation**

```kotlin
package com.programmersbox.supabaseintegration.sync

import com.programmersbox.favoritesdatabase.BackupPreferenceDao
import com.programmersbox.favoritesdatabase.BackupPreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackupPreferenceRepository(
    private val backupPreferenceDao: BackupPreferenceDao,
) {
    suspend fun isBackupEnabled(tableName: String): Boolean =
        backupPreferenceDao.getPreference(tableName)?.enabled ?: true

    suspend fun setBackupEnabled(tableName: String, enabled: Boolean) {
        backupPreferenceDao.upsertPreference(
            BackupPreferenceEntity(
                tableName = tableName,
                enabled = enabled
            )
        )
    }

    fun observeAllPreferences(): Flow<Map<String, Boolean>> =
        backupPreferenceDao.observeAllPreferences().map { preferences ->
            preferences.associate { it.tableName to it.enabled }
        }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:
`./gradlew :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.sync.BackupPreferenceRepositoryTest"`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add favoritesdatabase/supabase-integration/build.gradle.kts favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/BackupPreferenceRepository.kt favoritesdatabase/supabase-integration/src/jvmTest/kotlin/com/programmersbox/supabaseintegration/sync/BackupPreferenceRepositoryTest.kt
git commit -m "feat: add BackupPreferenceRepository wrapping backup preference DAO"
```

---

### Task 3: `SyncProcessor` backup-enabled guard

**Files:**

- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/SyncProcessor.kt`
- Test:
  `favoritesdatabase/supabase-integration/src/jvmTest/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/SyncProcessorBackupGuardTest.kt`

**Interfaces:**

- Consumes: `BackupPreferenceRepository` (Task 2).
- Produces: `SyncProcessor` gains `abstract val displayName: String`,
  `abstract val backupPreferenceRepository: BackupPreferenceRepository`,
  `suspend fun isBackupEnabled(): Boolean`. `push()`/`pull()` now early-return when
  `isBackupEnabled()` is false. Every existing subclass (Task 4) must add the two new overrides or
  the module will not compile — that is expected and fixed in Task 4.

- [ ] **Step 1: Write the failing test**

This test defines a minimal in-file `TestSyncProcessor` fake (no mocking library is used anywhere in
this repo) to observe whether `getDirtyItems()`/`performSelect()` are invoked.

```kotlin
package com.programmersbox.supabaseintegration.sync.syncprocessor

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.SyncPreferences
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class TestSyncProcessor(
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<String, String>(tableName = "test_table") {
    override val displayName: String = "Test Table"

    var getDirtyItemsCallCount = 0
    var performSelectCallCount = 0

    override suspend fun getDirtyItems(): List<String> {
        getDirtyItemsCallCount++
        return emptyList()
    }

    override fun observeDirtyItems(): Flow<Int> = flowOf(0)
    override fun isLocalDeleted(local: String) = false
    override fun getLocalUpdatedAt(local: String) = 0L
    override fun toRemoteRow(local: String, uid: String, timestamp: Long) = local
    override suspend fun markLocalSynced(local: String, timestamp: Long) {}
    override suspend fun deleteLocal(local: String) {}
    override suspend fun performUpsert(client: SupabaseClient, items: List<String>) {}
    override fun isRemoteDeleted(remote: String) = false
    override fun getRemoteUpdatedAt(remote: String) = 0L
    override suspend fun getLocalEquivalent(remote: String): String? = null
    override suspend fun upsertLocal(remote: String) {}

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<String> {
        performSelectCallCount++
        return emptyList()
    }
}

class SyncProcessorBackupGuardTest {

    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var repository: BackupPreferenceRepository
    private lateinit var processor: TestSyncProcessor

    private val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://example.supabase.co",
        supabaseKey = "test-key",
    ) { install(Postgrest) }

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("sync-processor-guard-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = BackupPreferenceRepository(database.backupPreferenceDao())
        processor = TestSyncProcessor(repository)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `push skips work when backup disabled for this table`() = runTest {
        repository.setBackupEnabled("test_table", false)

        processor.push(client, uid = "user-1")

        assertEquals(0, processor.getDirtyItemsCallCount)
    }

    @Test
    fun `push runs when backup enabled for this table`() = runTest {
        repository.setBackupEnabled("test_table", true)

        processor.push(client, uid = "user-1")

        assertEquals(1, processor.getDirtyItemsCallCount)
    }

    @Test
    fun `push runs by default when no preference is stored`() = runTest {
        processor.push(client, uid = "user-1")

        assertEquals(1, processor.getDirtyItemsCallCount)
    }

    @Test
    fun `pull skips work when backup disabled for this table`() = runTest {
        repository.setBackupEnabled("test_table", false)

        processor.pull(client, uid = "user-1", since = 0L)

        assertEquals(0, processor.performSelectCallCount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
`./gradlew :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessorBackupGuardTest"`
Expected: FAIL to compile — `TestSyncProcessor` cannot override `backupPreferenceRepository`/
`displayName` because they don't exist yet on `SyncProcessor`, and the "enabled" tests fail
logically since there is no guard yet (once the two abstract members are added,
`getDirtyItemsCallCount` would be 1 in the "disabled" test too, without the guard).

- [ ] **Step 3: Write the implementation — full modified `SyncProcessor.kt`**

```kotlin
package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

abstract class SyncProcessor<LocalModel, RemoteModel : Any>(
    val tableName: String,
) {

    // ==========================================
    // Abstract Methods (Implemented per table)
    // ==========================================

    // Metadata
    abstract val displayName: String
    abstract val backupPreferenceRepository: BackupPreferenceRepository

    // Push Requirements
    abstract suspend fun getDirtyItems(): List<LocalModel>
    abstract fun observeDirtyItems(): Flow<Int>
    abstract fun isLocalDeleted(local: LocalModel): Boolean
    abstract fun getLocalUpdatedAt(local: LocalModel): Long
    abstract fun toRemoteRow(local: LocalModel, uid: String, timestamp: Long): RemoteModel
    abstract suspend fun markLocalSynced(local: LocalModel, timestamp: Long)
    abstract suspend fun deleteLocal(local: LocalModel)

    // The concrete class knows its type, so it handles the actual Supabase upsert call
    abstract suspend fun performUpsert(client: SupabaseClient, items: List<RemoteModel>)

    // Pull Requirements
    abstract fun isRemoteDeleted(remote: RemoteModel): Boolean
    abstract fun getRemoteUpdatedAt(remote: RemoteModel): Long
    abstract suspend fun getLocalEquivalent(remote: RemoteModel): LocalModel?
    abstract suspend fun upsertLocal(remote: RemoteModel)

    // The concrete class handles the Supabase select & decodeList call
    abstract suspend fun performSelect(postgrestResult: PostgrestResult): List<RemoteModel>

    // ==========================================
    // Backup Preference
    // ==========================================

    suspend fun isBackupEnabled(): Boolean = backupPreferenceRepository.isBackupEnabled(tableName)

    // ==========================================
    // Shared Sync Logic
    // ==========================================

    open suspend fun push(client: SupabaseClient, uid: String) {
        if (!isBackupEnabled()) return

        val dirty = getDirtyItems()
        if (dirty.isEmpty()) return

        println("Pushing ${dirty.size} items to $tableName")
        val errors = mutableListOf<Throwable>()

        dirty.chunked(500).forEach { chunk ->
            runCatching {
                val rowsToUpsert = chunk.map { model ->
                    val updatedAt = getLocalUpdatedAt(model)
                    val timestamp =
                        if (updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else updatedAt
                    toRemoteRow(model, uid, timestamp)
                }

                // Delegate to subclass to bypass the reified type error
                performUpsert(client, rowsToUpsert)

                chunk.forEach { model ->
                    val updatedAt = getLocalUpdatedAt(model)
                    val timestamp =
                        if (updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else updatedAt

                    markLocalSynced(model, timestamp)
                    if (isLocalDeleted(model)) {
                        deleteLocal(model)
                    }
                }
            }.onFailure { errors.add(it) }
        }

        if (errors.isNotEmpty()) throw errors.first()
    }

    open suspend fun pull(client: SupabaseClient, uid: String, since: Long) {
        if (!isBackupEnabled()) return

        val allRecords = fetchAllRecords(client, uid, since)
        if (allRecords.isEmpty()) return

        println("Pulling ${allRecords.size} items from $tableName")

        allRecords.forEach { row ->
            val local = getLocalEquivalent(row)
            val localUpdatedAt = local?.let { getLocalUpdatedAt(it) } ?: -1L

            if (local == null || getRemoteUpdatedAt(row) > localUpdatedAt) {
                if (isRemoteDeleted(row)) {
                    if (local != null) deleteLocal(local)
                } else {
                    upsertLocal(row)
                }
            }
        }
    }

    private suspend inline fun fetchAllRecords(
        client: SupabaseClient,
        uid: String,
        since: Long
    ): List<RemoteModel> {
        val allRecords = mutableListOf<RemoteModel>()
        val pageSize = 1000L
        var offset = 0L

        while (true) {
            val toIndex = offset + pageSize - 1

            // Delegate to subclass to bypass the reified type error
            val batch = performSelect(
                client.postgrest[tableName].select {
                    range(offset, toIndex)
                    filter {
                        eq("user_id", uid)
                        gt("updated_at", since)
                    }
                }
            )

            allRecords.addAll(batch)
            if (batch.size < pageSize) break
            offset += pageSize
        }
        return allRecords
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
`./gradlew :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessorBackupGuardTest"`
Expected: still FAILS TO COMPILE at this point — the 8 real `SyncProcessor` subclasses in the same
module (`FavoritesSyncer`, `ChaptersWatchedSyncProcessor`, `BookmarksSyncProcessor`,
`NotesSyncProcessor`, `HistorySyncProcessor`, `CustomListInfoSyncProcessor`,
`CustomListItemSyncProcessor`, `HeatMapSyncProcessor`) do not yet implement the two new abstract
members. This is expected — proceed immediately to Task 4, then return here.

- [ ] **Step 5: Commit** (after Task 4 makes the module compile again)

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/SyncProcessor.kt favoritesdatabase/supabase-integration/src/jvmTest/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/SyncProcessorBackupGuardTest.kt
git commit -m "feat: add backup-enabled guard to SyncProcessor push/pull"
```

---

### Task 4: Update all 8 concrete `SyncProcessor` subclasses

**Files:**

- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/ChaptersWatchedSyncProcessor.kt`
- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/FavoritesSyncProcessor.kt`
- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/BookmarksSyncProcessor.kt`
- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/NotesSyncProcessor.kt`
- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/HistorySyncProcessor.kt`
- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/HeatMapSyncProcessor.kt`
- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/CustomListSyncProcessor.kt` (
  contains two classes: `CustomListInfoSyncProcessor`, `CustomListItemSyncProcessor`)

**Interfaces:**

- Consumes: `BackupPreferenceRepository` (Task 2), the two new abstract members on `SyncProcessor` (
  Task 3).
- Produces: every subclass constructor now also takes
  `private val backupPreferenceRepository: BackupPreferenceRepository`, used by Task 6's Koin
  wiring.

Every subclass gets the same two mechanical additions: an extra constructor parameter, and two
`override val`s placed right after the class's opening `SyncProcessor<...>(tableName = "...")` line.
Below is the exact diff for each file (only the changed lines are shown — everything else in each
file is unchanged).

- [ ] **Step 1: `ChaptersWatchedSyncProcessor.kt`**

```kotlin
class ChaptersWatchedSyncProcessor(
    private val itemDao: ItemDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<ChapterWatched, ChapterWatchedRow>(
    tableName = "chapters_watched"
) {
    override val displayName: String = "Chapters Watched"

// ... rest of file unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block.

- [ ] **Step 2: `FavoritesSyncProcessor.kt`**

```kotlin
class FavoritesSyncer(
    private val itemDao: ItemDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<DbModel, FavoriteItemRow>(
    tableName = "favorite_items"
) {
    override val displayName: String = "Favorites"

// ... rest of file unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block.

- [ ] **Step 3: `BookmarksSyncProcessor.kt`**

```kotlin
class BookmarksSyncProcessor(
    private val bookmarkDao: BookmarkDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<BookmarkedChapter, BookmarkedChapterRow>(
    tableName = "bookmarked_chapters"
) {
    override val displayName: String = "Bookmarks"

// ... rest of file unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block.

- [ ] **Step 4: `NotesSyncProcessor.kt`**

```kotlin
class NotesSyncProcessor(
    private val notesDao: NotesDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<NoteItem, NoteItemRow>(
    tableName = "notes"
) {
    override val displayName: String = "Notes"

// ... rest of file unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block.

- [ ] **Step 5: `HistorySyncProcessor.kt`**

```kotlin
class HistorySyncProcessor(
    private val historyDao: HistoryDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<HistoryItem, HistoryItemRow>(
    tableName = "history"
) {
    override val displayName: String = "History"

// ... rest of file unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block.

- [ ] **Step 6: `HeatMapSyncProcessor.kt`**

```kotlin
class HeatMapSyncProcessor(
    private val heatMapDao: HeatMapDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<HeatMapItem, HeatMapItemRow>(
    tableName = "heatmap_items"
) {
    override val displayName: String = "Activity Heat Map"

// ... rest of file unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block.

- [ ] **Step 7: `CustomListSyncProcessor.kt` (both classes)**

```kotlin
class CustomListInfoSyncProcessor(
    private val listDao: ListDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<CustomListInfo, CustomListInfoRow>(
    tableName = "custom_list_info"
) {
    override val displayName: String = "Custom Lists"

// ... rest of class unchanged ...
```

```kotlin
class CustomListItemSyncProcessor(
    private val listDao: ListDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<CustomListItem, CustomListItemRow>(
    tableName = "custom_list_items"
) {
    override val displayName: String = "Custom List Items"

// ... rest of class unchanged ...
```

Add `import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository` to the file's
import block (once — it covers both classes in the file).

- [ ] **Step 8: Compile the module and run Task 3's guard test**

Run:
`./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessorBackupGuardTest"`
Expected: compiles cleanly; all 4 tests from Task 3 PASS (the "disabled" tests now correctly report
0 calls).

- [ ] **Step 9: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/ChaptersWatchedSyncProcessor.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/FavoritesSyncProcessor.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/BookmarksSyncProcessor.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/NotesSyncProcessor.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/HistorySyncProcessor.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/HeatMapSyncProcessor.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/CustomListSyncProcessor.kt
git commit -m "feat: wire backupPreferenceRepository and displayName into every SyncProcessor subclass"
```

---

### Task 5: `BackupPreferencesViewModel`

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/BackupPreferencesViewModel.kt`
- Test:
  `favoritesdatabase/supabase-integration/src/jvmTest/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/BackupPreferencesViewModelTest.kt`

**Interfaces:**

- Consumes: `BackupPreferenceRepository` (Task 2), `SyncProcessor<*, *>` (Task 3/4), `AuthManager`/
  `AuthState` (existing, `com.programmersbox.supabaseintegration.auth`).
- Produces:
  `data class BackupPreferenceItem(val tableName: String, val displayName: String, val enabled: Boolean)`,
  `data class BackupPreferencesUiState(val items: List<BackupPreferenceItem> = emptyList(), val isLoggedIn: Boolean = false)`,
  `BackupPreferencesViewModel(backupPreferenceRepository: BackupPreferenceRepository, syncProcessors: List<SyncProcessor<*, *>>, authManager: AuthManager) : ViewModel()`
  exposing `val uiState: StateFlow<BackupPreferencesUiState>` and
  `fun setBackupEnabled(tableName: String, enabled: Boolean)` — consumed by
  `BackupPreferencesScreen` (Task 7).

- [ ] **Step 1: Write the failing test**

This reuses the "hand-written fake, no mocking library" convention from
`SyncProcessorBackupGuardTest` (Task 3) and the `backgroundScope.launch { ... .collect {} }` +
`awaitCondition` pattern already used in `AllNotesViewModelTest`.

```kotlin
package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.SyncPreferences
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeSyncProcessor(
    tableName: String,
    override val displayName: String,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<String, String>(tableName = tableName) {
    override suspend fun getDirtyItems(): List<String> = emptyList()
    override fun observeDirtyItems(): Flow<Int> = flowOf(0)
    override fun isLocalDeleted(local: String) = false
    override fun getLocalUpdatedAt(local: String) = 0L
    override fun toRemoteRow(local: String, uid: String, timestamp: Long) = local
    override suspend fun markLocalSynced(local: String, timestamp: Long) {}
    override suspend fun deleteLocal(local: String) {}
    override suspend fun performUpsert(client: SupabaseClient, items: List<String>) {}
    override fun isRemoteDeleted(remote: String) = false
    override fun getRemoteUpdatedAt(remote: String) = 0L
    override suspend fun getLocalEquivalent(remote: String): String? = null
    override suspend fun upsertLocal(remote: String) {}
    override suspend fun performSelect(postgrestResult: PostgrestResult): List<String> = emptyList()
}

private class FakeAuthManager(initial: AuthState = AuthState.Unauthenticated) : AuthManager {
    private val _authState = MutableStateFlow(initial)
    override val authState: StateFlow<AuthState> = _authState
    override fun isLoggedIn(): Boolean = _authState.value is AuthState.Authenticated
    override suspend fun signInWithEmail(email: String, password: String) {}
    override suspend fun signUpWithEmail(email: String, password: String) {}
    override suspend fun signInWithOAuth(provider: OAuthProvider) {}
    override suspend fun signInWithMagicLink(email: String) {}
    override suspend fun signInWithPhone(phone: String, otp: String) {}
    override suspend fun signInAnonymously() {}
    override suspend fun signOut() {}
    override suspend fun deleteAccount() {}
    override suspend fun refreshSession() {}

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }
}

class BackupPreferencesViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var repository: BackupPreferenceRepository
    private lateinit var authManager: FakeAuthManager

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(processors: List<SyncProcessor<*, *>>) = BackupPreferencesViewModel(
        backupPreferenceRepository = repository,
        syncProcessors = processors,
        authManager = authManager,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("backup-prefs-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = BackupPreferenceRepository(database.backupPreferenceDao())
        authManager = FakeAuthManager()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test
    fun `items default to enabled when no preference is stored`() = runTest {
        val processors = listOf(FakeSyncProcessor("notes", "Notes", repository))
        val vm = viewModel(processors)

        val sub = backgroundScope.launch { vm.uiState.collect {} }
        awaitCondition { vm.uiState.value.items.isNotEmpty() }

        assertEquals(1, vm.uiState.value.items.size)
        assertTrue(vm.uiState.value.items.single().enabled)
        assertEquals("Notes", vm.uiState.value.items.single().displayName)
    }

    @Test
    fun `toggling a table off is reflected in uiState`() = runTest {
        val processors = listOf(FakeSyncProcessor("notes", "Notes", repository))
        val vm = viewModel(processors)

        val sub = backgroundScope.launch { vm.uiState.collect {} }
        awaitCondition { vm.uiState.value.items.isNotEmpty() }

        vm.setBackupEnabled("notes", false)
        awaitCondition { vm.uiState.value.items.single().enabled.not() }

        assertFalse(vm.uiState.value.items.single().enabled)
    }

    @Test
    fun `isLoggedIn reflects the current auth state`() = runTest {
        val processors = listOf(FakeSyncProcessor("notes", "Notes", repository))
        val vm = viewModel(processors)

        val sub = backgroundScope.launch { vm.uiState.collect {} }
        awaitCondition { vm.uiState.value.items.isNotEmpty() }
        assertFalse(vm.uiState.value.isLoggedIn)

        authManager.setAuthState(
            AuthState.Authenticated(
                com.programmersbox.supabaseintegration.auth.SupabaseUser(
                    id = "user-1",
                    email = null,
                    phone = null,
                    displayName = null
                )
            )
        )
        awaitCondition { vm.uiState.value.isLoggedIn }

        assertTrue(vm.uiState.value.isLoggedIn)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
`./gradlew :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModelTest"`
Expected: FAIL to compile — `BackupPreferencesViewModel` is an unresolved reference.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupPreferenceItem(
    val tableName: String,
    val displayName: String,
    val enabled: Boolean,
)

data class BackupPreferencesUiState(
    val items: List<BackupPreferenceItem> = emptyList(),
    val isLoggedIn: Boolean = false,
)

class BackupPreferencesViewModel(
    private val backupPreferenceRepository: BackupPreferenceRepository,
    syncProcessors: List<SyncProcessor<*, *>>,
    authManager: AuthManager,
) : ViewModel() {

    val uiState: StateFlow<BackupPreferencesUiState> = combine(
        backupPreferenceRepository.observeAllPreferences(),
        authManager.authState,
    ) { preferences, authState ->
        BackupPreferencesUiState(
            items = syncProcessors.map { processor ->
                BackupPreferenceItem(
                    tableName = processor.tableName,
                    displayName = processor.displayName,
                    enabled = preferences[processor.tableName] ?: true,
                )
            },
            isLoggedIn = authState is AuthState.Authenticated,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupPreferencesUiState(),
    )

    fun setBackupEnabled(tableName: String, enabled: Boolean) {
        viewModelScope.launch {
            backupPreferenceRepository.setBackupEnabled(tableName, enabled)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
`./gradlew :favoritesdatabase:supabase-integration:jvmTest --tests "com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModelTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/BackupPreferencesViewModel.kt favoritesdatabase/supabase-integration/src/jvmTest/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/BackupPreferencesViewModelTest.kt
git commit -m "feat: add BackupPreferencesViewModel combining preferences and auth state"
```

---

### Task 6: Koin wiring

**Files:**

- Modify:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt`

**Interfaces:**

- Consumes: `SyncPreferences.getInstance(DatabaseBuilder)` (Task 1 — `DatabaseBuilder` is already
  bound to Koin in `kmpuiviews`'s platform modules, loaded app-wide alongside `supabaseModule`),
  `BackupPreferenceRepository` (Task 2), updated constructors from Task 4,
  `BackupPreferencesViewModel` (Task 5).

- [ ] **Step 1: Update `SupabaseModule.kt`**

Add imports:

```kotlin
import com.programmersbox.favoritesdatabase.BackupPreferenceDao
import com.programmersbox.favoritesdatabase.SyncPreferences
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModel
import org.koin.core.module.dsl.viewModel
```

In the `supabaseModule` block, add the database/repository singles and the new view model right
after the existing `singleOf(::MigrationManager)` line:

```kotlin
    singleOf(::MigrationManager)

single<SyncPreferences> { SyncPreferences.getInstance(get()) }
single<BackupPreferenceDao> { get<SyncPreferences>().backupPreferenceDao() }
single { BackupPreferenceRepository(get()) }
```

Add the view model registration alongside the other `viewModelOf(...)` calls. **Do not
use `viewModelOf` here** — it only calls `get()` for each constructor parameter, and
`List<SyncProcessor<*, *>>` needs `getAll()` instead (this is the same reason `SyncEngineImpl` above
is registered with an explicit `single { ... }` lambda rather than `singleOf`):

```kotlin
    viewModel { BackupPreferencesViewModel(get(), getAll(), get()) }
```

Update `syncProcessorModule()` — no code changes are needed here, since `singleOf(::X)` already
resolves every constructor parameter (including the new
`backupPreferenceRepository: BackupPreferenceRepository`) via `get()` automatically. Leave it
exactly as-is:

```kotlin
private fun Module.syncProcessorModule() {
    singleOf(::FavoritesSyncer) bind SyncProcessor::class
    singleOf(::ChaptersWatchedSyncProcessor) bind SyncProcessor::class
    singleOf(::BookmarksSyncProcessor) bind SyncProcessor::class
    singleOf(::NotesSyncProcessor) bind SyncProcessor::class
    singleOf(::HistorySyncProcessor) bind SyncProcessor::class
    singleOf(::CustomListItemSyncProcessor) bind SyncProcessor::class
    singleOf(::CustomListInfoSyncProcessor) bind SyncProcessor::class
    singleOf(::HeatMapSyncProcessor) bind SyncProcessor::class
}
```

(This one registration, `singleOf(::ChaptersWatchedSyncProcessor) bind SyncProcessor::class`, is
the "existing registration updated to accommodate the new constructor parameter" the spec asks to
demonstrate — the call site is unchanged because `singleOf` resolves constructor params
reflectively; the parameter is satisfied by the `single { BackupPreferenceRepository(get()) }` added
above.)

- [ ] **Step 2: Compile**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt
git commit -m "feat: register SyncPreferences database and BackupPreferenceRepository in Koin"
```

---

### Task 7: `BackupPreferencesScreen` composable

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/BackupPreferencesScreen.kt`

**Interfaces:**

- Consumes: `BackupPreferencesViewModel` (Task 5), `BackButton()` (same package, defined in
  `SupabaseRoutes.kt`) for the top bar back action, matching the convention in`SyncStatusScreen.kt`.
- Produces:
  `@Composable fun BackupPreferencesScreen(viewModel: BackupPreferencesViewModel = koinViewModel())` —
  navigation wiring is out of scope per the spec; this composable is handed off as-is.

There is no Compose UI test harness anywhere in this repo (`createComposeRule`/`ComposeTestRule` do
not appear in any module), so per this repo's existing convention this task is verified by
compilation only, consistent with every other screen in
`favoritesdatabase/supabase-integration/src/commonMain/.../ui/`.

- [ ] **Step 1: Write the implementation**

```kotlin
package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferenceItem
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupPreferencesScreen(viewModel: BackupPreferencesViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Backup Preferences") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(uiState.items, key = BackupPreferenceItem::tableName) { item ->
                ListItem(
                    headlineContent = { Text(item.displayName) },
                    trailingContent = {
                        Switch(
                            checked = item.enabled,
                            enabled = uiState.isLoggedIn,
                            onCheckedChange = { checked ->
                                viewModel.setBackupEnabled(item.tableName, checked)
                            }
                        )
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Compile**

Run:
`./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm :favoritesdatabase:supabase-integration:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/BackupPreferencesScreen.kt
git commit -m "feat: add BackupPreferencesScreen with per-table backup toggles"
```

---

## Final Verification

- [ ] Run the full module's test suite:
  `./gradlew :favoritesdatabase:test :favoritesdatabase:supabase-integration:test`
- [ ] Run a full project build to confirm nothing else broke:
  `./gradlew :mangaworld:assembleNoFirebaseDebug`
