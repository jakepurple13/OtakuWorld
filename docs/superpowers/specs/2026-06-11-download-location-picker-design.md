# Download Location Picker — Android Design Spec

**Date:** 2026-06-11  
**Branch:** feat/download-picker-android  
**Scope:** Android only (MangaWorld)

---

## Overview

Allow users to select and persist a custom root directory for manga chapter downloads. Default to `context.filesDir` when no custom path is set. Triggered from a new Android `PlatformSettings` screen mirroring the existing desktop `JvmSettingsScreen`.

---

## Out of Scope

- Cloud storage (Google Drive, Dropbox)
- Multiple simultaneous download locations
- Per-manga download locations
- Modifying download logic (retry, progress, image fetching)

---

## Section 1: Data Persistence

**Proto field** — `datastore/mangasettings/src/commonMain/proto/manga_settings.proto`:
```protobuf
string downloadPath = 12;  // empty string = use filesDir default
```

Empty string is the sentinel for "use default." No migration needed — existing installs have `""` and fall back to `filesDir` automatically.

**Settings accessor** — `MangaNewSettingsHandling`:
```kotlin
val downloadPath: ProtoStoreHandler<String>
```

**Path resolution** (applied at read time in the worker, not stored):
- `""` → `context.filesDir`
- non-empty → `Uri.parse(stored)` (content URI from SAF picker)

---

## Section 2: Download Manager Adaptation

**`DownloadChapterWorker`** receives `MangaNewSettingsHandling` via constructor injection:

```kotlin
class DownloadChapterWorker(
    context: Context,
    params: WorkerParameters,
    private val mangaSettings: MangaNewSettingsHandling,
) : CoroutineWorker(context, params)
```

Requires Koin's `KoinWorkerFactory` (or a custom factory) to supply the dependency. If the factory is not yet wired, it must be registered in the app's `WorkManager` configuration.

Path resolution in `doWork()`:
```kotlin
val storedPath = mangaSettings.downloadPath.get()
val rootDir = if (storedPath.isEmpty()) context.filesDir
              else DocumentFile.fromTreeUri(context, Uri.parse(storedPath))
```

Subdirectory creation under `rootDir` follows the existing `MangaWorld/{title}/{chapter}/` structure. When `rootDir` is a `DocumentFile` (content URI path), each level is created with `findFile(name) ?: createDirectory(name)` chained for `MangaWorld`, then `title`, then `chapter`.

`MangaDownloadManager` — no changes needed.

---

## Section 3: Navigation & Screen Registration

**New file:** `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/PlatformSettings.kt`

```kotlin
@Serializable
data object PlatformSettings : NavKey
```

**`GenericManga.settingsNav3Setup()`** — registers the screen:
```kotlin
override fun settingsNav3Setup() {
    super.settingsNav3Setup()
    navGraph.entry<PlatformSettings> { AndroidSettingsScreen() }
}
```

**`GenericManga.composeCustomPreferences()`** — adds the navigation entry point:
```kotlin
segmentedListItem(
    content = { Text("Platform Settings") },
    leadingContent = { Icon(Icons.Default.Android, null) },
    onClick = { navigationActions.navigate(PlatformSettings) }
)
```

---

## Section 4: UI — `AndroidSettingsScreen`

**Location:** `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/AndroidSettingsScreen.kt`

**Layout:**
- Screen title: "Platform Settings"
- Single settings group: **Download Location**
  - Label: "Download Location"
  - Subtitle: `"Default (Internal Storage)"` when path is empty; otherwise display name of the chosen directory
  - Trailing action: folder icon → triggers FileKit `rememberDirectoryPickerLauncher`
  - Secondary action: reset icon → clears stored path, reverts to `filesDir` default

**Directory pick handler:**
```kotlin
val launcher = rememberDirectoryPickerLauncher { dir ->
    dir?.let {
        context.contentResolver.takePersistableUriPermission(
            it.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        scope.launch { mangaSettings.downloadPath.set(it.uri.toString()) }
    }
}
```

**ViewModel (`AndroidSettingsViewModel`):**
- Injected via Koin
- Reads `mangaSettings.downloadPath` as `StateFlow<String>`
- Exposes `resetDownloadPath()` (sets path to `""`)
- No business logic — thin coordinator only

---

## Files Changed

| File | Change |
|------|--------|
| `datastore/mangasettings/.../manga_settings.proto` | Add `string downloadPath = 12` |
| `datastore/mangasettings/.../MangaNewSettingsHandling.kt` | Add `downloadPath` accessor |
| `mangaworld/shared/.../DownloadChapterWorker.kt` | Constructor inject `MangaNewSettingsHandling`, resolve root path |
| `mangaworld/src/.../GenericManga.kt` | Override `settingsNav3Setup()`, add menu item |
| `mangaworld/src/.../settings/PlatformSettings.kt` | New — `NavKey` data object |
| `mangaworld/src/.../settings/AndroidSettingsScreen.kt` | New — screen composable + ViewModel |
| `README.md` | Document download location feature |

---

## Dependencies

- **FileKit** `0.14.1` — already in `kmpuiviews/build.gradle.kts`, no new dependency
- **DocumentFile** (AndroidX) — standard Android library, no new dependency
- **Koin WorkManager** — must confirm `KoinWorkerFactory` is registered; add if missing
