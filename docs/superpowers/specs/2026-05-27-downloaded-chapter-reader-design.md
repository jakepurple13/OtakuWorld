# Downloaded Chapter Reader — Design Spec

**Date:** 2026-05-27  
**Branch:** feat/downloading  
**Scope:** 2 files, ~25 lines

## Problem

Navigating from `DownloadScreen` to `ReadView` shows nothing.  
`ReadViewModel.init` sets `list = chapterHolder.chapters.orEmpty()`.  
`ChapterHolder` is never populated from `DownloadScreen` → `list` is empty →  
`loadInitialChapter()` hits `list.getOrNull(0) ?: return` and exits.  
`MangaReader` NavKey already carries `filePath` and `downloaded=true` but neither is used when `list` is empty.

## Solution

Approach A — direct path load in `ReadViewModel`.

When `list.isEmpty() && mangaReader.downloaded && filePath != null`:  
skip the `KmpChapterModel`-based flow, read images directly from `filePath`.

## Changes

### ReadViewModel.kt

1. **`init` — add downloaded-only branch** after existing list setup:
   ```kotlin
   if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
       loadDirectFromPath(mangaReader.filePath)
   }
   ```
   Existing `loadInitialChapter()` call stays in the else path.

2. **`loadDirectFromPath(filePath: String)`** — new private function:
   - `PlatformFile(filePath).list()` sorted by numeric file prefix
   - `sanitizePath()` applied per file (same as `chapterFlow` local branch)
   - Emits `PageItem.ChapterTransition(1, 0)` + pages + `PageItem.ChapterTransition(0, -1)`
   - Uses same `loadingChapters`, `exceptionDao`, `heatMapDao` as other load paths

### DownloadScreen.kt

1. **`navigateToMangaReader` call** — add `mangaTitle = c?.folderName` so reader title is not blank.

## Data Flow

```
DownloadScreen chapter click
  → navigateToMangaReader(filePath=chapterFolder, downloaded=true, mangaTitle=folderName)
  → ReadViewModel.init: list empty + downloaded + filePath set
  → loadDirectFromPath(filePath)
  → PlatformFile(filePath).list() → sort → sanitizePath → pageItems
  → Reader renders images ✓
```

## Edge Cases / Non-Goals

- `ChapterTransition` sentinels for index `1` / `-1` render but are inert —  
  `appendChapter`/`prependChapter` guard `if (idx > list.lastIndex) return`.
- `addToFavorites` / `chapters` tracking: runs against empty `mangaUrl`, returns empty, no crash.
- Cross-chapter navigation between downloaded chapters: **not in scope** (can extend later by populating `list` with sibling dirs).
- No unit tests required.
