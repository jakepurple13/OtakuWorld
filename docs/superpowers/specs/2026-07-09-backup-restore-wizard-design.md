# Backup & Restore Wizard Overhaul — Design

**Date:** 2026-07-09
**Modules:** `:sharedcomponents` (new UI + interface), `:kmpuiviews` (Koin wiring, workers, existing processors), all consumer apps (unaffected — no new app-level registration)
**Scope:** Replace the flat "Create Full Backup" / "Restore Full Backup" pref rows in `MoreSettingsScreen.kt` with a multi-step wizard for selective backup and restore. Does not touch the separate Supabase cloud DB backup feature.

---

## Background

Today, backup/restore is two flat rows in `MoreSettingsScreen.kt`: tap "Create Full Backup" → pick a save location → `BackgroundWorkHandler.startBackup` enqueues `BackupWorker`, which resolves `getAll<BackupProcessor>()` (13 registered participants, Koin `factory` bind) and zips every one, no selection, no preview, no confirmation. Restore is the mirror image. `BackupProcessor` (`sharedtools/commonMain`) is the per-participant contract — `fileName`, `suspend backup(sink)`, `suspend restore(json, source)` — and **must not be modified**.

Two real platform gaps exist today and shape this design's scope:
- **iOS has no zip capability at all** — no multiplatform zip library in the catalog, no `Zipper` `iosMain` actual, no `BackgroundWorkHandlerImpl` `iosMain` actual.
- Everything else (Android execution via WorkManager, JVM/Desktop execution, FileKit-based file picking) already works and is reused as-is.

## Goals

1. New `BackupUiInfo` interface (`:sharedcomponents`) that each `BackupProcessor` additionally implements, exposing display metadata and two summary methods (live-state for backup, raw-content for restore).
2. Multi-step wizard (stepper UI) for both backup and restore: item selection with expandable per-item summaries → review/confirmation → execution with live per-item progress → completion.
3. Restore reads a zip's entries and summarizes them **without** fully restoring, so the user sees what's inside before committing.
4. Selective execution — only checked items are backed up or restored; failures are reported per-item, not as one all-or-nothing outcome.

## Out of Scope

- Scheduled/automatic backups.
- Cloud/remote backup destinations (Supabase DB backup/restore is a separate, pre-existing feature — untouched).
- Backup encryption/password protection.
- Cross-version backup format migration.
- Editing backup data before restore (view/select only).
- Backup file management (deleting/browsing prior backups).
- iOS zip execution (see Platform Scope below).

---

## Platform Scope

The wizard UI (Compose, `:sharedcomponents`, commonMain) renders identically on Android, Desktop (JVM), and iOS — no platform branching in the UI layer.

Actual backup/restore **execution** is scoped to Android and Desktop for v1, matching what already works:
- Android: existing `BackupWorker`/`RestoreWorker` (WorkManager) + `Zipper` Android actual — extended, not replaced.
- Desktop/JVM: existing JVM worker impl + `Zipper.jvm.kt` actual — extended the same way.
- iOS: wizard renders fully (selection, review) but the Review step's confirm action is gated — if the platform capability check fails, it shows "Not supported on this platform yet" instead of invoking `BackgroundWorkHandler`. Adding real iOS zip support (library evaluation + `iosMain` actuals) is explicit follow-up work, not part of this design.

---

## `BackupUiInfo` Interface

New file in `:sharedcomponents`, commonMain:

```kotlin
interface BackupUiInfo {
    val key: String              // must equal the pairing BackupProcessor.fileName
    val displayName: String
    val description: String?
    val icon: ImageVector?
    suspend fun currentSummary(): BackupDataSummary
    suspend fun parseSummary(json: String?, source: BufferedSource?): BackupDataSummary
}

data class BackupDataSummary(
    val itemCount: Int? = null,
    val sizeBytes: Long? = null,
    val lastModified: Instant? = null,
    val details: List<Pair<String, String>> = emptyList(),
)
```

- `currentSummary()` queries live state (DB row count via existing DAO/repository calls, or a description for preference bags / proto blobs where a count doesn't apply — `itemCount` stays `null` in that case).
- `parseSummary(json, source)` mirrors `BackupProcessor.restore`'s signature exactly, but only **reads** — used during the restore flow's peek pass over raw zip entry bytes, never writes anything.
- All 13 existing processors (`kmpuiviews/.../utils/backupproccesor/`, plus the per-app manga settings processor) are edited to additionally implement `BackupUiInfo`. `ImageVector` is already multiplatform-safe — no expect/actual needed.
- `BackupUiInfo` is optional by design (a future processor can skip it and fall back to a default display derived from `fileName`), even though all current processors implement it.

### Koin wiring

The existing `backupProcessor()` helper (`kmpuiviews/.../di/AppModule.kt`) is updated to conditionally bind `BackupUiInfo::class` alongside `BackupProcessor::class`, based on whether the reified type implements it:

```kotlin
inline fun <reified T : BackupProcessor> Module.backupProcessor(named: String, crossinline factoryBlock: () -> T) =
    factory(named(named)) { new(factoryBlock) }
        .bind(BackupProcessor::class)
        .apply { if (BackupUiInfo::class.isSuperclassOf(T::class)) bind(BackupUiInfo::class) }
```

The wizard's selection screen builds its checklist from `getAll<BackupUiInfo>()`. Execution resolves `getAll<BackupProcessor>()` separately and filters to selected `key == fileName`. Different Koin `factory` calls produce different instances, which is fine — processors are stateless with respect to their injected dependencies.

---

## Wizard Flow

### Backup: Select Items → Review → Executing → Complete
### Restore: Pick File → Select Items → Review → Executing → Complete

A shared `WizardStepper` composable renders the current step; each flow supplies its own step list/labels.

**Select Items** — `LazyColumn` of `BackupUiInfo` entries, each row: checkbox, icon, display name, item-count/size preview, expand chevron. Expanding lazily loads and caches the summary (`currentSummary()` for backup, `parseSummary()` against the already-opened zip for restore) via `AnimatedVisibility`. Header has a Select All / Deselect All toggle (derived from whether all items are currently selected — not separately stored state). Restore's flow prepends a **Pick File** step: single button via `rememberFilePickerLauncher(type = FileKitType.File("zip"))` (FileKit — already a `:sharedcomponents` dependency, reused as-is, no new abstraction); choosing a file opens the zip once via `ZipInputStream` in peek mode (iterate entries, call `parseSummary` per entry, close without restoring) to populate the checklist.

**Review** — flat list of only the selected items with their (by-now loaded) summaries, a total estimated size, and a single "Confirm Backup" / "Confirm Restore" button. If an item was never expanded during selection, its summary loads here before totals are shown.

**Executing** — per-item row transitions spinner → checkmark/error icon as results stream in.

**Complete** — final per-item success/fail list, reusing the same row shape; a failed item does not block others from completing.

### Execution model

WorkManager stays the execution mechanism (existing `BackupWorker`/`RestoreWorker`, unlike a plain in-process coroutine, survives process death/backgrounding — matches current architecture and the Desktop JVM impl that already extends it):

- Worker input gains a `List<String>` of selected keys (via `Data`), in addition to the existing file reference.
- The worker resolves `getAll<BackupProcessor>()`, filters to selected keys, iterates, and calls `setProgress` after each item with a serialized `ItemResult(key, displayName, success, error)`.
- The wizard ViewModel observes `WorkInfo.progress` and updates the Executing/Complete screens' per-item state as results stream in.
- Restore's real (non-peek) pass reopens the same zip file and calls `processor.restore(...)` only for selected keys.

### ViewModel state

```kotlin
data class WizardItemState(
    val uiInfo: BackupUiInfo,
    val summary: BackupDataSummary? = null,   // null until loaded
    val expanded: Boolean = false,
    val selected: Boolean = true,
)
data class ItemResult(val key: String, val displayName: String, val success: Boolean, val error: String? = null)
```

`BackupWizardViewModel` / `RestoreWizardViewModel` (new, `:kmpuiviews` or `:sharedcomponents`) each hold a `StateFlow` of current step + item list + result list. New nav3 `Screen` entries registered the same way existing screens are (`globalNav3Setup()` pattern).

---

## Testing

- `BackupUiInfo` implementations: `currentSummary()` against a fake/in-memory DB matches expected row count; `parseSummary()` against a known JSON fixture returns the expected count/details.
- ViewModel tests (backup + restore): selection toggling, select-all/deselect-all derived state, step transitions, Review only reflecting selected items, restore's Pick-File → Select-Items transition triggering the peek pass.
- `Zipper` (JVM): peek mode reads entries without invoking `restore`; a subsequent real pass still restores correctly.
- Koin helper: a processor implementing both interfaces resolves via both `getAll<BackupProcessor>()` and `getAll<BackupUiInfo>()`; one implementing only `BackupProcessor` is absent from the latter (fallback path).
- No Compose UI tests planned (not this project's convention).

---

## Open Follow-Up (explicitly not part of this design)

- iOS zip execution (library evaluation + `iosMain` actuals for `Zipper` and `BackgroundWorkHandlerImpl`).
- The existing `Zipper` reads every zip entry fully as UTF-8 text before dispatching to `restore`, even for binary/proto entries that ignore the `json` param — wasteful, pre-existing, not touched here.
