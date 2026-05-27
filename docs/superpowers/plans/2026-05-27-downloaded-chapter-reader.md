# Downloaded Chapter Reader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the reader display images when opened from DownloadScreen by loading pages directly from the chapter folder path.

**Architecture:** `ReadViewModel.init` currently exits early when `list` is empty (no `ChapterHolder` data from DownloadScreen). Add a branch that, when `downloaded=true` and `filePath` is set, reads images directly via `PlatformFile(filePath).list()` — reusing the same sorting/sanitizing logic already present in `chapterFlow`'s local-path branch.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, FileKit (`PlatformFile`), Koin, Navigation3

---

## File Map

| File | Change |
|------|--------|
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt` | Add `loadDirectFromPath()`, modify `init` |
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt` | Pass `mangaTitle` in nav call |

---

### Task 1: Add `loadDirectFromPath` to ReadViewModel and branch init

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt`

- [ ] **Step 1: Add `loadDirectFromPath` private function**

  Add after the closing brace of `loadInitialChapter()` (after line 251):

  ```kotlin
  private fun loadDirectFromPath(filePath: String) {
      flow {
          PlatformFile(filePath)
              .list()
              .sortedBy { f -> f.name.split(".").first().toIntOrNull() ?: 0 }
              .fastMap { sanitizePath(it.toKotlinxIoPath().toString()) }
              .let { emit(it) }
      }
          .catch { exceptionDao.insertException(it) }
          .flowOn(Dispatchers.IO)
          .onStart { loadingChapters = loadingChapters + 0 }
          .onEach { urls ->
              pageItems.add(PageItem.ChapterTransition(1, 0))
              pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, 0, i, true) })
              pageItems.add(PageItem.ChapterTransition(0, -1))
              heatMapDao.upsertHeatMap()
          }
          .onCompletion { loadingChapters = loadingChapters - 0 }
          .launchIn(viewModelScope)
  }
  ```

- [ ] **Step 2: Modify `init` to branch on downloaded-only mode**

  Replace:
  ```kotlin
  loadInitialChapter()
  ```
  With:
  ```kotlin
  if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
      loadDirectFromPath(mangaReader.filePath)
  } else {
      loadInitialChapter()
  }
  ```

- [ ] **Step 3: Commit**

  ```bash
  git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt
  git commit -m "feat(reader): load downloaded chapter pages directly from file path

  When navigated from DownloadScreen, ChapterHolder is empty.
  Add loadDirectFromPath() that reads PlatformFile(filePath).list()
  and branch init to use it when list is empty + downloaded + filePath set.

  Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
  ```

---

### Task 2: Pass mangaTitle from DownloadScreen

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt`

- [ ] **Step 1: Add mangaTitle to the nav call**

  In `DownloadScreen.kt`, inside `ChapterItem`'s `clickable` lambda (around line 381), replace:

  ```kotlin
  ReadViewModel.navigateToMangaReader(
      navController,
      filePath = c?.chapterFolder,
      downloaded = true
  )
  ```

  With:

  ```kotlin
  ReadViewModel.navigateToMangaReader(
      navController,
      mangaTitle = c?.folderName,
      filePath = c?.chapterFolder,
      downloaded = true
  )
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt
  git commit -m "feat(downloads): pass mangaTitle when navigating to reader

  Populates MangaReader.mangaTitle so the reader toolbar shows
  the manga name when opened from the downloads screen.

  Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
  ```

---

### Task 3: Verify

- [ ] **Step 1: Build**

  ```bash
  ./gradlew :mangaworld:assembleNoFirebaseDebug
  ```

  Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Manual smoke test**

  1. Open MangaWorld
  2. Navigate to Downloads screen
  3. Expand a manga with downloaded chapters
  4. Tap a chapter
  5. Verify reader opens and images display
  6. Verify manga title appears in reader toolbar
  7. Verify scrolling to chapter end/start shows transition sentinels but no crash
