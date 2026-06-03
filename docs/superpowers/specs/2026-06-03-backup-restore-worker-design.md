# Backup/Restore Worker Design

**Date:** 2026-06-03  
**Branch:** feat/chapter-bookmarks  
**Status:** Approved

---

## Overview

Move `exportFullBackup` and `importFullBackup` in `MoreSettingsViewModel` off the
`viewModelScope` and onto Android WorkManager Workers. Workers show an in-progress
foreground notification and a completion/failure notification. No UI state feedback
needed — notifications only.

---

## Architecture

```
MoreSettingsViewModel
  └─ backgroundWorkHandler.startBackup(file) / startRestore(file)

BackgroundWorkHandler (commonMain interface)
  ├─ startBackup(file: PlatformFile)
  └─ startRestore(file: PlatformFile)

BackgroundWorkHandlerImpl (androidMain)
  ├─ startBackup → WorkManager.enqueueUniqueWork("backup", KEEP, BackupWorker)
  └─ startRestore → WorkManager.enqueueUniqueWork("restore", KEEP, RestoreWorker)

BackupWorker (androidMain CoroutineWorker)
  ├─ reads "uri" from inputData
  ├─ setForeground(indeterminate progress notification)
  ├─ backup.createBackup(platformFile)
  └─ posts complete / failure notification

RestoreWorker (androidMain CoroutineWorker)
  ├─ reads "uri" from inputData
  ├─ setForeground(indeterminate progress notification)
  ├─ backup.restoreBackup(platformFile)
  └─ posts complete / failure notification
```

---

## Components

### 1. `BackgroundWorkHandler` (commonMain)

Add two methods to the existing interface in
`kmpuiviews/src/commonMain/.../repository/WorkRepository.kt`:

```kotlin
fun startBackup(file: PlatformFile)
fun startRestore(file: PlatformFile)
```

`PlatformFile` is a KMP type (from FileKit) so it's valid in the commonMain interface.
JVM (`BackgroundWorkHandlerImpl.kt` in `jvmMain`): no-op stubs.

### 2. `BackgroundWorkHandlerImpl` (androidMain)

The Android impl extracts the `Uri` string internally using FileKit's `toAndroidUri`
extension, then passes it as `WorkData`. Workers receive a plain string.

```kotlin
override fun startBackup(file: PlatformFile) {
    workManager.enqueueUniqueWork(
        "backup",
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(workDataOf("uri" to file.toAndroidUri("").toString()))
            .build()
    )
}

override fun startRestore(file: PlatformFile) {
    workManager.enqueueUniqueWork(
        "restore",
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<RestoreWorker>()
            .setInputData(workDataOf("uri" to file.toAndroidUri("").toString()))
            .build()
    )
}
```

`ExistingWorkPolicy.KEEP` — if a backup/restore is already running, ignore the new request.

### 3. `BackupWorker` (androidMain)

File: `kmpuiviews/src/androidMain/.../workers/BackupWorker.kt`

- Koin-injected: `Backup`, `NotificationLogo`
- `doWork()`:
  1. Call `setForeground(getForegroundInfo())`
  2. Read `"uri"` from `inputData` — return `Result.failure()` if absent
  3. Reconstruct `PlatformFile` via `readPlatformFile(uri)` (already in `Platform.android.kt`)
  4. Call `backup.createBackup(file)`
  5. On success: post "Backup complete" notification, return `Result.success()`
  6. On exception: post "Backup failed" notification, log via `recordFirebaseException`, return `Result.failure()`
- `getForegroundInfo()`: indeterminate progress notification on `Backup` channel

### 4. `RestoreWorker` (androidMain)

File: `kmpuiviews/src/androidMain/.../workers/RestoreWorker.kt`

Same shape as `BackupWorker`, calls `backup.restoreBackup(file)` instead.

### 5. `MoreSettingsViewModel` (commonMain)

Replace coroutine-based implementations:

```kotlin
fun exportFullBackup(document: PlatformFile) {
    backgroundWorkHandler.startBackup(document)
}

fun importFullBackup(document: PlatformFile) {
    backgroundWorkHandler.startRestore(document)
}
```

### 6. Notification Channel

Add `Backup` entry to `NotificationChannels` enum in
`kmpuiviews/src/androidMain/.../utils/NotificationEnums.kt`:

```kotlin
Backup("backup_channel", NotificationManagerCompat.IMPORTANCE_LOW)
```

`IMPORTANCE_LOW` — silent, no heads-up for the ongoing foreground notification.
Completion/failure notifications also post on this channel (acceptable — backup
events are not time-critical).

### 7. Notification IDs

| Notification | ID |
|---|---|
| Backup in-progress | 200 |
| Restore in-progress | 201 |
| Backup complete/failed | 200 (replaces in-progress) |
| Restore complete/failed | 201 (replaces in-progress) |

Reusing the same ID per operation means the in-progress notification is replaced
by the completion notification automatically.

---

## Notification States

### In-progress (foreground service)
- Title: "Backing up…" / "Restoring…"
- Indeterminate progress bar
- `ongoing = true`, `onlyAlertOnce = true`
- Channel: `Backup` (IMPORTANCE_LOW — silent)

### Complete
- Title: "Backup complete" / "Restore complete"
- No progress bar
- `timeoutAfter = 3000L` (auto-dismiss after 3 s)
- Channel: `Backup`

### Failed
- Title: "Backup failed" / "Restore failed"
- No progress bar, no auto-dismiss
- Channel: `Backup`

---

## Worker Registration (Koin)

Workers use Koin's `workerOf` / `WorkerFactory`. Add `BackupWorker` and
`RestoreWorker` to the existing Koin worker module (same pattern as
`LocalToCloudSyncWorker`).

---

## Files Changed

| File | Change |
|---|---|
| `kmpuiviews/commonMain/.../repository/WorkRepository.kt` | Add `startBackup`, `startRestore` to interface |
| `kmpuiviews/androidMain/.../repository/BackgroundWorkHandlerImpl.kt` | Implement new methods |
| `kmpuiviews/jvmMain/.../repository/BackgroundWorkHandlerImpl.kt` | Add no-op stubs |
| `kmpuiviews/androidMain/.../workers/BackupWorker.kt` | New file |
| `kmpuiviews/androidMain/.../workers/RestoreWorker.kt` | New file |
| `kmpuiviews/androidMain/.../utils/NotificationEnums.kt` | Add `Backup` channel |
| `kmpuiviews/commonMain/.../moresettings/MoreSettingsViewModel.kt` | Replace backup/restore coroutines with handler calls |

---

## Out of Scope

- ViewModel progress state for backup/restore (notification only)
- Backup scheduling / periodic workers
- Desktop/JVM notification support for backup
