# Activity Timer Backup & Restore with Supabase Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the cumulative "time spent doing" activity counter from a DataStore `Long` into a new Room 3 KMP `SettingsDatabase`/`ActivityTable`, and sync it to/from Supabase using the existing `SyncProcessor` infrastructure with higher-value-wins conflict resolution, triggered only when activity stops (not every second).

**Architecture:** New Room database (`favoritesdatabase` module) replaces the DataStore value as source of truth; a new `ActivityRepository` (`favoritesdatabase/supabase-integration` module, since it depends on `SyncManager`) mediates ticking/stopping/one-time migration; a new `ActivitySyncProcessor` overrides the base `SyncProcessor`'s push/pull to implement fetch-remote-compare-keep-max instead of the default dirty-chunk/`updated_at`-wins logic, while still plugging into the existing `SyncEngine`/`SyncManager`/`BackupPreferencesScreen`/`ManagedTable` machinery for free.

**Tech Stack:** Kotlin Multiplatform, Room 3 KMP, Koin, Supabase Postgrest (`io.github.jan.supabase`), Jetpack Compose, `androidx.lifecycle` (`ProcessLifecycleOwner`).

## Global Constraints

- Sync to Supabase happens only when activity stops — never on the per-second tick. (Design doc, Overview / Out of Scope)
- Conflict resolution is "higher value wins" in both directions (push and pull), not last-write-wins by timestamp. (Design doc, Overview)
- `SettingsDatabase` is a new, separate Room database from any existing database in `favoritesdatabase` — its own file, own `RoomDatabase` subclass. (Prompt, Additional Notes)
- The Supabase table must have a `user_id` column tied to the authenticated user. (Prompt, Additional Notes)
- No new UI/screens — the feature only adds a row to the existing generic Backup Preferences list via DI registration. (Design doc, Out of Scope)
- No unit tests — verification is by Gradle compile tasks only. (Prompt, Additional Notes)
- Reuse the existing `SyncProcessor`/`SyncEngine`/`ManagedTable` infrastructure rather than a bespoke Supabase call path. (User decision during brainstorming)

Spec: `docs/superpowers/specs/2026-07-23-activity-timer-supabase-backup-design.md`

## Task Dependency Graph

```
T1 (SettingsDatabase/ActivityTable/ActivityDao) ─┬─> T3 (ActivityRow + mappers) ─┐
                                                  ├─> T4 (ActivityManagedTable) ─┼─> T5 (ActivitySyncProcessor)
                                                  └─> T6 (ActivityRepository + DI) ─┬─> T7 (register processor in SupabaseModule.kt)
                                                                                    ├─> T8 (RecordTimeSpentDoing wiring)
                                                                                    └─> T9 (KmpOtakuApp: migration + ON_STOP)
T2 (Supabase SQL schema) ── independent, no deps

T10 (full build verification) depends on T1-T9
```

T7 also requires T5 (it registers `ActivitySyncProcessor`). T7 is sequenced after
T6 (not just T5) even though it doesn't consume anything from T6, because both
tasks edit `SupabaseModule.kt` — T6 adds a line to `supabaseModule()`, T7 adds a
line to `syncProcessorModule()` in the same file, and running them as truly
parallel edits to one file risks a conflict. Do T6 before T7.

Tasks with no dependency edge between them (e.g. T1↔T2, T3↔T4↔T6) can run in
parallel. T8 and T9 can run in parallel with each other and with T5/T7 (they only
depend on T6).

---

### Task 1: `SettingsDatabase` / `ActivityTable` / `ActivityDao`

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/SettingsDatabase.kt`

**Interfaces:**
- Consumes: `DatabaseBuilder` (existing expect class, same package `com.programmersbox.favoritesdatabase`, already used by every other `*Database.getInstance(databaseBuilder)` factory — see `HeatMapDatabase.kt`).
- Produces: `ActivityTable(id: Int = 1, cumulativeSeconds: Long, updatedAt: Long, isDirty: Boolean)`, `ActivityDao` with `getActivity(): ActivityTable?`, `observeActivity(): Flow<ActivityTable?>`, `incrementSeconds(seconds: Long = 1L)`, `markDirtyNow(timestamp: Long)`, `upsertSynced(seconds: Long, timestamp: Long)`, `SettingsDatabase.getInstance(databaseBuilder: DatabaseBuilder): SettingsDatabase`.

- [ ] **Step 1: Create the entity, DAO, and database file**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ActivityTable")
data class ActivityTable(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "cumulative_seconds", defaultValue = "0") val cumulativeSeconds: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_dirty", defaultValue = "0") val isDirty: Boolean = false,
)

@Dao
interface ActivityDao {

    @Query("SELECT * FROM ActivityTable WHERE id = 1")
    suspend fun getActivity(): ActivityTable?

    @Query("SELECT * FROM ActivityTable WHERE id = 1")
    fun observeActivity(): Flow<ActivityTable?>

    @Query(
        "INSERT INTO ActivityTable (id, cumulative_seconds) VALUES (1, :seconds) " +
            "ON CONFLICT(id) DO UPDATE SET cumulative_seconds = cumulative_seconds + :seconds"
    )
    suspend fun incrementSeconds(seconds: Long = 1L)

    @Query("UPDATE ActivityTable SET is_dirty = 1, updated_at = :timestamp WHERE id = 1")
    suspend fun markDirtyNow(timestamp: Long)

    @Query(
        "INSERT INTO ActivityTable (id, cumulative_seconds, updated_at, is_dirty) " +
            "VALUES (1, :seconds, :timestamp, 0) " +
            "ON CONFLICT(id) DO UPDATE SET cumulative_seconds = :seconds, updated_at = :timestamp, is_dirty = 0"
    )
    suspend fun upsertSynced(seconds: Long, timestamp: Long)
}

@Database(entities = [ActivityTable::class], version = 1)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): SettingsDatabase = databaseBuilder
            .build<SettingsDatabase>("settings_database.db")
            .build()
    }
}
```

`incrementSeconds()` is a plain upsert-and-add — it never touches `is_dirty`/`updated_at`. This is what keeps the every-second tick from ever marking the row dirty, which is what keeps the existing WiFi-reactive push path (which reacts to dirty rows) from firing every second.

- [ ] **Step 2: Verify it compiles (Room/KSP schema generation)**

Run: `./gradlew :favoritesdatabase:compileAndroidMain :favoritesdatabase:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`. A new schema JSON should appear under `favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.SettingsDatabase/1.json`.

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/SettingsDatabase.kt favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.SettingsDatabase/
git commit -m "feat(favoritesdatabase): add SettingsDatabase with ActivityTable for activity timer"
```

---

### Task 2: Supabase schema for `activity_timer`

**Files:**
- Modify: `docs/supabase/supabase_schema.sql`

**Interfaces:**
- Consumes: nothing (independent of other tasks).
- Produces: `activity_timer` Postgres table (`user_id` PK, `cumulative_seconds`, `updated_at`) that Task 5's `ActivitySyncProcessor` targets via `tableName = "activity_timer"`.

- [ ] **Step 1: Read the existing `heatmap_items` block for exact formatting**

Run: `grep -n "heatmap_items" -A 20 docs/supabase/supabase_schema.sql`

- [ ] **Step 2: Append the new table, mirroring that block's style**

```sql
CREATE TABLE activity_timer (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id),
    cumulative_seconds BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE activity_timer ENABLE ROW LEVEL SECURITY;

CREATE POLICY own_activity_timer ON activity_timer
    USING (auth.uid() = user_id);
```

- [ ] **Step 3: Commit**

```bash
git add docs/supabase/supabase_schema.sql
git commit -m "docs(supabase): add activity_timer table schema"
```

---

### Task 3: `ActivityRow` DTO + mappers

**Files:**
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SupabaseRows.kt`

**Interfaces:**
- Consumes: `ActivityTable` (from Task 1, `com.programmersbox.favoritesdatabase`).
- Produces: `ActivityRow(userId: String, cumulativeSeconds: Long = 0L, updatedAt: Long = 0L)`, `ActivityRow.toActivityTable(): ActivityTable`, `ActivityTable.toActivityRow(userId: String, timestamp: Long = updatedAt): ActivityRow` — used by Task 5.

- [ ] **Step 1: Add the import**

```kotlin
import com.programmersbox.favoritesdatabase.ActivityTable
```

Add this alongside the existing imports at the top of the file (after `import com.programmersbox.favoritesdatabase.DbModel` alphabetically, i.e. right before `import com.programmersbox.favoritesdatabase.HeatMapItem`).

- [ ] **Step 2: Append the DTO and mappers at the end of the file**

```kotlin

@Serializable
data class ActivityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("cumulative_seconds") val cumulativeSeconds: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

fun ActivityRow.toActivityTable() = ActivityTable(
    cumulativeSeconds = cumulativeSeconds,
    updatedAt = updatedAt,
    isDirty = false,
)

fun ActivityTable.toActivityRow(userId: String, timestamp: Long = updatedAt) = ActivityRow(
    userId = userId,
    cumulativeSeconds = cumulativeSeconds,
    updatedAt = timestamp,
)
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SupabaseRows.kt
git commit -m "feat(supabase-integration): add ActivityRow DTO and mappers"
```

---

### Task 4: `ActivityManagedTable`

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/database/ActivityManagedTable.kt`

**Interfaces:**
- Consumes: `ActivityDao` (Task 1), `ManagedTable`/`SupportedTableAction` (existing, `com.programmersbox.supabaseintegration.database`).
- Produces: `ActivityManagedTable(activityDao: ActivityDao) : ManagedTable` — used by Task 5's `ManagedTable by ActivityManagedTable(activityDao)` delegation.

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.ActivityDao
import kotlin.time.Clock

class ActivityManagedTable(private val activityDao: ActivityDao) : ManagedTable {
    override val displayName: String = "Activity Timer"
    override val supportedActions: List<SupportedTableAction> = listOf(SupportedTableAction.CLEAR_ALL)
    override val defaultAction: SupportedTableAction = SupportedTableAction.CLEAR_ALL

    override suspend fun executeAction(action: SupportedTableAction) {
        if (action == SupportedTableAction.CLEAR_ALL) {
            activityDao.upsertSynced(0L, Clock.System.now().toEpochMilliseconds())
        }
    }
}
```

`PURGE_DELETED`/`RESTORE_DELETED` are omitted from `supportedActions` — there's no soft-delete concept for a single counter row.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/database/ActivityManagedTable.kt
git commit -m "feat(supabase-integration): add ActivityManagedTable for activity timer clear-all action"
```

---

### Task 5: `ActivitySyncProcessor`

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/ActivitySyncProcessor.kt`

**Interfaces:**
- Consumes: `ActivityDao` (Task 1), `ActivityRow`/`toActivityRow()`/`toActivityTable()` (Task 3), `ActivityManagedTable` (Task 4), `BackupPreferenceRepository` (existing, `com.programmersbox.supabaseintegration.sync`), `SyncProcessor`/`ManagedTable` (existing).
- Produces: `ActivitySyncProcessor(activityDao: ActivityDao, backupPreferenceRepository: BackupPreferenceRepository)` — registered by Task 7.

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.ActivityTable
import com.programmersbox.supabaseintegration.database.ActivityManagedTable
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.sync.ActivityRow
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.toActivityRow
import com.programmersbox.supabaseintegration.sync.toActivityTable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ActivitySyncProcessor(
    private val activityDao: ActivityDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<ActivityTable, ActivityRow>(tableName = "activity_timer"),
    ManagedTable by ActivityManagedTable(activityDao) {

    override val displayName: String = "Activity Timer"

    // Higher-value-wins push: fetch remote, compare, upsert the winner both places.
    override suspend fun push(client: SupabaseClient, uid: String) {
        if (!isBackupEnabled()) return
        val local = activityDao.getActivity() ?: return
        if (!local.isDirty) return

        val remote = client.postgrest[tableName].select { filter { eq("user_id", uid) } }
            .decodeSingleOrNull<ActivityRow>()
        val winner = maxOf(local.cumulativeSeconds, remote?.cumulativeSeconds ?: 0L)
        val timestamp = Clock.System.now().toEpochMilliseconds()

        client.postgrest[tableName].upsert(
            local.toActivityRow(uid, timestamp).copy(cumulativeSeconds = winner)
        ) { onConflict = "user_id" }

        activityDao.upsertSynced(winner, timestamp)
    }

    // Higher-value-wins pull: only overwrite local if remote is strictly greater.
    override suspend fun pull(client: SupabaseClient, uid: String, since: Long) {
        if (!isBackupEnabled()) return
        val remote = client.postgrest[tableName].select { filter { eq("user_id", uid) } }
            .decodeSingleOrNull<ActivityRow>() ?: return
        val local = activityDao.getActivity()
        if (remote.cumulativeSeconds > (local?.cumulativeSeconds ?: 0L)) {
            activityDao.upsertSynced(remote.cumulativeSeconds, remote.updatedAt)
        }
    }

    // Remaining abstract members from SyncProcessor are unused by our overridden
    // push()/pull() but must be implemented to satisfy the base class contract.
    override suspend fun getDirtyItems(): List<ActivityTable> =
        activityDao.getActivity()?.takeIf { it.isDirty }?.let { listOf(it) } ?: emptyList()

    override fun observeDirtyItems(): Flow<Int> =
        activityDao.observeActivity().map { if (it?.isDirty == true) 1 else 0 }

    override fun isLocalDeleted(local: ActivityTable): Boolean = false

    override fun getLocalUpdatedAt(local: ActivityTable): Long = local.updatedAt

    override fun toRemoteRow(local: ActivityTable, uid: String, timestamp: Long): ActivityRow =
        local.toActivityRow(uid, timestamp)

    override suspend fun markLocalSynced(local: ActivityTable, timestamp: Long) {
        activityDao.upsertSynced(local.cumulativeSeconds, timestamp)
    }

    override suspend fun deleteLocal(local: ActivityTable) = Unit

    override suspend fun performUpsert(client: SupabaseClient, items: List<ActivityRow>) {
        client.postgrest[tableName].upsert(items) { onConflict = "user_id" }
    }

    override fun isRemoteDeleted(remote: ActivityRow): Boolean = false

    override fun getRemoteUpdatedAt(remote: ActivityRow): Long = remote.updatedAt

    override suspend fun getLocalEquivalent(remote: ActivityRow): ActivityTable? = activityDao.getActivity()

    override suspend fun upsertLocal(remote: ActivityRow) {
        activityDao.upsertSynced(remote.cumulativeSeconds, remote.updatedAt)
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<ActivityRow> =
        postgrestResult.decodeList<ActivityRow>()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/ActivitySyncProcessor.kt
git commit -m "feat(supabase-integration): add ActivitySyncProcessor with higher-value-wins sync"
```

---

### Task 6: `ActivityRepository` + Koin DI wiring

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/repository/ActivityRepository.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt`
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt`

`ActivityRepository` lives in `supabase-integration`, not `kmpuiviews` — it needs
`SyncManager`, which is defined in this module, and `supabase-integration` already
depends on both `favoritesdatabase` (for `ActivityDao`) and `datastore` (for
`DataStoreHandling`), so no new cross-module dependency edge is needed. `kmpModule`
(`kmpuiviews/.../di/KmpModule.kt`) includes both `databases` and (transitively via
`AppModule.kt`) `supabaseModule()`, so both end up in the same Koin container at
runtime — Koin resolves `ActivityDao` for `ActivityRepository` fine across the
Gradle module boundary.

**Interfaces:**
- Consumes: `ActivityDao`/`SettingsDatabase` (Task 1), `DataStoreHandling`/`timeSpentDoing` (existing, `com.programmersbox.datastore`), `SyncManager`/`triggerSync()` (existing, `com.programmersbox.supabaseintegration.sync`).
- Produces: `ActivityRepository` (package `com.programmersbox.supabaseintegration.repository`) with `suspend fun incrementSeconds()`, `suspend fun onActivityStop()`, `suspend fun migrateFromDataStoreIfNeeded()` — used by Task 8 and Task 9.

- [ ] **Step 1: Create `ActivityRepository.kt`**

```kotlin
package com.programmersbox.supabaseintegration.repository

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.supabaseintegration.sync.SyncManager
import kotlin.time.Clock

class ActivityRepository(
    private val activityDao: ActivityDao,
    private val dataStoreHandling: DataStoreHandling,
    private val syncManager: SyncManager,
) {
    suspend fun incrementSeconds() {
        activityDao.incrementSeconds()
    }

    suspend fun onActivityStop() {
        activityDao.markDirtyNow(Clock.System.now().toEpochMilliseconds())
        syncManager.triggerSync()
    }

    suspend fun migrateFromDataStoreIfNeeded() {
        val existing = dataStoreHandling.timeSpentDoing.getOrNull() ?: 0L
        if (existing == 0L) return
        activityDao.incrementSeconds(existing)
        dataStoreHandling.timeSpentDoing.set(0L)
    }
}
```

- [ ] **Step 2: Register `SettingsDatabase`/`ActivityDao` in `DatabaseModule.kt` (kmpuiviews)**

Add this import (alphabetically among the existing `com.programmersbox.favoritesdatabase.*` imports):

```kotlin
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.SettingsDatabase
```

Add these lines inside the `databases` module block (after `single<DictionaryDao> { DictionaryDatabase.getInstance(get()).dictionaryDao() }`):

```kotlin
    single<SettingsDatabase> { SettingsDatabase.getInstance(get()) }
    single<ActivityDao> { get<SettingsDatabase>().activityDao() }
```

- [ ] **Step 3: Register `ActivityRepository` in `SupabaseModule.kt` (supabase-integration)**

Add this import (alongside the other `com.programmersbox.supabaseintegration.*` imports, alphabetically near `com.programmersbox.supabaseintegration.migration.MigrationManager`):

```kotlin
import com.programmersbox.supabaseintegration.repository.ActivityRepository
```

Add this line inside `supabaseModule()`, right after `singleOf(::MigrationManager)`:

```kotlin
    singleOf(::ActivityRepository)
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :kmpuiviews:compileAndroidMain :kmpuiviews:compileKotlinJvm :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/repository/ActivityRepository.kt kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt
git commit -m "feat(supabase-integration): add ActivityRepository and register in DI"
```

---

### Task 7: Register `ActivitySyncProcessor` in `SupabaseModule.kt`

**Files:**
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt`

**Interfaces:**
- Consumes: `ActivitySyncProcessor` (Task 5). Do this task after Task 6 — both edit `SupabaseModule.kt` (Task 6 adds a line to `supabaseModule()`, this task adds a line to `syncProcessorModule()`), so run them sequentially rather than in parallel to avoid a merge conflict on the same file.
- Produces: nothing further downstream — this is a leaf DI registration. `SyncEngineImpl`'s `getAll<SyncProcessor<*, *>>()` and `BackupPreferencesViewModel`'s `getAll()` pick it up automatically at runtime.

- [ ] **Step 1: Add the import**

```kotlin
import com.programmersbox.supabaseintegration.sync.syncprocessor.ActivitySyncProcessor
```

Add alongside the existing `sync.syncprocessor.*` imports, alphabetically right before `import com.programmersbox.supabaseintegration.sync.syncprocessor.BookmarksSyncProcessor` (the first one in that group — "Activity" sorts before "Bookmarks").

- [ ] **Step 2: Register it in `syncProcessorModule()`**

```kotlin
private fun Module.syncProcessorModule() {
    singleOf(::FavoritesSyncer) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::ChaptersWatchedSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::BookmarksSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::NotesSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::HistorySyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::CustomListItemSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::CustomListInfoSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::HeatMapSyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
    singleOf(::ActivitySyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
}
```

(Only the last line, `singleOf(::ActivitySyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)`, is new — added after the existing `HeatMapSyncProcessor` line.)

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt
git commit -m "feat(supabase-integration): register ActivitySyncProcessor"
```

---

### Task 8: Wire `RecordTimeSpentDoing()` to `ActivityRepository`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/ComposableUtils.kt`

**Interfaces:**
- Consumes: `ActivityRepository.incrementSeconds()`, `ActivityRepository.onActivityStop()` (Task 6).
- Produces: nothing further downstream — this is the live-tracking + screen-level stop trigger, already invoked from reader/player screens today.

- [ ] **Step 1: Replace the `DataStoreHandling` import with `ActivityRepository`, add `DisposableEffect`/`rememberCoroutineScope`/`launch` imports**

Old imports block (top of file):
```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.ThemeColor
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
```

New imports block:
```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.supabaseintegration.repository.ActivityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
```

(`DataStoreHandling` import is removed — no longer used in this file after Step 2.)

- [ ] **Step 2: Rewrite `RecordTimeSpentDoing()`**

Old:
```kotlin
@Composable
fun RecordTimeSpentDoing() {
    val timeSpent = koinInject<DataStoreHandling>().timeSpentDoing
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timeSpent.set((timeSpent.getOrNull() ?: 0) + 1)
        }
    }
}
```

New:
```kotlin
@Composable
fun RecordTimeSpentDoing() {
    val activityRepository = koinInject<ActivityRepository>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            activityRepository.incrementSeconds()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch { activityRepository.onActivityStop() }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :kmpuiviews:compileAndroidMain :kmpuiviews:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/ComposableUtils.kt
git commit -m "feat(kmpuiviews): route RecordTimeSpentDoing through ActivityRepository, sync on screen exit"
```

---

### Task 9: App-level migration + `ON_STOP` sync trigger (Android)

**Files:**
- Modify: `gradle/android.versions.toml`
- Modify: `kmpuiviews/build.gradle.kts`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/KmpOtakuApp.kt`

**Interfaces:**
- Consumes: `ActivityRepository.migrateFromDataStoreIfNeeded()`, `ActivityRepository.onActivityStop()` (Task 6).
- Produces: nothing further downstream — this is the app-level entry point.

- [ ] **Step 1: Add the `lifecycle-process` catalog entry**

In `gradle/android.versions.toml`, in the `# Lifecycle Android` section, add a new line after `lifecycleRuntime`:

```toml
lifecycleProcess = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycle" }
```

(Full section after this edit:)
```toml
# Lifecycle Android
lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
lifecycleLivedata = { module = "androidx.lifecycle:lifecycle-livedata-ktx", version.ref = "lifecycle" }
lifecycleRuntime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycleProcess = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycle" }
lifecycleViewModel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
```

- [ ] **Step 2: Add the dependency to `kmpuiviews`'s `androidMain` source set**

In `kmpuiviews/build.gradle.kts`, inside the `androidMain { dependencies { ... } }` block, add:

```kotlin
                implementation(androidLibs.lifecycleProcess)
```

(after `implementation(androidx.paging.pagingCompose)`, the last existing line in that block.)

- [ ] **Step 3: Verify the dependency resolves**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Wire migration + `ON_STOP` observer into `KmpOtakuApp.onCreate()`**

Add these imports (alongside the existing ones, alphabetically):
```kotlin
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.programmersbox.supabaseintegration.repository.ActivityRepository
```

Old (inside `onCreate()`, replacing the commented-out migration block):
```kotlin
        //TODO: Remove the migration after the next full release
        /*migrateSettings(
            context = this,
            dataStoreHandling = dataStoreHandling,
            settingsHandling = settingsHandling,
            newSettingsHandling = newSettingsHandling
        )*/
```

New:
```kotlin
        val activityRepository = get<ActivityRepository>()
        GlobalScope.launch(Dispatchers.IO) { activityRepository.migrateFromDataStoreIfNeeded() }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                GlobalScope.launch(Dispatchers.IO) { activityRepository.onActivityStop() }
            }
        })
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add gradle/android.versions.toml kmpuiviews/build.gradle.kts kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/KmpOtakuApp.kt
git commit -m "feat(kmpuiviews): run one-time activity migration and sync on app background"
```

---

### Task 10: Full build verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1-9.
- Produces: nothing — this is the final gate.

- [ ] **Step 1: Build each Android app (noFirebase debug)**

Run: `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Build the Desktop app**

Run: `./gradlew :mangaworld:desktop:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Full project compile sanity check**

Run: `./gradlew compileDebugKotlin compileKotlinJvm --continue`
Expected: `BUILD SUCCESSFUL` (or only pre-existing unrelated failures — compare against a clean-branch baseline if anything fails)

No commit for this task — it's a verification-only gate.
