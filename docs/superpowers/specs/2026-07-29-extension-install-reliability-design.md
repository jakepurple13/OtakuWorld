# Reliable APK Extension Installation (PackageInstaller replacing Ackpine)

## Problem

Extension source APKs are downloaded via Ktor (works fine, kept as-is) and installed via
Ackpine (`ru.solrudev.ackpine:ackpine-core`/`ackpine-ktx`), which is unreliable in production.
This replaces the Ackpine install engine with Android's native
`android.content.pm.PackageInstaller` API, while preserving the existing download pipeline and
WorkManager-based orchestration.

Android-only. Desktop/JVM `actual` stays a no-op stub, as today.

## Out of scope

- Auto-update of already-installed extensions.
- Rewriting the Ktor download logic.
- Extension discovery/browsing UI, repository management.
- Uninstallation flow (unrelated `PackageUninstaller` usage is untouched).
- Sideloading from a file picker.
- Unit tests, README updates.
- Deleting the pre-existing, unused `DownloadWorker`/`InstallWorker` chained-worker experiment
  in `DownloadAndInstallWorker.kt` — left untouched per surgical-changes; it will keep
  compiling against the new engine unchanged.

## Architecture

Ackpine is removed from `kmpuiviews/src/androidMain/kotlin/.../utils/DownloadAndInstaller.kt`
(the Android `actual`) and from `kmpuiviews/build.gradle.kts` /
`gradle/android.versions.toml`. Everything **above** `DownloadAndInstaller` — the expect/actual
contract shape, `DownloadAndInstallWorker`, `DownloadStateRepository`,
`ExtensionListViewModel`, `DownloadStateScreen`, `PrereleaseScreen` — keeps working against the
same `Flow<DownloadAndInstallStatus>` contract. The unreliability was in the install engine, not
the WorkManager orchestration, notifications, or cancellation wiring already built around it, so
those are reused rather than replaced.

Three new Android-only pieces, all in `kmpuiviews/src/androidMain`:

1. **`PackageInstallEngine`** — wraps `android.content.pm.PackageInstaller` directly. Creates a
   session (`SessionParams(MODE_FULL_INSTALL)`), streams the APK file into it, commits with a
   `PendingIntent` (mutable — required API 31+) targeting the receiver below. Before creating a
   session, checks `packageManager.canRequestPackageInstalls()`.
2. **`PackageInstallReceiver`** — a manifest-declared `BroadcastReceiver`
   (`android:exported="false"`, since it is only ever triggered by our own explicit
   `PendingIntent`, never a public broadcast). Reads `PackageInstaller.EXTRA_STATUS`:
   - `STATUS_PENDING_USER_ACTION`: pulls `EXTRA_INTENT` and calls
     `startActivity(intent.addFlags(FLAG_ACTIVITY_NEW_TASK))` unconditionally. This mirrors
     Android's own documented `PackageInstaller` sample. It works regardless of whether the app
     is foregrounded, because the confirmation UI is the system installer's own task — this is
     what makes "app backgrounded during the install prompt" a non-issue rather than a special
     case to handle.
   - Any terminal status (`STATUS_SUCCESS` or a `STATUS_FAILURE_*` code): maps to a typed result
     (see State machine below), pushes it into `InstallStatusRepository`, deletes the temp APK
     file, and abandons the session's bookkeeping entry.
3. **`InstallStatusRepository`** — a Koin singleton holding
   `MutableStateFlow<Map<Int, DownloadAndInstallStatus>>` keyed by PackageInstaller session id,
   plus a `sessionId -> File` map for temp-file cleanup. The OS instantiates
   `PackageInstallReceiver` itself (no constructor injection available), so it reaches this
   repository via `KoinComponent.get()`. `DownloadAndInstaller.install()` commits the session via
   `PackageInstallEngine`, then collects `repository.flowFor(sessionId)` until a terminal state —
   this is what lets its return type stay `Flow<DownloadAndInstallStatus>`, unchanged from today.

## State machine

`DownloadAndInstallStatus` (`kmpuiviews/src/commonMain/.../utils/DownloadAndInstaller.kt`) is
extended additively. The sealed class is not annotated `@Serializable` incidentally — the
`Error` shape change and new cases require it to stay compatible with the existing
`WorkManager` progress serialization by class-name string matching in
`DownloadAndInstallWorker.listToDownloads` (see Migration note below).

```kotlin
sealed class DownloadAndInstallStatus {
    data class Downloading(val progress: Float) : DownloadAndInstallStatus()
    data object Downloaded : DownloadAndInstallStatus()
    data object Installing : DownloadAndInstallStatus()
    data object PendingUserAction : DownloadAndInstallStatus()      // NEW
    data object PermissionRequired : DownloadAndInstallStatus()     // NEW
    data object Installed : DownloadAndInstallStatus()
    data object Cancelled : DownloadAndInstallStatus()              // NEW
    data class Error(
        val reason: InstallErrorReason,                            // NEW field
        val message: String,
    ) : DownloadAndInstallStatus()
}

enum class InstallErrorReason {
    BLOCKED, CONFLICT, INCOMPATIBLE, INVALID, STORAGE, GENERIC, UNKNOWN,
}
```

`Cancelled` is kept distinct from `Error` so the UI can render it neutrally instead of as a
failure. `STATUS_FAILURE_ABORTED` (the code the system broadcasts when the user taps "Cancel" on
the install confirmation dialog) maps to `Cancelled`, not `Error` — declining the dialog isn't a
failure, it's a cancellation. `InstallErrorReason` therefore only covers the codes that represent
genuine failures, mapped 1:1 from the remaining `PackageInstaller.STATUS_FAILURE_*` int constants
in `PackageInstallReceiver`. There is no native `STATUS_FAILURE_TIMEOUT` or permission-denied
failure code (permission is handled separately via `PermissionRequired`, checked before a session
is even created) — both were part of Ackpine's constraint layer, not the native API, so they're
dropped.

Adding cases to this sealed class breaks exhaustive `when` blocks in `DownloadStateScreen.kt`
and `PrereleaseScreen.kt` — both must be updated to render the three new states. This is
intentional: the compiler enforces that every UI consumer accounts for the new states.

`ConfirmationType` (existing enum, `IMMEDIATE`/`DEFERRED`) stays in the `DownloadAndInstaller`
expect/actual signature as a no-op on Android: a normal (non-privileged, non-device-owner) app
installing via `PackageInstaller` always shows the system confirmation UI regardless of this
value — there is no silent-install capability to preserve. Keeping the parameter avoids touching
every call site (`DownloadAndInstallWorker`, `DownloadWorker`, `InstallWorker`,
`DownloadStateRepository`, `ExtensionListViewModel`, `PrereleaseViewModel`).

## Permission handling (`REQUEST_INSTALL_PACKAGES` / unknown-sources)

`PackageInstallEngine` checks `canRequestPackageInstalls()` before creating a session. If false,
it emits `PermissionRequired` and stops.

The screens that render `DownloadAndInstallStatus` (`DownloadStateScreen`, `PrereleaseScreen`)
are commonMain — shared with the JVM/desktop target — so they cannot call
`Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` or touch `android.content.Context` directly without
adding new expect/actual plumbing through the ViewModel → Screen → nav graph chain. Given this is
a rare edge case (hit at most once per device install, until the user revokes the permission),
that plumbing isn't justified: `PermissionRequired` renders as guidance text ("Enable install
from this source in Settings, then tap to retry") and reuses the existing tap-to-retry affordance
already in `DownloadStateScreen` (the card's `onClick` re-invokes `install()`), extended to also
fire from this state. The user grants the permission themselves via Settings and returns to
retry — no new retry mechanism, no cross-platform plumbing.

## Cancellation

`DownloadAndInstallWorker`'s coroutine cancellation (triggered by
`WorkManager.cancelWorkById`, called from `DownloadStateRepository.cancelDownload`) is caught in
a `finally` block:

- Cancelled before session commit: delete the temp APK file.
- Cancelled after commit, before a terminal receiver event: call
  `packageInstaller.abandonSession(sessionId)`, then delete the temp file.

`InstallStatusRepository`'s entry for that session is cleared in both cases. No orphaned
sessions or temp files should remain after a cancel.

## Retry

A failed or cancelled install has no persisted retry state — the caller (`ExtensionListViewModel`
/ `PrereleaseViewModel`) simply calls `downloadAndInstall(url, ...)` again, which re-downloads
and creates a fresh `PackageInstaller` session. No special-cased retry path is needed.

## Sequential queue (multiple extensions)

`DownloadAndInstallWorker.downloadAndInstall(context, url)` switches its enqueue call from
`WorkManager.enqueue(...)` to
`enqueueUniqueWork("downloadAndInstall", ExistingWorkPolicy.APPEND_OR_REPLACE, request)`.
WorkManager serializes execution under that unique work name — no new coordinator or mutex is
introduced.

## Manifest & DI wiring

- `kmpuiviews/src/androidMain/AndroidManifest.xml`: add
  `<receiver android:name=".workers.PackageInstallReceiver" android:exported="false" />`.
- `AppModule.android.kt`: register `InstallStatusRepository` via `singleOf`.
  `PackageInstallEngine` is constructed inside the `DownloadAndInstaller` actual and is not
  exposed separately — it has no other consumer.
- ProGuard: add a keep rule for `PackageInstallReceiver` (manifest-referenced components can be
  stripped/renamed by R8 without one); no existing ackpine/PackageInstaller rules exist to
  remove.
- Remove `androidLibs.ackpine.core` / `androidLibs.ackpine.ktx` from
  `kmpuiviews/build.gradle.kts`, and the `ackpine-core`/`ackpine-ktx`/`ackpineVersion` entries
  from `gradle/android.versions.toml`.
- `DownloadAndInstaller.uninstall(packageName)` currently uses ackpine's `PackageUninstaller`.
  Uninstallation UX redesign is explicitly out of scope, but the prompt mandates removing
  ackpine entirely, so this method is swapped to the minimal native equivalent —
  `startActivity(Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", packageName, null)))`
  — with no behavior change beyond dropping the dependency.

## Migration note

`DownloadAndInstallWorker.listToDownloads` matches on `DownloadAndInstallStatus`'s fully
qualified class name (string) to deserialize `WorkInfo.progress`. Adding new sealed subclasses
and changing `Error`'s shape needs corresponding new `when` branches there (for the new states)
and an update to the `Error` branch to also persist/restore `reason`. This is a small, mechanical
follow-on inside the same function that already does this today.

## Testing (manual — unit tests out of scope)

Verify by hand after implementation:

1. Install a new extension end-to-end (success path).
2. Force a failure (e.g. attempt to install over a differently-signed existing package) and
   confirm `Error(reason = CONFLICT, ...)` surfaces with a readable message.
3. Cancel mid-download and cancel mid-install (after commit, before the system dialog is
   resolved); confirm no crash, no orphaned session, temp file removed.
4. Toggle "install unknown apps" off for the app, attempt install, confirm `PermissionRequired`
   path launches Settings and retry succeeds after granting.
5. Trigger install while app is backgrounded; confirm the system confirmation dialog still
   appears.
6. Queue two extension installs back-to-back; confirm they run sequentially, not concurrently.
