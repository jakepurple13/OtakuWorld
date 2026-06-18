# Supabase Integration Setup Guide

This guide covers everything needed to wire the `:favoritesdatabase:supabase-integration` module into a host app (MangaWorld, AnimeWorld, or NovelWorld).

---

## Prerequisites

- A [Supabase](https://supabase.com) project with **Authentication enabled**
- The project's **URL** and **anon key** (found in Project Settings → API)
- Supabase schema deployed (see Step 1)

---

## Step 1 — Deploy the database schema

Run the contents of `docs/supabase/supabase_schema.sql` in the Supabase **SQL Editor** once per project.

This creates 8 tables with RLS policies and a storage bucket:

| Supabase table | Synced from |
|---|---|
| `favorite_items` | `FavoriteItem` (ItemDatabase) |
| `chapters_watched` | `ChapterWatched` (ItemDatabase) |
| `bookmarked_chapters` | `BookmarkedChapter` (BookmarkDatabase) |
| `notes` | `NoteItem` (NotesDatabase) |
| `history` | `HistoryItem` (HistoryDatabase) |
| `custom_list_items` | `CustomListItem` (CustomList) |
| `custom_list_info` | `CustomListInfo` (CustomList) |
| `heatmap_items` | `HeatMapItem` (HeatMapDatabase) |

A storage bucket named `otakuworld-backups` is also created for JSON backups.

> `recommendations` sync is **not yet implemented** due to a `genre: List<String>` type mapping issue. The table schema is included for future use.

---

## Step 2 — Gradle dependency

Add the module to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(projects.favoritesdatabase.supabaseIntegration)
}
```

---

## Step 3 — Koin module wiring

Include `supabaseModule` in your app's `startKoin` call. The module uses `platformModule()` internally to wire platform-specific credential storage and connectivity.

**Minimum wiring** (syncs favorites + chapters only):

```kotlin
startKoin {
    androidContext(this@App)
    modules(
        // ... your existing modules ...
        supabaseModule,
        module {
            // Required: ItemDao for favorites + chapters sync
            single { ItemDatabase.getInstance(...).itemDao() }
        }
    )
}
```

**Full wiring** (syncs all 8 tables):

```kotlin
startKoin {
    androidContext(this@App)
    modules(
        supabaseModule,
        module {
            single { ItemDatabase.getInstance(...).itemDao() }
            single { HistoryDatabase.getInstance(...).historyDao() }
            single { BookmarkDatabase.getInstance(...).bookmarkDao() }
            single { NotesDatabase.getInstance(...).notesDao() }
            single { CustomListDatabase.getInstance(...).listDao() }
            single { HeatMapDatabase.getInstance(...).heatMapDao() }
        }
    )
}
```

`SyncEngineImpl` resolves the optional DAOs via `getOrNull()` — tables whose DAOs are not registered are silently skipped during sync. Only `ItemDao` is required.

---

## Step 4 — Start the sync manager

Call `SyncManager.start()` once during app startup (e.g., in `Application.onCreate` or a root composable). Sync only runs when the user is authenticated and online.

```kotlin
// In Application.onCreate, after startKoin:
val syncManager: SyncManager by inject()
syncManager.start()
```

Or from a composable:

```kotlin
val syncManager = koinInject<SyncManager>()
LaunchedEffect(Unit) { syncManager.start() }
```

`SyncManager` monitors auth state and connectivity automatically. It will:
- Run a full sync immediately when the user signs in while online
- Poll every 5 minutes when offline
- Stop when the user signs out

---

## Step 5 — Wire navigation

Add the Supabase screens to your nav graph. The routes are in `SupabaseRoutes.kt`:

```kotlin
// Available route objects (all @Serializable NavKey):
SupabaseConfigRoute    // Enter project URL + anon key, test connection
AuthRoute              // Sign in / sign up / magic link
SyncStatusRoute        // View sync state, trigger manual sync
BackupRestoreRoute     // Backup to / restore from Supabase Storage
```

Example with Navigation3:

```kotlin
// In your entryGraph() or globalNav3Setup():
navEntry<SupabaseConfigRoute> { SupabaseConfigScreen() }
navEntry<AuthRoute> { AuthScreen() }
navEntry<SyncStatusRoute> { SyncStatusScreen() }
navEntry<BackupRestoreRoute> { BackupRestoreScreen() }
```

Add entry points in your Settings screen:

```kotlin
SettingsItem("Cloud Sync") { nav.navigate(AuthRoute) }
SettingsItem("Supabase Config") { nav.navigate(SupabaseConfigRoute) }
SettingsItem("Backup & Restore") { nav.navigate(BackupRestoreRoute) }
```

---

## Step 6 — First-time user flow

The intended UX flow for a new user:

1. **SupabaseConfigScreen** — user enters their Supabase project URL and anon key, taps "Test Connection". Credentials are saved to `EncryptedSharedPreferences` (Android) / AES-encrypted file (JVM) / `NSUserDefaults` (iOS). Credentials are **never hardcoded**.

2. **AuthScreen** — user signs in with email/password, creates an account, or requests a magic link.

3. **First sync** — `MigrationManager` detects existing local data and runs `SyncEngine.fullSync()` to upload it to Supabase. Subsequent syncs are incremental (dirty-only push, `since`-filtered pull).

---

## Step 7 — Soft-delete setup (optional but recommended)

The sync engine supports soft-deletes: instead of `@Delete`, mark records with `is_deleted=1, is_dirty=1` and the next push propagates the deletion to all devices.

New DAO methods are available on all databases:

```kotlin
// ItemDao
itemDao.softDeleteFavorite(url, Clock.System.now().toEpochMilliseconds())
itemDao.softDeleteChapter(url, Clock.System.now().toEpochMilliseconds())

// HistoryDao
historyDao.softDeleteHistory(searchText, timestamp)
historyDao.softDeleteRecentlyViewed(url, timestamp)

// BookmarkDao
bookmarkDao.softDeleteBookmark(chapterUrl, timestamp)

// NotesDao
notesDao.softDeleteNote(itemUrl, timestamp)

// ListDao
listDao.softDeleteCustomListItem(uuid, timestamp)
listDao.softDeleteCustomListInfo(uniqueId, timestamp)

// HeatMapDao
heatMapDao.softDeleteHeatMapItem(localDate, timestamp)
```

To fully enable cross-device deletes, route your app's delete actions through these methods instead of the existing `@Delete` methods. Until then, local deletes do not propagate to other devices, but remote deletes (from another device) are applied locally on the next pull.

---

## Step 8 — `updatedAt` timestamping (optional but recommended)

For correct conflict resolution, each local write should stamp `updated_at` with the current time. The sync engine does this automatically during push (it uses `Clock.System.now()` when `updatedAt == 0L`). For more precise conflict handling on multi-device edits, call the `markSynced` methods after writes, or intercept inserts at the repository layer to set `updatedAt` before Room persists the record.

---

## Step 9 — BackupWorker (Android only)

`BackupWorker` runs a one-off backup via `WorkManager`. Schedule it in your app:

```kotlin
val request = OneTimeWorkRequestBuilder<BackupWorker>().build()
WorkManager.getInstance(context).enqueue(request)
```

Backups are uploaded as JSON to `otakuworld-backups/<userId>/backup_<timestamp>.json` in Supabase Storage.

**Restore:** Use `BackupRestoreScreen` or call `RestoreManager.restore(backupJson)` directly. After restore completes, close Room and swap the staging file before restarting:

```kotlin
// RestoreManager writes to "$dbPath.restore"
// Then:
val dbFile = File(dbPath)
val stagingFile = File("$dbPath.restore")
if (stagingFile.exists()) {
    dbFile.delete()
    stagingFile.renameTo(dbFile)
}
// restart app
```

---

## Configuration

`SyncConfig` defaults:

| Parameter | Default | Description |
|---|---|---|
| `pollIntervalMs` | 5 min | How often to sync when offline |
| `maxRetries` | 5 | Retry attempts per sync cycle |
| `initialBackoffMs` | 1 s | First retry delay |
| `maxBackoffMs` | 30 s | Maximum retry delay (exponential cap) |

Override by providing a custom `SyncConfig` in your Koin module:

```kotlin
single { SyncConfig(pollIntervalMs = 10 * 60 * 1000L) }
```

---

## Architecture overview

```
Host App
  └── startKoin(supabaseModule + DAO modules)
        └── SyncManager.start()
              ├── Watches AuthState + ConnectivityMonitor
              ├── On authenticated + online: SyncEngine.fullSync()
              │     ├── pushLocalChanges() — sends is_dirty=1 rows to Supabase
              │     └── pullRemoteChanges() — fetches rows updated since last sync
              └── On offline: polls every pollIntervalMs

CredentialManager (platform)
  ├── Android: EncryptedSharedPreferences
  ├── JVM:     AES-encrypted file (~/.otakuworld/)
  └── iOS:     NSUserDefaults (plain — acceptable for anon key)

AuthManager
  └── Supabase Auth v3 (email/password, magic link, OAuth)
```

---

## Known limitations

| Limitation | Status |
|---|---|
| `recommendations` table sync | Not implemented — `genre: List<String>` ↔ Postgres `TEXT` JSON needs DTO bridge |
| `recently_viewed` sync | Not included in schema (by design) |
| `supabaseModule` not wired in host apps yet | Requires integration PR per app |
| Soft-delete not routed through existing delete calls | Host app must adopt `softDelete*()` DAO methods |
| JVM credential storage uses AES/ECB | Anon keys are semi-public; acceptable, but not production-grade secret storage |
| Realtime subscriptions | Not wired — polling-only sync currently |
