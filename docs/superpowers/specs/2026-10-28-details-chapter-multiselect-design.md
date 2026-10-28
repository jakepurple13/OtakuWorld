# Details Screen: Chapter Multi-Select — Design

Date: 2026-10-28
Approved preview: https://claude.ai/artifact/5ahBLUFTWKa393Q5VL5WF2

## Intent

Bulk chapter actions on the Details screen are scattered and clunky today: a side drawer
(`MarkAsScreen`) of checkboxes for read state, a separate bottom sheet (`BatchDownloads`) for
downloading, and a long-press options sheet (`chapterItemOptions`) for single-chapter actions.
Replace all three with one pattern the app already uses on the Notifications screen: long-press a
row to start selecting, tap to add/remove, and act on the selection from a
`HorizontalFloatingToolbar`.

## Goals

- Long-press a chapter row enters selection mode with that chapter selected.
- A floating toolbar offers: mark as read, bookmark, download, select all, invert selection.
- With exactly one chapter selected, the toolbar also offers Read and Share.
- Remove the Mark As drawer, the batch download sheet, and the long-press options sheet.
- Behavior and motion match the Notifications screen so the two feel like one system.
- Portrait (`DetailsView`) and landscape (`DetailsViewLandscape`) both get it.

## Non-goals

- No change to tap-to-open, swipe actions outside selection mode, the header, description,
  notes, lists, favorites, or the FAB menu's other entries.
- No range select (drag-to-select or shift-click).
- No change to the download manager or existing `GenericInfo` download method signatures.
  One addition is allowed: `val supportsBatchDownload: Boolean get() = false` on
  `KmpGenericInfo`/`GenericInfo` (wherever `batchDownloadChapters` is declared), overridden to
  `true` in `GenericSharedManga` (the only `batchDownloadChapters` override today).

## Anti-goals

- Selection highlight must not be confused with the existing unread highlight.
- Swipe-to-dismiss must not fire while selecting.

## Behavior

### Entering, editing, leaving selection

- Long-press a chapter row: haptic `HapticFeedbackType.LongPress`, toggle that chapter into the
  selection. Selection mode = selection is non-empty.
- While selecting:
  - Tap a row toggles it (does not open the reader).
  - Long-press a row also toggles it.
  - Swipe actions are disabled (`enableDismissFrom*` = false).
  - Row trailing bookmark/share icon buttons are hidden (download-state indicator stays).
  - The FAB menu is hidden.
  - Back (`BackHandler`) clears the selection.
- Selection empties (last row deselected, close button, invert to zero, or after an action
  that clears) → leave selection mode.
- Selection is keyed by chapter URL and lives in `DetailsViewModel`
  (`mutableStateSetOf<String>()` + `isSelecting` derivedStateOf), mirroring
  `NotificationScreenViewModel.selected/toggleSelection/selectAll/clearSelection`.
  Add `invertSelection()`.

### Top app bar

`AnimatedContent(isSelecting)` swaps the normal bar for a selection bar, same transitionSpec as
`NotificationScreen` (forward: slide in from bottom / out to top; back: reverse). Selection bar:
close icon (clears selection), title `"<n> selected"` reusing `Res.string.selected_count`,
container `surfaceContainerHigh`. The count animates with a vertical `AnimatedContent` roll
(up when growing, down when shrinking). Select all lives in the toolbar, not the top bar.

### Floating toolbar

`AnimatedVisibility(isSelecting)` at bottom center, enter
`slideInVertically { it * 2 } + fadeIn() + scaleIn()`, exit the reverse, padded by
`LocalNavHostPadding.current` and offset `-FloatingToolbarDefaults.ScreenOffset` — identical to
`NotificationScreen`. Contents, left to right:

1. Single-selection group (only when exactly 1 selected), `AnimatedVisibility` with
   `expandHorizontally + fadeIn` / `shrinkHorizontally + fadeOut`:
   - **Read** — filled primary button with play icon + "Read". Calls `detailsActions.onClick`.
   - **Share** — icon button. Calls `detailsActions.shareChapter`.
   - Vertical divider.
2. **Mark read / Mark unread** — if every selected chapter is read, label/icon is "Mark unread"
   (`Icons.Default.RemoveDone`) and it marks all unread; otherwise "Mark read"
   (`Icons.Default.DoneAll`) and it marks all read. Icon swaps via `AnimatedContent`/`Crossfade`.
3. **Bookmark / Remove bookmark** — same rule against bookmark state
   (`Icons.Outlined.BookmarkBorder` / `Icons.Default.BookmarkRemove`).
4. **Download / Delete** — shown only when the source can download
   (`info.source.canDownload`, the same check the old options sheet used), and then:
   - **Exactly 1 selected:** always shown. "Download" calls `detailsActions.onDownload`
     (`genericInfo.downloadChapter`, the per-chapter path every app implements). If that chapter
     is `ChapterDownloadUiState.Downloaded`, it is "Delete" and calls
     `genericInfo.deleteDownloadedChapter`.
   - **2+ selected:** shown only when `genericInfo.supportsBatchDownload` is true (new flag, see Non-goals).
     If every selected chapter is `Downloaded`, it is "Delete" and calls
     `genericInfo.deleteDownloadedChapter` for each; otherwise "Download" and calls
     `genericInfo.batchDownloadChapters` with only the selected chapters whose state is `None`.
   - Hide/show animates (`AnimatedVisibility` with horizontal expand/shrink) as the count crosses
     1 ↔ 2 in apps without batch support.
5. Vertical divider.
6. **Select all** — selects every chapter; disabled when all are selected.
7. **Invert selection** — selects exactly the unselected chapters.

All icon buttons have content descriptions equal to their current label.

Landscape (`DetailsViewLandscape`, two-pane): the toolbar and the Undo snackbar host are anchored
bottom-center of the chapter-list pane, not the whole screen, so they don't cover the
header/description pane (as shown in the approved preview). *(Builder-check call.)*
The left pane (header, description, `DetailBottomBar`) is unchanged and stays interactive while
selecting; using it does not clear the selection. The top bar swaps to the selection bar across
the full width, same as portrait. The chapter `LazyColumn` gets bottom content padding while
selecting so the last chapter can scroll clear of the toolbar (portrait too).

### After an action

- Mark read/unread, Bookmark/Remove bookmark, Download/Delete, Read, Share clear the selection.
- Select all and Invert keep selection mode (invert to zero exits).
- Mark read/unread and Bookmark/Remove bookmark show a snackbar with an **Undo** action that
  restores each affected chapter's prior read/bookmark state. Message examples:
  "Marked 4 chapters read", "Bookmarked 3 chapters".
- Download and Delete show a plain snackbar with no Undo ("Downloading 3 chapters",
  "Deleted 2 chapters").
- Read and Share show no snackbar.

### Rows

- Selected row container animates (`animateColorAsState`, ~200ms) to `primaryContainer`.
- In selection mode, every row gets a leading selection indicator (ring; filled check when
  selected, check pops in with `scaleIn`). The unread dot stays visible as a smaller pip.
- Outside selection mode, rows look and behave as today.
- `ListItem(selected = !isRead)` currently tints unread rows; while selecting, `selected` must
  reflect selection instead, so the highlight only ever means "selected".

## Removals

- `MarkAsScreen` composable and the `ModalNavigationDrawer`/`ModalDrawerSheet` wrapping in
  `DetailsPortrait.kt` and `DetailsLandscape.kt`, plus the drawer `BackHandler` and
  `rememberDrawerState`.
- "Mark As..." item in `ChapterListHeader` ButtonGroup and in the `DetailActions` dropdown;
  `DetailActions.scaffoldState` and `ChapterListHeader.markAsClick` parameters.
- `BatchDownloads` composable, `showBatchDownloads` state, `DetailsActions.batchDownload`, the
  "Batch Download" entries in the `DetailActions` dropdown and `DetailFloatingActionButtonMenu`,
  and their `onBatchDownloadClick` parameters.
- `chapterItemOptions` (long-press options sheet) and its use in `ChapterItem`.
- Any strings/imports orphaned by the above. `Res.string.markAs` may still be used elsewhere
  (`TipDemos.kt`, `GeneralContent.kt`) — only remove if unused.

## Constraints

- Code lives in `kmpuiviews` commonMain (KMP: Android, JVM, iOS). Use only APIs available in
  common Compose Multiplatform (the Notifications screen already uses all of them).
- Follow existing Notifications screen code for structure, specs, and naming.
- Use `noFirebase` variants for local builds.
- `DetailsViewModelTest` (jvmTest) covers markAs/toggleBookmark; extend it for selection
  (toggle, selectAll, invert, clear).

## Left to the builder

- Whether toolbar/selection-bar composables live in `DetailsScreen.kt` or a new file in the
  `details` package.
- How bulk mark/bookmark is expressed in the ViewModel (loop over existing `markAs` /
  `toggleBookmark`, or new bulk functions). Bookmark bulk must set an explicit target state, not
  toggle each chapter, so mixed selections end consistent.
- Exact animation durations within the ranges in the approved preview.
