# Infinite Manga Scroll — Design Spec

**Date:** 2026-04-24
**Branch:** infinite-manga-scroll
**Scope:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/`

---

## Goal

Replace the single-chapter page list with a continuous infinite scroll experience across both the List (LazyColumn) and Pager (VerticalPager) reader types. Chapters load lazily when their transition marker becomes visible. A sliding window of 3 chapters keeps memory bounded. Scrolling backward reloads previous chapters.

---

## Data Model

### New sealed class: `PageItem`

Add to the reader package (new file `PageItem.kt`):

```kotlin
sealed class PageItem {
    data class Page(val url: String, val chapterIndex: Int) : PageItem()
    data class ChapterTransition(val fromChapter: Int, val toChapter: Int) : PageItem()
}
```

- `Page.chapterIndex` identifies which chapter owns this page — used for sliding window eviction and `currentChapter` derivation.
- `ChapterTransition` items are inserted between chapter page blocks. They trigger loading when they become visible.

---

## ViewModel Changes (`ReadViewModel`)

### Replaced fields

| Before | After |
|---|---|
| `pageList: SnapshotStateList<String>` | `pageItems: SnapshotStateList<PageItem>` |
| `isLoadingPages: Boolean` | `loadingChapters: SnapshotStateSet<Int>` (per-chapter loading state) |

### New fields

```kotlin
val loadedChapterWindow = ArrayDeque<Int>()  // ordered list of loaded chapter indices
val WINDOW_SIZE = 3
```

### New / changed methods

**`loadInitialChapter()`** (replaces `loadPages()` call in `init`)
- Loads the starting chapter's pages as `PageItem.Page(url, currentChapter)` items.
- Seeds `loadedChapterWindow` with the initial chapter index.

**`appendChapter(chapterIndex: Int)`** — forward load
1. Guard: return if `chapterIndex < 0` or already in `loadedChapterWindow`.
2. If `loadedChapterWindow.size >= WINDOW_SIZE`: remove `loadedChapterWindow.removeFirst()`, strip all `PageItem.Page` items with that `chapterIndex` from the front of `pageItems`.
3. Mark `chapterIndex` as loading in `loadingChapters`.
4. Insert `ChapterTransition(fromChapter = loadedChapterWindow.last(), toChapter = chapterIndex)` at the end of `pageItems`.
5. Load pages, append as `PageItem.Page(url, chapterIndex)` items.
6. Add `chapterIndex` to `loadedChapterWindow`.
7. Remove `chapterIndex` from `loadingChapters`.

**`prependChapter(chapterIndex: Int)`** — backward load
- Mirror of `appendChapter` but inserts at index 0 of `pageItems`.
- Drops from `loadedChapterWindow.removeLast()` when window full.
- Returns `insertedCount` (= prepended pages count + 1 for the transition item) so the composable can compensate scroll position via `listState.requestScrollToItem(insertedCount)`.

**`updateCurrentChapter(chapterIndex: Int)`**
- Sets `currentChapter = chapterIndex`.
- Saves `ChapterWatched` to DB (moved from `addChapterToWatched`).

**`addChapterToWatched()`** — kept for drawer/button jump navigation
- Now calls `appendChapter` or `prependChapter` instead of `loadPages`.

**`refresh()`** — unchanged behavior, but rebuilds `pageItems` from scratch.

---

## Composable Changes

### `ListView` / `reader()` LazyListScope

`reader()` iterates `pageItems` and switches on type:

```kotlin
pageItems.forEachIndexed { index, item ->
    when (item) {
        is PageItem.Page -> item { ChapterPage(...) }
        is PageItem.ChapterTransition -> item(key = "transition_${item.fromChapter}_${item.toChapter}") {
            ChapterTransitionItem(...)
        }
    }
}
```

**Trigger logic** — `LaunchedEffect(listState)` using `snapshotFlow { listState.layoutInfo.visibleItemsInfo }`:
- Find any visible item whose key matches a `ChapterTransition`.
- If `transition.toChapter < transition.fromChapter` (forward): call `vm.appendChapter(toChapter)`.
- If `transition.toChapter > transition.fromChapter` (backward): call `vm.prependChapter(toChapter)`, then `listState.requestScrollToItem(insertedCount)` to cancel scroll jump.

**`currentChapter` derivation** — shared `LaunchedEffect`:
```kotlin
snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index }
    .collect { index ->
        (pageItems.getOrNull(index) as? PageItem.Page)?.chapterIndex
            ?.let { vm.updateCurrentChapter(it) }
    }
```

### `PagerView`

- `count = pageItems.size` (no more `+1`).
- Each page switches on `pageItems[page]`:
  - `Page` → `ChapterPage` (unchanged)
  - `ChapterTransition` → `ChapterTransitionItem` in a full-screen `Box`

**Trigger logic** — `snapshotFlow { pagerState.currentPage }`:
```kotlin
(pageItems.getOrNull(it) as? PageItem.ChapterTransition)?.let { t ->
    if (t.toChapter < t.fromChapter) vm.appendChapter(t.toChapter)
    else vm.prependChapter(t.toChapter)
}
```

Note: Pager does not need scroll-jump compensation — `VerticalPager` handles index shifts correctly when items are prepended because it is index-stable.

### Scroll sync

Existing `LaunchedEffect` blocks syncing `listState ↔ pagerState` are unchanged — both are still indexed by position into `pageItems`.

---

## Chapter Transition UI (`ChapterTransitionItem`)

Replaces `LastPageReached`. Full-height composable (not full-screen — fills available space in the list).

```
┌──────────────────────────────────────┐
│                                      │
│  Finished                            │
│  Chapter X: <fromChapter name>       │  bold, centered
│  ──────────────────────────────────  │
│  Next                                │
│  Chapter Y: <toChapter name>         │  muted, centered
│                                      │
│       ◌ (loading indicator)          │  visible while loading
│                                      │
└──────────────────────────────────────┘
```

**Edge cases:**
- `toChapter < 0`: show "No more chapters" — no loading triggered.
- `toChapter > list.lastIndex`: show "Beginning of series" — no loading triggered.
- `isLoading` (derived from `loadingChapters.contains(toChapter)`): show `CircularWavyProgressIndicator`, hide once done.

**Removed:** `ChangeChapterSwipe` and `SwipeToDismissBox` — chapter navigation is now scroll-driven. The swipe UI is removed entirely from the end-of-chapter experience.

---

## Sliding Window Details

| Event | `loadedChapterWindow` | `pageItems` |
|---|---|---|
| Initial load (ch 5) | `[5]` | pages of ch 5 |
| Scroll forward (ch 4) | `[5, 4]` | pages of ch 5 → transition → pages of ch 4 |
| Scroll forward (ch 3) | `[5, 4, 3]` | … → transition → pages of ch 3 |
| Scroll forward (ch 2) | `[4, 3, 2]` | ch 5 pages dropped from front |
| Scroll back (ch 3) | `[3, 4, 2]` | no drop needed, ch 3 already loaded |
| Scroll back past ch 3 (ch 6) | `[6, 5, 4]` | ch 2 pages dropped from end |

Window size is a constant `WINDOW_SIZE = 3`. Can be made a user setting later.

---

## Files Changed

| File | Change |
|---|---|
| `reader/PageItem.kt` | **New** — sealed class |
| `reader/ReadViewModel.kt` | Replace `pageList` with `pageItems`, new load methods |
| `reader/ReaderCompose.kt` | Update `ListView`, `PagerView`, trigger logic, sync logic |
| `reader/Pages.kt` | Add `ChapterTransitionItem`, remove `ChangeChapterSwipe` swipe logic from end-of-chapter |

`FlipPager` and `CurlPager` reader types are **out of scope** — they remain chapter-at-a-time with the existing `LastPageReached` UI.

---

## Out of Scope

- Preloading (next chapter loads only when transition is visible, not proactively)
- FlipPager / CurlPager infinite scroll
- Configurable window size
- Download-mode infinite scroll (downloaded chapters stay single-chapter)
