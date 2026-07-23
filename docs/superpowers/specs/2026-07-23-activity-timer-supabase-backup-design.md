# Activity Timer Backup & Restore with Supabase — Design

**Date:** 2026-07-23
**Branch:** feat/activity-supabase-backup
**Status:** Approved

---

## Overview

Migrate the cumulative "time spent doing" activity counter (currently a `Long` in
DataStore, incremented every second while a reader/player is open) into a new,
separate Room 3 KMP database (`SettingsDatabase` / `ActivityTable`) inside the
`favoritesdatabase` module. Sync that value to/from Supabase using the existing
`SyncProcessor`/`SyncEngine` infrastructure, but only when the activity stops (not
on every per-second tick), using a "higher value wins" conflict resolution strategy
since the value is monotonically increasing.

---

## Architecture

```
RecordTimeSpentDoing() composable (kmpuiviews)
  ├─ every 1s: activityRepository.incrementSeconds()   (DAO UPDATE, no dirty flag)
  └─ onDispose: activityRepository.onActivityStop()

ProcessLifecycle ON_STOP observer (kmpuiviews, new)
  └─ activityRepository.onActivityStop()

ActivityRepository (kmpuiviews, new Koin singleton)
  ├─ incrementSeconds()          → activityDao.incrementSeconds()
  ├─ onActivityStop()            → activityDao.markDirtyNow(); syncManager.triggerSync()
  └─ migrateFromDataStoreIfNeeded() → one-time, called at app startup

SettingsDatabase (favoritesdatabase, new Room 3 KMP RoomDatabase, own file)
  └─ ActivityTable entity (single row, id = 1)
       cumulativeSeconds: Long, updatedAt: Long, isDirty: Boolean
  └─ ActivityDao
       getActivity(), incrementSeconds(), markDirtyNow(), upsertSynced(value, ts)

ActivitySyncProcessor (favoritesdatabase/supabase-integration, new)
  extends SyncProcessor<ActivityTable, ActivityRow>(tableName = "activity_timer")
  implements ManagedTable by ActivityManagedTable(activityDao)
  overrides push()/pull() with higher-value-wins logic (bypasses base class's
  dirty-chunk / updated_at-wins default, which is built for list-of-rows tables)

Supabase table: activity_timer (user_id PK, cumulative_seconds, updated_at), RLS scoped to auth.uid()
```

---

## Components

### 1. `SettingsDatabase` / `ActivityTable` / `ActivityDao` (favoritesdatabase, new)

File: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/SettingsDatabase.kt`

```kotlin
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
        fun getInstance(databaseBuilder: DatabaseBuilder): SettingsDatabase =
            databaseBuilder.build("settings_database.db")
    }
}
```

`incrementSeconds()` never touches `is_dirty`/`updated_at` — this is the mechanism
that keeps per-second writes from ever marking the row dirty, so the existing
WiFi-reactive push path (which reacts to dirty rows) never fires from ticking alone.

### 2. `ActivityRepository` (kmpuiviews, new)

File: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/ActivityRepository.kt`

```kotlin
class ActivityRepository(
    private val activityDao: ActivityDao,
    private val dataStoreHandling: DataStoreHandling,
    private val syncManager: SyncManager,
) {
    suspend fun incrementSeconds() = activityDao.incrementSeconds()

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

Registered as a Koin singleton in `kmpuiviews/.../di/DatabaseModule.kt` (or a new
small `ActivityModule.kt` alongside it):

```kotlin
single<SettingsDatabase> { SettingsDatabase.getInstance(get()) }
single<ActivityDao> { get<SettingsDatabase>().activityDao() }
singleOf(::ActivityRepository)
```

### 3. Trigger points

- `RecordTimeSpentDoing()` (`kmpuiviews/.../utils/ComposableUtils.kt`) — replace the
  `DataStoreHandling.timeSpentDoing.set(...)` call with
  `activityRepository.incrementSeconds()`, and wrap the loop in a
  `DisposableEffect(Unit) { onDispose { scope.launch { activityRepository.onActivityStop() } } }`
  so leaving the reader/player screen fires a stop.
- Process lifecycle `ON_STOP` — **Android only** (no `ProcessLifecycleOwner`
  observer exists anywhere in the codebase today, and "app backgrounded" isn't a
  meaningful concept on Desktop/JVM — the composable-dispose trigger alone covers
  Desktop). New `DefaultLifecycleObserver` registered in
  `KmpOtakuApp.onCreate()` (`kmpuiviews/src/androidMain/.../KmpOtakuApp.kt`, after
  Koin setup):
  ```kotlin
  ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) {
          val activityRepository = getKoin().get<ActivityRepository>()
          GlobalScope.launch { activityRepository.onActivityStop() }
      }
  })
  ```
- Both paths call the same `onActivityStop()`; double-firing (e.g., closing a
  reader also backgrounds the app) is harmless — `markDirtyNow` is idempotent and a
  second `triggerSync()` is a cheap no-op once nothing is dirty.

### 4. One-time DataStore → Room migration

`activityRepository.migrateFromDataStoreIfNeeded()` is called once from
`KmpOtakuApp.onCreate()` (`kmpuiviews/src/androidMain/.../KmpOtakuApp.kt`), right
after `koinSetup()` — the same call site the codebase's previous (now-deleted)
settings migration used (`b3524269` removed a `migrateSettings(...)` call that
lived exactly here; a commented-out remnant is still at line ~90-96). Launched via
`GlobalScope.launch` matching that prior pattern, since `onCreate()` isn't suspend
and Koin is already started by this point. Idempotent by construction: after
migration the DataStore value is `0`, so every subsequent launch's check is a
no-op. No separate "have I migrated" flag needed.

### 5. `ActivitySyncProcessor` (favoritesdatabase/supabase-integration, new)

File: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/syncprocessor/ActivitySyncProcessor.kt`

```kotlin
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
            ActivityRow(userId = uid, cumulativeSeconds = winner, updatedAt = timestamp)
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
        ActivityRow(userId = uid, cumulativeSeconds = local.cumulativeSeconds, updatedAt = timestamp)
    override suspend fun markLocalSynced(local: ActivityTable, timestamp: Long) =
        activityDao.upsertSynced(local.cumulativeSeconds, timestamp)
    override suspend fun deleteLocal(local: ActivityTable) = Unit
    override suspend fun performUpsert(client: SupabaseClient, items: List<ActivityRow>) {
        client.postgrest[tableName].upsert(items) { onConflict = "user_id" }
    }
    override fun isRemoteDeleted(remote: ActivityRow): Boolean = false
    override fun getRemoteUpdatedAt(remote: ActivityRow): Long = remote.updatedAt
    override suspend fun getLocalEquivalent(remote: ActivityRow): ActivityTable? = activityDao.getActivity()
    override suspend fun upsertLocal(remote: ActivityRow) =
        activityDao.upsertSynced(remote.cumulativeSeconds, remote.updatedAt)
    override suspend fun performSelect(postgrestResult: PostgrestResult): List<ActivityRow> =
        postgrestResult.decodeList<ActivityRow>()
}
```

### 6. `ActivityRow` (remote DTO)

Added to `favoritesdatabase/supabase-integration/.../sync/SupabaseRows.kt`:

```kotlin
@Serializable
data class ActivityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("cumulative_seconds") val cumulativeSeconds: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)
```

### 7. `ActivityManagedTable`

File: `favoritesdatabase/supabase-integration/.../database/ActivityManagedTable.kt`

```kotlin
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

`PURGE_DELETED`/`RESTORE_DELETED` don't apply — there's no soft-delete concept for
a single counter row.

### 8. DI registration

`favoritesdatabase/supabase-integration/.../di/SupabaseModule.kt`, inside
`syncProcessorModule()`:

```kotlin
singleOf(::ActivitySyncProcessor) binds arrayOf(SyncProcessor::class, ManagedTable::class)
```

This auto-registers the processor with `SyncEngineImpl` (picked up via
`getAll<SyncProcessor<*, *>>()`) and adds an "Activity Timer" row to the existing
`BackupPreferencesScreen` per-table toggle list — no new screen, just an extra row
in existing UI.

### 9. Supabase schema

Added to `docs/supabase/supabase_schema.sql`, mirroring the `heatmap_items` block:

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

---

## Data Flow

**Live tracking:** `RecordTimeSpentDoing()` ticks every second →
`activityRepository.incrementSeconds()` → `ActivityDao.incrementSeconds()` (atomic
`UPDATE`, no dirty flag touched).

**Backup on stop:** reader/player screen disposed OR app backgrounded →
`activityRepository.onActivityStop()` → `activityDao.markDirtyNow()` (flags the row
dirty exactly once) → `syncManager.triggerSync()` (existing manual-trigger entry
point, same one used by the "Sync Now" button) → `SyncEngineImpl.pushLocalChanges()`
calls `ActivitySyncProcessor.push()` for this table (other processors' dirty-checks
are cheap no-ops) → fetch remote, compare, upsert higher value both locally and
remotely, clear dirty flag.

**Restore:** any existing pull path (sign-in, WiFi Realtime event, polling,
WorkManager daily sync, manual "Sync Now") calls
`SyncEngineImpl.pullRemoteChanges()` → `ActivitySyncProcessor.pull()` for this
table → if remote is strictly greater than local, overwrite local with remote
value.

**One-time migration:** app startup → `activityRepository.migrateFromDataStoreIfNeeded()`
→ if DataStore value is non-zero, add it into `ActivityTable` and zero the
DataStore value; if already zero, no-op.

---

## Error Handling & Edge Cases

- Network/Supabase failures during push/pull are caught by `SyncEngineImpl`'s
  existing per-processor `runCatching` wrapper — a failure on `activity_timer`
  doesn't block other tables, matching existing behavior for every other
  `SyncProcessor`.
- Offline / unauthenticated: `SyncManager.doSync()`'s existing guards already skip
  all processors before any of them run — no extra handling needed in
  `ActivitySyncProcessor`.
- Backup disabled for this table (`isBackupEnabled() == false` via the existing
  `BackupPreferencesScreen` toggle): both `push()` and `pull()` early-return,
  consistent with every other table.
- Migration and restore-pull don't race in a way that loses data: migration only
  ever adds the DataStore value into the (initially zero) local row; a subsequent
  pull compares the post-migration local value against remote and keeps the max.

---

## Files Changed / Added

| File | Change |
|---|---|
| `favoritesdatabase/src/commonMain/kotlin/.../SettingsDatabase.kt` | New — `SettingsDatabase`, `ActivityTable`, `ActivityDao` |
| `favoritesdatabase/supabase-integration/.../sync/syncprocessor/ActivitySyncProcessor.kt` | New |
| `favoritesdatabase/supabase-integration/.../database/ActivityManagedTable.kt` | New |
| `favoritesdatabase/supabase-integration/.../sync/SupabaseRows.kt` | Add `ActivityRow` + mappers |
| `favoritesdatabase/supabase-integration/.../di/SupabaseModule.kt` | Register `ActivitySyncProcessor` in `syncProcessorModule()` |
| `kmpuiviews/.../repository/ActivityRepository.kt` | New |
| `kmpuiviews/.../di/DatabaseModule.kt` | Register `SettingsDatabase`, `ActivityDao`, `ActivityRepository` |
| `kmpuiviews/.../utils/ComposableUtils.kt` | `RecordTimeSpentDoing()` uses `ActivityRepository` instead of `DataStoreHandling.timeSpentDoing`, adds `onDispose` stop hook |
| `kmpuiviews/src/androidMain/kotlin/.../KmpOtakuApp.kt` | Register `ProcessLifecycleOwner` `ON_STOP` observer (Android only); call `activityRepository.migrateFromDataStoreIfNeeded()` once after `koinSetup()` |
| `docs/supabase/supabase_schema.sql` | Add `activity_timer` table + RLS policy |

---

## Out of Scope

- Syncing on a timer/interval or every second — sync only on activity stop (backed
  by immediate `triggerSync()`, not periodic polling).
- Realtime subscriptions for this specific value (the existing Realtime channel
  already covers all tables generically; no per-table Realtime work needed here).
- UI/new screens — `ActivityTimer` gets a row in the existing generic Backup
  Preferences list "for free" via DI registration, not a new screen.
- Syncing any other DataStore value.
- Unauthenticated sync — handled entirely by existing `SyncManager` guards.
- Unit tests.
