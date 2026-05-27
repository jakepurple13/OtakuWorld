# Download Progress Notifications — Design Spec

**Date:** 2026-05-27  
**Scope:** MangaWorld chapter download notifications (Android + JVM Desktop)  
**Approach:** Inline platform-specific — modify two existing files only

---

## Overview

Show local notifications for manga chapter download progress. Android uses
`NotificationManagerCompat` directly inside `DownloadChapterWorker`. JVM Desktop uses
`TrayState.sendNotification()` via a state-transition observer added to
`MangaDownloadManager.jvm.kt`.

No new files. No UI changes. No tests.

---

## Files Changed

| File | Change |
|------|--------|
| `mangaworld/shared/src/androidMain/.../downloads/DownloadChapterWorker.kt` | Add notification lifecycle |
| `mangaworld/shared/src/jvmMain/.../downloads/MangaDownloadManager.jvm.kt` | Add `TrayState` param + observer |

---

## Android — `DownloadChapterWorker`

### Notification ID strategy

```kotlin
private val notifId = abs(chapterUrl.hashCode())
private val notifCompleteId = notifId + 100_000
private val notifFailId     = notifId + 200_000
```

`abs(chapterUrl.hashCode())` is stable and unique per chapter URL. Offset constants separate
the ongoing progress notification from the terminal result notification so both can coexist
briefly during the cancel→post transition.

### Notification channel

Use the pre-existing `NotificationChannels.Download` channel (`"download_channel"`). No new
channel registration needed.

### Flow inside `doWork()`

```
doWork() called
  └─ POST progress notification (indeterminate, ongoing = true, onlyAlertOnce = true)
     └─ onProgress(done=0, total=N) → UPDATE same notification ID (determinate 0/N)
     └─ onProgress(done=k, total=N) → UPDATE same notification ID (determinate k/N)
     └─ onProgress(done=N, total=N) → UPDATE same notification ID (determinate N/N)
  └─ SUCCESS
       cancel(notifId)                 // dismiss progress bar
       notify(notifCompleteId, ...)    // "Downloaded — Chapter X" (autoCancel = true)
  └─ FAILURE (after all retries)
       cancel(notifId)
       notify(notifFailId, ...)        // "Download Failed — Chapter X: <reason>" (autoCancel = true)
```

### Notification shape

**Progress (ongoing):**
- Title: manga title
- Text: chapter name
- Progress bar: determinate `done/total` (indeterminate until first `onProgress`)
- `setOngoing(true)` — not dismissable by user during download
- `setOnlyAlertOnce(true)` — no repeated sounds on progress updates

**Complete:**
- Title: `"Downloaded"`
- Text: `"$mangaTitle — $chapterName"`
- `setAutoCancel(true)`
- No progress bar

**Failed:**
- Title: `"Download Failed"`
- Text: `"$chapterName: $reason"`
- `setAutoCancel(true)`

### Icon

Use `android.R.drawable.stat_sys_download` (progress) and
`android.R.drawable.stat_sys_download_done` (complete/failed) — standard system icons,
available without app-level drawable dependencies.

### Permission handling

`NotificationManagerCompat` requires `POST_NOTIFICATIONS` on Android 13+ (API 33+). Guard
the `notify()` calls with:

```kotlin
if (
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED
) {
    notificationManager.notify(...)
}
```

The app is responsible for requesting the permission at runtime (existing behavior, not
changed here).

---

## JVM — `MangaDownloadManager.jvm.kt`

### Constructor change

Add `trayState: TrayState` parameter (already a Koin singleton registered in
`koinInject<TrayState>()` in `DesktopUi.kt`).

```kotlin
actual class MangaDownloadManager(
    private val scope: CoroutineScope,
    mangaDesktopSettings: MangaDesktopSettings,
    private val trayState: TrayState,          // new
)
```

### State-transition observer

Add a third `init` block (after the existing two) that collects `_downloads` and diffs
against the previous snapshot to detect transitions:

```kotlin
init {
    var previousStates = emptyMap<String, DownloadState>()
    _downloads
        .onEach { list ->
            val current = list.associateBy { it.chapterUrl }
            list.forEach { progress ->
                val prev = previousStates[progress.chapterUrl]
                when {
                    // New entry — download started
                    prev == null ->
                        trayState.sendNotification(
                            title   = "Downloading",
                            message = "${progress.mangaTitle} — ${progress.chapterName}",
                        )
                    // Became complete
                    prev !is DownloadState.Completed && progress.state is DownloadState.Completed ->
                        trayState.sendNotification(
                            title   = "Downloaded",
                            message = "${progress.mangaTitle} — ${progress.chapterName}",
                        )
                    // Became failed
                    prev !is DownloadState.Failed && progress.state is DownloadState.Failed ->
                        trayState.sendNotification(
                            title   = "Download Failed",
                            message = "${progress.chapterName}: ${progress.state.reason}",
                        )
                }
            }
            previousStates = current.mapValues { it.value.state }
        }
        .launchIn(scope)
}
```

### Notifications sent

| Trigger | Title | Message |
|---------|-------|---------|
| New entry in `_downloads` | `"Downloading"` | `"$mangaTitle — $chapterName"` |
| State → `Completed` | `"Downloaded"` | `"$mangaTitle — $chapterName"` |
| State → `Failed` | `"Download Failed"` | `"$chapterName: $reason"` |
| State → `Cancelled` | *(none)* | User-initiated, silent |

No per-image progress updates — tray balloons are not suited for rapid-fire updates.

### TrayState API

`TrayState.sendNotification(title: String, message: String)` — maps to
`java.awt.TrayIcon.displayMessage()`. Available in Compose Multiplatform Desktop without
additional dependencies.

---

## Out of Scope

- UI changes to any existing screen
- Unit tests
- expect/actual notification abstraction
- Notification permission request flow (runtime permission UI)
- AnimeWorld / NovelWorld downloads
