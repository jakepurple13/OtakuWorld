# Details Chapter Multi-Select Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Long-press chapter rows on the Details screen to multi-select them and act on them from a floating toolbar, replacing the Mark As drawer, the batch download sheet, and the long-press options sheet.

**Architecture:** Selection state (URL set) lives in `DetailsViewModel`, mirroring `NotificationScreenViewModel`. A pure function derives the toolbar's UI state from the selection; one shared composable (`ChapterSelectionToolbarHost`) renders the toolbar, runs actions, and shows snackbars, and both portrait and landscape layouts host it. `ChapterItem` gains a selection mode.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material3 (expressive: `HorizontalFloatingToolbar`), Koin, Room, kotlin.test (jvmTest).

**Spec:** `docs/superpowers/specs/2026-10-28-details-chapter-multiselect-design.md` (approved preview: https://claude.ai/artifact/5ahBLUFTWKa393Q5VL5WF2)

## Global Constraints

- All new code in `kmpuiviews/src/commonMain` (targets Android, JVM, iOS); only APIs `NotificationScreen.kt` already uses.
- Enter/exit specs copied from `NotificationScreen.kt`: top bar `AnimatedContent` transitionSpec; toolbar `slideInVertically { it * 2 } + fadeIn() + scaleIn()` / `slideOutVertically { it * 2 } + fadeOut() + scaleOut()`, padded `LocalNavHostPadding.current`, offset `y = -FloatingToolbarDefaults.ScreenOffset`.
- Selection bar title uses `Res.string.selected_count`; close icon uses `Res.string.clear_selection`; container `surfaceContainerHigh`.
- Undo snackbar ONLY for mark read/unread and bookmark/remove bookmark. Download/Delete: plain snackbar, no Undo. Read/Share: no snackbar.
- Download button: hidden unless `info.source.canDownload`; with 2+ selected also hidden unless `genericInfo.supportsBatchDownload`.
- Only `GenericInfo` API change allowed: `val supportsBatchDownload: Boolean get() = false` on `KmpGenericInfo`, overridden `true` in `GenericSharedManga`.
- Local builds use `noFirebase` variants.
- Copy (hardcoded English, matching existing Details strings): "Read", "Share", "Mark read", "Mark unread", "Bookmark", "Remove bookmark", "Download", "Delete download", "Select all", "Invert selection", "Undo". Snackbars: "Marked N chapters read" / "Marked N chapters unread" / "Bookmarked N chapters" / "Removed bookmark from N chapters" / "Downloading N chapters" / "Deleted N chapters" — "chapter" when N == 1.

## Review Focus

1. **Mixed bookmark selection** — some bookmarked, some not → Bookmark sets ALL bookmarked (never per-chapter toggle). Test in Task 1.
2. **Download with some already downloaded/queued** — only chapters whose state is `None` (or absent) are sent to `batchDownloadChapters`; snackbar count = that subset. Test in Task 2.
3. **Undo restores each chapter's own prior state**, not the inverse of the action (a chapter already read before "Mark read" stays read after Undo). Test in Task 1.
4. **App without batch support, selection grows 1 → 2** — Download hides, and Delete-all-downloaded with 2+ also hides. Test in Task 2.
5. **Single-select toolbar width on a 360dp phone** (7 controls) — must not clip. Checked on device in Task 6.

---

### Task 1: Selection + bulk state in DetailsViewModel

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModelTest.kt`

**Interfaces:**
- Produces (on `DetailsViewModel`):
  - `val selectedChapterUrls: SnapshotStateSet<String>` (`mutableStateSetOf()`)
  - `val isSelecting: Boolean` (derivedStateOf `selectedChapterUrls.isNotEmpty()`)
  - `fun toggleSelection(url: String)`
  - `fun selectAllChapters()` — all URLs of `(detailState as? DetailState.Success)?.info?.chapters`
  - `fun invertSelection()`
  - `fun clearSelection()`
  - `fun markChaptersAs(chapters: List<KmpChapterModel>, read: Boolean)` — calls `markAs` per chapter
  - `fun restoreReadState(chapters: List<KmpChapterModel>, previouslyRead: Set<String>)` — `markAs(c, c.url in previouslyRead)`
  - `fun setBookmarked(chapters: List<KmpChapterModel>, bookmarked: Boolean)` — explicit insert/delete via `bookmarkRepository`, same `BookmarkedChapter` fields `toggleBookmark` uses; skip chapters already in target state
  - `fun restoreBookmarkState(chapters: List<KmpChapterModel>, previouslyBookmarked: Set<String>)`

- [ ] **Step 1: Write failing tests** in `DetailsViewModelTest` (ExampleService gives 10 chapters, URLs `https://example0.com/`…`https://example9.com/`; use existing `viewModel()` + `awaitCondition`):
  - `selection toggle selectAll invert and clear` — toggle ch[0] → `isSelecting` true, set == {ch0}; toggle ch[0] again → empty, `isSelecting` false; toggle ch[0], `selectAllChapters()` → size 10; `clearSelection()`; toggle ch[0], ch[1], `invertSelection()` → size 8 and contains neither; `selectAllChapters()`, `invertSelection()` → empty.
  - `setBookmarked true on mixed selection bookmarks all` — `toggleBookmark(ch0)`, await; `setBookmarked(listOf(ch0, ch1, ch2), true)` → await all three in `bookmarkedChapterUrls`; then `setBookmarked(same, false)` → await none.
  - `restoreReadState restores per-chapter prior state` — `markAs(ch0, true)`, await; `markChaptersAs(listOf(ch0, ch1), true)`, await both in `chapters`; `restoreReadState(listOf(ch0, ch1), setOf(ch0.url))` → await `chapters` contains ch0 and not ch1.
  - `restoreBookmarkState restores per-chapter prior state` — same shape with bookmarks.

- [ ] **Step 2: Run, expect FAIL (unresolved references)**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.details.DetailsViewModelTest"`

- [ ] **Step 3: Implement the Interfaces above in `DetailsViewModel.kt`.**

- [ ] **Step 4: Run same command, expect PASS (all tests, including existing ones).**

- [ ] **Step 5: Commit** — `feat(details): chapter selection and bulk read/bookmark in DetailsViewModel`

---

### Task 2: Toolbar UI state + batch-download flag

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/ChapterSelection.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/GenericInfo.kt` (in `KmpGenericInfo`, next to `batchDownloadChapters`)
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/GenericSharedManga.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/details/ChapterSelectionStateTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class SelectionDownloadMode { Hidden, Download, Delete }

  data class ChapterSelectionUiState(
      val count: Int,
      val showSingleActions: Boolean,      // count == 1
      val allRead: Boolean,                // every selected is read → "Mark unread"
      val allBookmarked: Boolean,          // → "Remove bookmark"
      val downloadMode: SelectionDownloadMode,
      val chaptersToDownload: List<KmpChapterModel>, // selected with state None/absent
      val selectAllEnabled: Boolean,       // count < total
  )

  fun chapterSelectionUiState(
      selected: List<KmpChapterModel>,
      totalChapters: Int,
      readUrls: Set<String>,
      bookmarkedUrls: Set<String>,
      downloadStates: Map<String, ChapterDownloadUiState>,
      canDownload: Boolean,
      supportsBatchDownload: Boolean,
  ): ChapterSelectionUiState
  ```
  - `KmpGenericInfo.supportsBatchDownload: Boolean get() = false`; `GenericSharedManga` overrides `true`.
- downloadMode rule: `!canDownload` → Hidden; `count >= 2 && !supportsBatchDownload` → Hidden; all selected `Downloaded` → Delete; else Download.

- [ ] **Step 1: Write failing tests** (build chapters with `ExampleService` chapter shape or `KmpChapterModel(name, url, uploaded, sourceUrl, source = ExampleService())`):
  - `single selection shows single actions` — 1 of 10 → `showSingleActions` true, `selectAllEnabled` true.
  - `all read flips to unread only when every selected is read` — 2 selected, readUrls has 1 → `allRead` false; has both → true.
  - `mixed bookmarks is not allBookmarked`.
  - `download hidden when source cannot download` — canDownload false → Hidden.
  - `batch unsupported hides download for two or more` — supportsBatch false: count 1 → Download; count 2 → Hidden; count 2 all Downloaded → Hidden.
  - `all downloaded shows delete` — supportsBatch true, both Downloaded → Delete.
  - `chaptersToDownload skips downloaded and queued` — states {a: Downloaded, b: Queued, c: None, d absent} → [c, d], mode Download.
  - `selectAll disabled when everything selected` — 10 of 10 → false.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.details.ChapterSelectionStateTest"`

- [ ] **Step 3: Implement `chapterSelectionUiState`, the data types, and the `supportsBatchDownload` flag + manga override.**

- [ ] **Step 4: Run same command, expect PASS.**

- [ ] **Step 5: Commit** — `feat(details): chapter selection toolbar state and supportsBatchDownload flag`

---

### Task 3: Remove Mark As drawer, batch download sheet, long-press options sheet

**Files:**
- Modify: `.../presentation/details/DetailsScreen.kt` (delete `MarkAsScreen`, `BatchDownloads`, `chapterItemOptions`, `showBatchDownloads`, `DetailsActions.batchDownload`; `ChapterItem` long-press temporarily a no-op — Task 4 replaces it)
- Modify: `.../presentation/details/DetailsPortrait.kt` (remove `ModalNavigationDrawer`/`ModalDrawerSheet`, `rememberDrawerState`, drawer `BackHandler`, `markAsClick`)
- Modify: `.../presentation/details/DetailsLandscape.kt` (same; drop `scaffoldState` param from `DetailsLandscapeContent`)
- Modify: `.../presentation/details/DetailsUtils.kt` (remove "Mark As" + "Batch Download" `DropdownMenuItem`s, "Batch Download" `FloatingActionButtonMenuItem`, "Mark As..." `clickableItem`; remove params `scaffoldState`, `onBatchDownloadClick`, `markAsClick`)

(`.../presentation/details/` = `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/`)

**Interfaces:**
- Produces: `DetailActions(...)`, `DetailFloatingActionButtonMenu(...)`, `ChapterListHeader(...)` without the removed params. Remove imports these deletions orphan. Keep `Res.string.markAs` (still used in `TipDemos.kt`, `GeneralContent.kt`).

- [ ] **Step 1: Make the removals.**
- [ ] **Step 2: Verify no references remain**

Run: `grep -rn "MarkAsScreen\|BatchDownloads\|chapterItemOptions\|onBatchDownloadClick\|markAsClick\|batchDownload =" kmpuiviews/src`
Expected: no output.

- [ ] **Step 3: Compile + existing tests**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :kmpuiviews:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit** — `refactor(details): remove Mark As drawer, batch download sheet, chapter options sheet`

---

### Task 4: ChapterItem selection mode + toolbar host composables

**Files:**
- Modify: `.../presentation/details/DetailsScreen.kt` (`ChapterItem`)
- Modify: `.../presentation/details/ChapterSelection.kt` (add composables)

**Interfaces:**
- Consumes: Task 1 VM API, Task 2 `chapterSelectionUiState`.
- Produces:
  - `ChapterItem(..., selectionMode: Boolean, isSelected: Boolean, onToggleSelection: () -> Unit, ...)`:
    - `onLongClick`: `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` then `onToggleSelection()`.
    - `onClick`: `if (selectionMode) onToggleSelection() else detailsActions.onClick(c)`.
    - Swipe: both `enableDismissFrom*` false when `selectionMode`.
    - Trailing bookmark + share `IconButton`s inside `AnimatedVisibility(!selectionMode)`; download-state indicator always shown.
    - Leading: `AnimatedVisibility(selectionMode)` selection indicator (outlined circle; filled primary circle with `Icons.Default.Check` when selected, check `scaleIn`), unread dot kept beside it, smaller.
    - `ListItem(selected = if (selectionMode) isSelected else !updatedIsRead)`; container color `animateColorAsState` → `primaryContainer` when selected.
  - ```kotlin
    data class ChapterSelectionActions(
        val selectedUrls: Set<String>,
        val onToggle: (String) -> Unit,
        val onSelectAll: () -> Unit,
        val onInvert: () -> Unit,
        val onClear: () -> Unit,
        val onMarkAs: (List<KmpChapterModel>, Boolean) -> Unit,
        val onRestoreRead: (List<KmpChapterModel>, Set<String>) -> Unit,
        val onSetBookmarked: (List<KmpChapterModel>, Boolean) -> Unit,
        val onRestoreBookmarks: (List<KmpChapterModel>, Set<String>) -> Unit,
        val onBatchDownload: (List<KmpChapterModel>) -> Unit,
        val supportsBatchDownload: Boolean,
    )
    ```
    Added to `DetailsActions` as `val selection: ChapterSelectionActions`.
  - `@Composable fun ChapterSelectionTopBar(count: Int, onClear: () -> Unit, scrollBehavior: TopAppBarScrollBehavior?)` — `AnimatedContent(count)` vertical roll on the number (up when growing).
  - `@Composable fun ChapterSelectionToolbarHost(visible: Boolean, chapters: List<KmpChapterModel>, read: List<ChapterWatched>, detailsActions: DetailsActions, downloadStates: Map<String, ChapterDownloadUiState>, canDownload: Boolean, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier)`:
    - `AnimatedVisibility(visible)` with Global Constraints specs, wrapping `HorizontalFloatingToolbar(expanded = true)`.
    - Order: [single-only `AnimatedVisibility(expandHorizontally+fadeIn / shrinkHorizontally+fadeOut)`: filled "Read" button (`Icons.Default.PlayArrow`) → `detailsActions.onClick`, Share → `detailsActions.shareChapter`, `VerticalDivider`] · Mark read/unread (`DoneAll`/`RemoveDone`, icon via `AnimatedContent`) · Bookmark (`Outlined.BookmarkBorder`/`BookmarkRemove`) · Download/Delete in `AnimatedVisibility(mode != Hidden)` (`Download`/`Delete`) · `VerticalDivider` · Select all (`SelectAll`, `enabled = selectAllEnabled`) · Invert (`Flip`).
    - Content descriptions = Global Constraints copy for current state.
    - Action handlers: capture `previouslyRead`/`previouslyBookmarked` URL sets, run action, `onClear()`, then `showSnackbar(message, actionLabel = "Undo")` → on `SnackbarResult.ActionPerformed` call restore. Download: count 1 → `detailsActions.onDownload(c)`; else `onBatchDownload(chaptersToDownload)`. Delete: `detailsActions.onDeleteDownload` per chapter. Plain snackbars for those. Select all/Invert don't clear.
  - Icon availability: if `Icons.Default.Flip`/`RemoveDone`/`BookmarkRemove` aren't in the icon set the module uses, pick the closest existing extended icon and note it in the commit.

- [ ] **Step 1: Implement `ChapterItem` changes and the composables.**
- [ ] **Step 2: Compile**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (callers updated in Task 5 may need default args: give `selectionMode = false, isSelected = false, onToggleSelection = {}` defaults so this compiles alone).

- [ ] **Step 3: Commit** — `feat(details): chapter selection mode and floating selection toolbar`

---

### Task 5: Wire portrait and landscape

**Files:**
- Modify: `.../presentation/details/DetailsScreen.kt` (build `ChapterSelectionActions` from `details` VM + `genericInfo`; pass `isSelecting`)
- Modify: `.../presentation/details/DetailsPortrait.kt`
- Modify: `.../presentation/details/DetailsLandscape.kt`

**Interfaces:**
- Consumes: Tasks 1, 2, 4.
- `ChapterSelectionActions` built in `DetailsScreen`: `selectedUrls = details.selectedChapterUrls`, `onBatchDownload = { genericInfo.batchDownloadChapters(it, infoModel) }`, `supportsBatchDownload = genericInfo.supportsBatchDownload`, rest map 1:1 to Task 1 VM functions.

Portrait (`DetailsView`):
- `BackHandler(isSelecting) { selection.onClear() }`.
- `topBar` = `AnimatedContent(isSelecting)` with NotificationScreen's transitionSpec: `ChapterSelectionTopBar` vs existing `TopAppBar`.
- `DetailFloatingActionButtonMenu(isVisible = fabVisible && !isSelecting, ...)`.
- `ChapterSelectionToolbarHost` aligned `BottomCenter` in the scaffold content `Box`, using existing `hostState`, `canDownload = info.source.canDownload`.
- `ChapterItem` gets `selectionMode = isSelecting`, `isSelected = c.url in selection.selectedUrls`, `onToggleSelection = { selection.onToggle(c.url) }`.
- `LazyColumn` contentPadding adds bottom `88.dp` (toolbar 64 + 24 gap) while `isSelecting`.

Landscape (`DetailsViewLandscape` / `DetailsLandscapeContent`):
- Same `BackHandler`, top-bar `AnimatedContent`, `ChapterItem` args.
- `detailPane`: wrap `LazyColumn` in `Box`; inside it, `ChapterSelectionToolbarHost(modifier = Modifier.align(Alignment.BottomCenter))` and a pane-local `SnackbarHost(paneSnackbarState, Modifier.align(BottomCenter).padding(bottom = 88.dp))`; pass `paneSnackbarState` to the host. Toolbar here does NOT add `LocalNavHostPadding` (pane has none); keep the `ScreenOffset`.
- Left pane unchanged.

- [ ] **Step 1: Wire portrait.**
- [ ] **Step 2: Wire landscape.**
- [ ] **Step 3: Build all affected apps**

Run: `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm`
Expected: BUILD SUCCESSFUL. (Known: novelworld may fail on google-services unrelated to this change — if so, record the exact error and confirm it reproduces on `develop`.)

- [ ] **Step 4: Commit** — `feat(details): wire chapter multi-select into portrait and landscape`

---

### Task 6: On-device verification

**Files:** none (fixes found here go back into the owning task's files with their own commit).

- [ ] **Step 1: Room version check** — diff `@Database(version = …)` in `favoritesdatabase` between this worktree and the main checkout before installing; if they differ, stop and tell the user.
- [ ] **Step 2: Install** `:mangaworld:installNoFirebaseDebug` on the user-confirmed device (`adb devices -l`; ask if more than one).
- [ ] **Step 3: Verify with ARTEMIS (Flash) on a manga with ≥10 chapters, portrait:**
  - Long-press a chapter → "1 selected" bar, toolbar with Read + Share, FAB hidden.
  - Tap two more → Read/Share gone, count 3.
  - Mark read → rows read, selection cleared, snackbar "Marked 3 chapters read" with Undo; Undo restores.
  - Bookmark on a mixed selection → all bookmarked; Undo restores each.
  - Select all → button disabled; Invert → selection empty, bar returns.
  - Back while selecting clears selection, does not leave screen.
  - Single-select toolbar fully visible at the device's width (Review Focus 5).
  - Swipe on a row does nothing while selecting.
- [ ] **Step 4: Rotate to landscape (or use a tablet/foldable emulator):** toolbar and snackbar sit inside the chapter pane; left pane buttons still work without clearing selection.
- [ ] **Step 5: AnimeWorld smoke test** (if an Aniyomi/CloudStream source is installed): 1 selected → Download visible and triggers per-episode download; 2 selected → Download hidden.
- [ ] **Step 6: Report** results with screenshots; no commit unless fixes were needed.
