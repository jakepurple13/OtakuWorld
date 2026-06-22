# Downloads: Continuous Reading Design

**Date:** 2026-06-22  
**Branch:** feat/bettering-download-in-mangaworld  
**Status:** Approved

## Summary

When a user taps a downloaded chapter in the MangaWorld Downloads screen, the reader opens at that chapter and can seamlessly scroll into all other downloaded chapters for that manga — without going back to the Downloads screen.

## Data Flow

1. User taps a chapter row in `DownloadScreen.ChapterItem`
2. `ChapterItem` injects `ChapterHolder` via `koinInject()`
3. Before navigating: collect all `chapterFolder` paths for that manga group (`file.value.keys`), sort descending by numeric folder name (index 0 = newest chapter, matching existing reader convention)
4. Set `chapterHolder.downloadedChapterPaths = sortedPaths`
5. Navigate: `ReadViewModel.navigateToMangaReader(downloaded=true, filePath=tappedChapterFolder)` — unchanged signature
6. `ReadViewModel.init` detects non-null `downloadedChapterPaths`, derives `currentChapter = paths.indexOf(mangaReader.filePath)` (user's tapped chapter), clears the holder field immediately
7. Reader loads that chapter; `appendChapter`/`prependChapter` load adjacent downloaded chapters as the user scrolls

## Component Changes

### `ChapterHolder`
**File:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/ChapterHolder.kt`

Add one field:
```kotlin
var downloadedChapterPaths: List<String>? = null
```
Cleared by `ReadViewModel.init` after reading to prevent stale paths bleeding into subsequent navigations.

---

### `DownloadScreen` — `ChapterItem`
**File:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt`

In the chapter tap `onClick` (inside `SwipeToDismissBox` content), before calling `navigateToMangaReader`:

```kotlin
val chapterHolder: ChapterHolder = koinInject()
val allPaths = file.value.entries
    .sortedByDescending { (_, chapters) ->
        chapters.firstOrNull()?.chapterName?.filter { it.isDigit() }?.toIntOrNull() ?: 0
    }
    .map { (chapterFolder, _) -> chapterFolder }
chapterHolder.downloadedChapterPaths = allPaths
```

Sorting uses `chapterName` (e.g. "Chapter 14" → 14) rather than raw folder path to handle arbitrary folder naming schemes.

`navigateToMangaReader` call is unchanged — still passes `filePath = c?.chapterFolder`.

---

### `ReadViewModel`
**File:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt`

**New fields:**
```kotlin
private var downloadedPaths: List<String> = emptyList()
val isDownloadedPathsMode: Boolean get() = downloadedPaths.isNotEmpty()
val chapterCount: Int get() = if (isDownloadedPathsMode) downloadedPaths.size else list.size

fun chapterName(index: Int): String? =
    if (isDownloadedPathsMode) downloadedPaths.getOrNull(index)?.substringAfterLast("/")
    else list.getOrNull(index)?.name
```

**Modified `init`** — replace the existing downloaded branch:
```kotlin
val paths = chapterHolder.downloadedChapterPaths
chapterHolder.downloadedChapterPaths = null
if (paths != null && paths.isNotEmpty()) {
    downloadedPaths = paths
    currentChapter = paths.indexOf(mangaReader.filePath).coerceAtLeast(0)
    loadDownloadedChapterAtIndex(currentChapter)
} else if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
    loadDirectFromPath(mangaReader.filePath)  // existing single-chapter path — unchanged
} else {
    loadInitialChapter()
}
```

**New private function `loadDownloadedChapterAtIndex(index: Int)`:**  
Same mechanics as `loadDirectFromPath` but:
- Uses `downloadedPaths[index]` as the path
- Emits `PageItem.ChapterTransition(index + 1, index)` and `PageItem.ChapterTransition(index, index - 1)` with the correct `index` (not hardcoded 0) so `appendChapter`/`prependChapter` bounds checks work

**Modified `appendChapter` / `prependChapter`:**  
Add a downloaded-mode branch at the top of each:
- `appendChapter` (returns `Unit`): bounds-check against `downloadedPaths.size`, load via `downloadedChapterFlow(downloadedPaths[chapterListIndex])`, skip `ChapterWatched` marking, `return`
- `prependChapter` (returns `Int` — page count inserted): same, `return insertedPageCount` (or `return 0` if bounds fail)

**`downloadedChapterFlow(path: String): Flow<List<String>>`** — extracted helper:  
Identical to the `PlatformFile(localPath).list().sortedBy...` block reused by both `loadDirectFromPath` and the new append/prepend downloaded path. Avoids duplication.

---

### `ReaderCompose.kt`
**File:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt`

Five mechanical replacements:

| Before | After |
|--------|-------|
| `viewModel.list.size` | `viewModel.chapterCount` |
| `viewModel.list.lastIndex` | `viewModel.chapterCount - 1` |
| `vm.list.getOrNull(item.fromChapterListIndex)?.name` | `vm.chapterName(item.fromChapterListIndex)` |
| `vm.list.getOrNull(item.toChapterListIndex)?.name` | `vm.chapterName(item.toChapterListIndex)` |
| `viewModel.list.size > 1` (gesture/button guards) | `viewModel.chapterCount > 1` |

## What Does NOT Change

- `MangaReader` NavKey — no new fields
- `loadDirectFromPath` — unchanged (single-chapter downloaded fallback still works)
- Network-mode reading — `appendChapter`/`prependChapter` behave identically when `isDownloadedPathsMode == false`
- Watched/history tracking — skipped in downloaded mode (no `KmpChapterModel` to key off)

## Out of Scope

- Marking downloaded chapters as read when scrolling through them
- Sorting preference UI for downloaded chapter order
- Desktop (`jvmMain`) reader — same ViewModel changes apply but `ReaderCompose.jvm.kt` may need the same mechanical replacements if it references `list` directly
