# Backup & Restore Improvements — Per-Row Error Handling & Selective List Picker — Design

**Date:** 2026-07-29
**Modules:** `kmpuiviews` (backup processors, wizard ViewModels/UI, `Zipper`), `favoritesdatabase` (no schema change — reads existing `useBiometric` field)
**Scope:** Extends the wizard built in [2026-07-09-backup-restore-wizard-design.md](2026-07-09-backup-restore-wizard-design.md). Two independent improvements: (1) push failure isolation from processor-granularity down to per-row, (2) let the user pick individual custom lists to include when the "Custom Lists" category is selected, in both backup and restore.

---

## Background

Today, `Zipper.kt` already wraps each `BackupProcessor` call in `runCatching`, producing one `ItemResult(key, success, timeTaken, error)` per processor — a failing processor is skipped and reported, others continue. `BackupWizardScreen`/`RestoreWizardScreen`'s Complete step already lists these results. This is processor-granularity, not row-granularity: inside a processor's own loop (e.g. `ListBackupProcessor.restore()` iterating `CustomList` rows), one bad row throws and fails that processor's *entire* entry, even if the other rows were fine.

Selection today (`WizardItemState`, `BackupWizardViewModel`/`RestoreWizardViewModel`) is also processor-granularity only — checking "Custom Lists" includes or excludes *all* lists as one unit. There's no way to back up or restore a subset of lists.

## Goals

1. Processors whose `backup()`/`restore()` loop over multiple rows isolate failures per row instead of per processor call, and report a partial-success summary through the existing `ItemResult`.
2. The wizard's "Custom Lists" row, when expanded, shows a checklist of individual lists (cover thumbnail, name, item count, biometric indicator) that the user can check/uncheck to narrow what's included — in both the backup wizard and the restore wizard.
3. A selective-list zip is byte-for-byte the same format as a full backup (just a filtered `lists.json` entry) — no new zip structure, no restore-side changes needed to read it.

## Out of Scope

- Cloud/Supabase list sync — unaffected, separate feature.
- Any standalone "share a single list" entry point outside the wizard (considered, dropped — wizard reuse covers this need).
- Per-row failure isolation for `NewSettingsBackupProcessor` — it's a single Wire-proto blob, not a row loop; processor-level catch (existing behavior) already fits it.
- Biometric enforcement during export/import — the lock icon in the checklist is informational only, derived from existing per-item `useBiometric`, not a new list-level field, and does not gate anything.
- Restore-side conflict resolution (list already exists on device) — out of scope, same as the original wizard design.
- Sub-item selection for any category other than Custom Lists (e.g. picking individual favorites/history rows) — not requested, no hook added for it.

---

## Part 1 — Per-Row Failure Isolation

Processors with row loops (`ListBackupProcessor`, `FavoriteBackupProcessor`, `HistoryBackupProcessor`, `ChaptersWatchedBackupProcessor`, `BookmarksBackupProcessor`, `NotesBackupProcessor`, `HeatMapBackupProcessor`, `ActivityBackupProcessor`, `RecommendationsBackupProcessor`, `NotificationsBackupProcessor`, `DictionaryBackupProcessor`, `SourceOrderBackupProcessor`) wrap each row in try/catch inside the loop and return a result instead of `Unit`:

```kotlin
data class ProcessorResult(val successCount: Int, val failed: List<String>)
```

`Zipper.kt`'s existing per-processor `runCatching` block (backup at ~L48-58, restore at ~L100-108) is unchanged in structure — it still catches a processor throwing entirely — but on success it now reads the returned `ProcessorResult` and builds the `ItemResult`:

```kotlin
ItemResult(
    key = processor.key,
    success = result.successCount > 0,
    timeTaken = ...,
    error = result.failed.takeIf { it.isNotEmpty() }
        ?.let { "${it.size} failed: ${it.joinToString()}" },
)
```

No new UI — the Complete step already renders `result.error` as-is.

## Part 2 — Selective List Picker (Backup & Restore)

### Data model

```kotlin
data class ListSubItemState(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val itemCount: Int,
    val requiresBiometric: Boolean, // list.list.any { it.useBiometric } — display only
    val selected: Boolean = true,
)
```

`WizardItemState` gains `val subItems: List<ListSubItemState>? = null`. Populated only for the `"lists.json"` key; every other category leaves it `null`.

### Backup direction

- `BackupWizardViewModel.loadSummaryIfNeeded("lists.json")` additionally loads `listDao.getAllListsSync()` and maps each `CustomList` to a `ListSubItemState` (from the local DB — this is what would be backed up).
- New `toggleListSelected(listId)` flips one entry's `selected`.
- `confirm()` computes `selectedListIds = subItems?.filter { it.selected }?.map { it.id }?.toSet()` (`null` if the row was never expanded/touched → behaves exactly like today, full list export). Passed through the existing `startBackup(file, keys, ...)` call as an added `selectedListIds` param, threaded through `BackgroundWorkHandler` → `BackupWorker` → `Backup.createBackup` → `Zipper.zipFile`.

### Restore direction

- `RestoreWizardViewModel.loadSummaryIfNeeded("lists.json")` additionally parses the *peeked* zip's raw `lists.json` content (`json.fromJson<List<CustomList>>()` — the same call `ListBackupProcessor` already uses) into `subItems` — this reflects what's *in the zip*, not the local DB.
- Same `toggleListSelected`/`confirm()` shape, passed through `startRestore(file, keys, selectedListIds)` → `RestoreWorker` → `Backup.restoreBackup` → `Zipper.readZip`.

### Shared UI

`WizardItemRow`'s expand block (`AnimatedVisibility`) branches on `item.subItems`: non-null renders the checklist (`ImageLoaderChoice` cover thumbnail, name, "$itemCount items" subtitle, lock icon if `requiresBiometric`, trailing checkbox); `null` keeps today's plain `summary.details` text rows. No backup-vs-restore branching needed in the row composable itself.

### Filter plumbing into `ListBackupProcessor`

Rather than changing the `BackupProcessor` interface for all 12 processors, `ListBackupProcessor` gets one settable field, used by both directions (backup and restore are never in flight concurrently on the same processor instance):

```kotlin
var listIdFilter: Set<String>? = null
```

`Zipper.zipFile`/`Zipper.readZip` set it immediately before calling `processor.backup(sink)` / `processor.restore(json, source)` and reset it to `null` in a `finally`:

```kotlin
try {
    if (processor is ListBackupProcessor) processor.listIdFilter = selectedListIds
    processor.backup(sink) // or restore(...)
} finally {
    if (processor is ListBackupProcessor) processor.listIdFilter = null
}
```

`ListBackupProcessor.backup()` filters `listDao.getAllListsSync()` by `listIdFilter` (when non-null) before serializing. `ListBackupProcessor.restore()` filters the parsed `List<CustomList>` by `listIdFilter` before the `forEach { listRepository.createList(...) }` loop — this is also where Part 1's per-row try/catch lives, so a filtered-out list is simply absent, and a present-but-bad list is caught and reported without aborting the rest.

No other processor, the `BackupProcessor` interface, or the zip file format changes.

---

## Testing

- **Per-row isolation:** mixed success/failure row sets for at least `ListBackupProcessor` produce the correct `ProcessorResult(successCount, failed)` and resulting `ItemResult.error` text; an all-success case has `error = null`; an all-failure case has `success = false`.
- **`ListBackupProcessor` filtering:** `listIdFilter = null` → all lists included (unchanged behavior); a subset filter includes only matching lists on both `backup()` and `restore()`.
- **`BackupWizardViewModel`/`RestoreWizardViewModel`:** `toggleListSelected` flips the right entry; `confirm()` emits `selectedListIds = null` when the row was never touched, and the correct filtered set otherwise; restore-side `subItems` are built from parsed zip content, not the local DB.
- **`Zipper`:** `listIdFilter` is reset after each backup/restore call — a full backup run after a selective one is not affected by leftover state.
