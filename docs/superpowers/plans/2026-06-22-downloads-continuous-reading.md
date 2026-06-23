# Downloads: Continuous Reading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a user taps a downloaded chapter in the Downloads screen, the reader opens at that chapter and can seamlessly scroll into all other downloaded chapters for that manga.

**Architecture:** `DownloadScreen.ChapterItem` collects and sorts all chapter folder paths for the tapped manga, stores them on `ChapterHolder` (Koin singleton), then navigates. `ReadViewModel.init` detects the paths, stores them internally, and uses them instead of `list: List<KmpChapterModel>` for page loading. `appendChapter`/`prependChapter` gain downloaded-mode branches that load from file paths rather than `KmpChapterModel`. Reader UI references to `viewModel.list` are replaced with computed properties that abstract over both modes.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Koin, kotlinx-coroutines, FileKit (`PlatformFile`), kotlin.test + kotlinx-coroutines-test (jvmTest)

## Global Constraints

- Module: `:mangaworld:shared` — all source changes in `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/`
- Tests: `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/`; run with `./gradlew :mangaworld:shared:jvmTest`
- `ChapterHolder` is a Koin singleton (`singleOf(::ChapterHolder)` in `MangaModule.kt`) — inject via `koinInject()` in composables
- Chapter sort order: descending by numeric digits extracted from `chapterName` (index 0 = newest/highest chapter number, matching reader convention where `--currentChapter` = "next" in reading order)
- Network-mode reading (`isDownloadedPathsMode == false`) must behave identically to today — no regressions
- `loadDirectFromPath` single-chapter fallback must remain intact for cases where `downloadedChapterPaths` is null

---

## File Map

| Action | File |
|--------|------|
| Modify | `ChapterHolder.kt` |
| Modify | `downloads/DownloadScreen.kt` |
| Modify | `reader/ReadViewModel.kt` |
| Modify | `reader/ReaderCompose.kt` |
| Create | `jvmTest/.../ContinuousReadingTest.kt` |

---

### Task 1: Add `downloadedChapterPaths` to `ChapterHolder`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/ChapterHolder.kt`
- Test: `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/ContinuousReadingTest.kt`

**Interfaces:**
- Produces: `ChapterHolder.downloadedChapterPaths: List<String>?` — read by `ReadViewModel.init` in Task 3, written by `DownloadScreen` in Task 5

- [ ] **Step 1: Write the failing test**

Create `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/ContinuousReadingTest.kt`:

```kotlin
package com.programmersbox.manga.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChapterHolderTest {

    @Test
    fun `downloadedChapterPaths is null by default`() {
        val holder = ChapterHolder()
        assertNull(holder.downloadedChapterPaths)
    }

    @Test
    fun `downloadedChapterPaths stores and clears list`() {
        val holder = ChapterHolder()
        holder.downloadedChapterPaths = listOf("/manga/ch10", "/manga/ch2", "/manga/ch1")
        assertEquals(3, holder.downloadedChapterPaths?.size)
        assertEquals("/manga/ch10", holder.downloadedChapterPaths?.first())
        holder.downloadedChapterPaths = null
        assertNull(holder.downloadedChapterPaths)
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```
./gradlew :mangaworld:shared:jvmTest --tests "com.programmersbox.manga.shared.ChapterHolderTest"
```

Expected: compilation failure — `downloadedChapterPaths` does not exist on `ChapterHolder`.

- [ ] **Step 3: Add field to ChapterHolder**

Open `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/ChapterHolder.kt`. Replace entire file:

```kotlin
package com.programmersbox.manga.shared

import com.programmersbox.kmpmodels.KmpChapterModel

class ChapterHolder {
    var chapterModel: KmpChapterModel? = null
    var chapters: List<KmpChapterModel>? = null
    var downloadedChapterPaths: List<String>? = null
}
```

- [ ] **Step 4: Run test — expect PASS**

```
./gradlew :mangaworld:shared:jvmTest --tests "com.programmersbox.manga.shared.ChapterHolderTest"
```

Expected: `BUILD SUCCESSFUL`, both tests pass.

- [ ] **Step 5: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/ChapterHolder.kt \
        mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/ContinuousReadingTest.kt
git commit -m "feat(downloads): add downloadedChapterPaths to ChapterHolder"
```

---

### Task 2: Add sort helper to `DownloadScreen` + inject `ChapterHolder`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt`
- Test: `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/ContinuousReadingTest.kt`

**Interfaces:**
- Consumes: `ChapterHolder.downloadedChapterPaths` from Task 1
- Produces: `internal fun sortedChapterPaths(chapters: Map<String, List<DownloadedChapters>>): List<String>` — tested here, used in `ChapterItem` onClick

- [ ] **Step 1: Write the failing tests**

Append to `ContinuousReadingTest.kt`:

```kotlin
import com.programmersbox.manga.shared.downloads.DownloadedChapters
import com.programmersbox.manga.shared.downloads.sortedChapterPaths

class SortedChapterPathsTest {

    private fun chapter(name: String, folder: String) = DownloadedChapters(
        name = name,
        id = folder,
        data = "",
        assetFileStringUri = "",
        folder = "",
        folderName = "",
        chapterFolder = folder,
        chapterName = name,
    )

    @Test
    fun `sortedChapterPaths orders paths by numeric digits in name descending`() {
        val map = mapOf(
            "/root/ch1" to listOf(chapter("Chapter 1", "/root/ch1")),
            "/root/ch10" to listOf(chapter("Chapter 10", "/root/ch10")),
            "/root/ch2" to listOf(chapter("Chapter 2", "/root/ch2")),
        )
        val result = sortedChapterPaths(map)
        assertEquals(listOf("/root/ch10", "/root/ch2", "/root/ch1"), result)
    }

    @Test
    fun `sortedChapterPaths puts non-numeric names last`() {
        val map = mapOf(
            "/root/prologue" to listOf(chapter("Prologue", "/root/prologue")),
            "/root/ch1" to listOf(chapter("Chapter 1", "/root/ch1")),
        )
        val result = sortedChapterPaths(map)
        // "Chapter 1" digits=1, "Prologue" digits=0 → ch1 first
        assertEquals(listOf("/root/ch1", "/root/prologue"), result)
    }

    @Test
    fun `sortedChapterPaths handles empty map`() {
        assertEquals(emptyList(), sortedChapterPaths(emptyMap()))
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```
./gradlew :mangaworld:shared:jvmTest --tests "com.programmersbox.manga.shared.SortedChapterPathsTest"
```

Expected: compilation failure — `sortedChapterPaths` does not exist.

- [ ] **Step 3: Add `sortedChapterPaths` function and `ChapterHolder` injection to `DownloadScreen.kt`**

In `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt`:

**3a.** Add import for `ChapterHolder` and `koinInject` near the top (after existing imports):

```kotlin
import com.programmersbox.manga.shared.ChapterHolder
import org.koin.compose.koinInject
```

**3b.** Add `sortedChapterPaths` as a package-level internal function at the bottom of the file (before or after `ChapterItem`):

```kotlin
internal fun sortedChapterPaths(chapters: Map<String, List<DownloadedChapters>>): List<String> =
    chapters.entries
        .sortedByDescending { (_, pageList) ->
            pageList.firstOrNull()?.chapterName?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        }
        .map { (chapterFolder, _) -> chapterFolder }
```

**3c.** In `ChapterItem`, add `chapterHolder` injection and use it in the click handler. The composable currently starts at line ~277. Add `koinInject()` at the top of the composable body, and update the onClick in `SwipeToDismissBox`'s `content` lambda:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterItem(
    file: Map.Entry<String, Map<String, List<DownloadedChapters>>>,
    onDeleted: (DownloadedChapters) -> Unit,
    useNewReader: Boolean = true,
) {
    val chapterHolder: ChapterHolder = koinInject()   // <-- ADD THIS LINE
    var expanded by remember { mutableStateOf(false) }
    // ... rest of existing code unchanged until the onClick inside SwipeToDismissBox content ...
```

In the `content` lambda of `SwipeToDismissBox` (around line 374), replace the existing `clickable` onClick:

**Before:**
```kotlin
) {
    if (useNewReader) {
        ReadViewModel.navigateToMangaReader(
            navController,
            mangaTitle = c?.folderName,
            filePath = c?.chapterFolder,
            downloaded = true
        )
    } else {
```

**After:**
```kotlin
) {
    if (useNewReader) {
        chapterHolder.downloadedChapterPaths = sortedChapterPaths(file.value)
        ReadViewModel.navigateToMangaReader(
            navController,
            mangaTitle = c?.folderName,
            filePath = c?.chapterFolder,
            downloaded = true
        )
    } else {
```

- [ ] **Step 4: Run tests — expect PASS**

```
./gradlew :mangaworld:shared:jvmTest --tests "com.programmersbox.manga.shared.SortedChapterPathsTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadScreen.kt \
        mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/ContinuousReadingTest.kt
git commit -m "feat(downloads): inject ChapterHolder and set downloadedChapterPaths on chapter tap"
```

---

### Task 3: Add `downloadedChapterFlow` + computed properties to `ReadViewModel`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt`

**Interfaces:**
- Produces:
  - `private var downloadedPaths: List<String>` — consumed by Tasks 4 and 5
  - `val isDownloadedPathsMode: Boolean` — consumed by Tasks 4 and 5
  - `val chapterCount: Int` — consumed by Task 6
  - `fun chapterName(index: Int): String?` — consumed by Task 6
  - `private fun downloadedChapterFlow(filePath: String): Flow<List<String>>` — consumed by Tasks 4 and 5

- [ ] **Step 1: Add fields, computed properties, and `downloadedChapterFlow` to `ReadViewModel`**

In `ReadViewModel.kt`, add the following immediately after `private val loadedChapterWindow = ArrayDeque<Int>()` (around line 134):

```kotlin
private var downloadedPaths: List<String> = emptyList()
val isDownloadedPathsMode: Boolean get() = downloadedPaths.isNotEmpty()
val chapterCount: Int get() = if (isDownloadedPathsMode) downloadedPaths.size else list.size

fun chapterName(index: Int): String? =
    if (isDownloadedPathsMode) downloadedPaths.getOrNull(index)?.substringAfterLast("/")
    else list.getOrNull(index)?.name
```

- [ ] **Step 2: Add `downloadedChapterFlow` private function**

Add after the existing `private fun chapterFlow(...)` function (around line 88), before the `companion object`:

```kotlin
private fun downloadedChapterFlow(filePath: String): Flow<List<String>> =
    flow {
        PlatformFile(filePath)
            .list()
            .sortedBy { f -> f.name.split(".").first().toIntOrNull() ?: 0 }
            .fastMap { sanitizePath(it.toKotlinxIoPath().toString()) }
            .let { emit(it) }
    }
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
```

- [ ] **Step 3: Refactor `loadDirectFromPath` to use `downloadedChapterFlow`**

Find `loadDirectFromPath` (around line 257). Replace its body with:

```kotlin
private fun loadDirectFromPath(filePath: String) {
    loadedChapterWindow.clear()
    loadedChapterWindow.addLast(0)
    downloadedChapterFlow(filePath)
        .onStart {
            loadingChapters = loadingChapters + 0
            pageItems.clear()
        }
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

- [ ] **Step 4: Verify compilation**

```
./gradlew :mangaworld:shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL` — no compilation errors.

- [ ] **Step 5: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt
git commit -m "feat(reader): add downloadedChapterFlow and computed chapter properties to ReadViewModel"
```

---

### Task 4: Modify `ReadViewModel.init` + add `loadDownloadedChapterAtIndex`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt`

**Interfaces:**
- Consumes: `downloadedPaths`, `isDownloadedPathsMode`, `downloadedChapterFlow` from Task 3; `ChapterHolder.downloadedChapterPaths` from Task 1
- Produces: `private fun loadDownloadedChapterAtIndex(index: Int)` — the entry point for downloaded-mode reading

- [ ] **Step 1: Modify `init` to detect downloaded-paths mode**

In `ReadViewModel.kt`, find the `init` block (around line 147). Replace:

```kotlin
if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
    loadDirectFromPath(mangaReader.filePath)
} else {
    loadInitialChapter()
}
```

With:

```kotlin
val paths = chapterHolder.downloadedChapterPaths
chapterHolder.downloadedChapterPaths = null
if (paths != null && paths.isNotEmpty()) {
    downloadedPaths = paths
    currentChapter = paths.indexOf(mangaReader.filePath).coerceAtLeast(0)
    loadDownloadedChapterAtIndex(currentChapter)
} else if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
    loadDirectFromPath(mangaReader.filePath)
} else {
    loadInitialChapter()
}
```

- [ ] **Step 2: Add `loadDownloadedChapterAtIndex`**

Add the following private function after `loadDirectFromPath`:

```kotlin
private fun loadDownloadedChapterAtIndex(index: Int) {
    val filePath = downloadedPaths.getOrNull(index) ?: return
    loadedChapterWindow.clear()
    loadedChapterWindow.addLast(index)
    downloadedChapterFlow(filePath)
        .onStart {
            loadingChapters = loadingChapters + index
            pageItems.clear()
        }
        .onEach { urls ->
            pageItems.add(PageItem.ChapterTransition(index + 1, index))
            pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, index, i, true) })
            pageItems.add(PageItem.ChapterTransition(index, index - 1))
            heatMapDao.upsertHeatMap()
        }
        .onCompletion { loadingChapters = loadingChapters - index }
        .launchIn(viewModelScope)
}
```

- [ ] **Step 3: Verify compilation**

```
./gradlew :mangaworld:shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual smoke test — initial chapter loads**

Run the app (`./gradlew :mangaworld:assembleNoFirebaseDebug`, install on device). Navigate to Downloads. Tap any chapter. Verify:
- Reader opens at the tapped chapter
- Pages load correctly
- Chapter name/number shown in top bar matches the tapped chapter

- [ ] **Step 5: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt
git commit -m "feat(reader): detect downloadedChapterPaths in init, load starting chapter by index"
```

---

### Task 5: Add `appendDownloadedChapter` + `prependDownloadedChapter`, wire into `appendChapter`/`prependChapter`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt`

**Interfaces:**
- Consumes: `downloadedPaths`, `isDownloadedPathsMode`, `downloadedChapterFlow` from Task 3
- Produces: guards in `appendChapter`/`prependChapter` that route to downloaded implementations when `isDownloadedPathsMode == true`

- [ ] **Step 1: Add `appendDownloadedChapter`**

Add the following private function after `loadDownloadedChapterAtIndex`:

```kotlin
private fun appendDownloadedChapter(chapterListIndex: Int) {
    if (chapterListIndex < 0 || chapterListIndex >= downloadedPaths.size) return
    if (chapterListIndex in loadedChapterWindow) return
    loadedChapterWindow.addLast(chapterListIndex)
    val fromChapterListIndex = loadedChapterWindow[loadedChapterWindow.size - 2]

    viewModelScope.launch {
        while (loadedChapterWindow.size > WINDOW_SIZE) {
            val dropped = loadedChapterWindow.removeFirst()
            val firstKeptIdx = pageItems.indexOfFirst { item ->
                when (item) {
                    is PageItem.Page -> item.chapterListIndex != dropped
                    is PageItem.ChapterTransition -> item.fromChapterListIndex != dropped
                }
            }
            if (firstKeptIdx > 0) pageItems.subList(0, firstKeptIdx).clear()
        }

        loadingChapters = loadingChapters + chapterListIndex

        val newPageTransition = PageItem.ChapterTransition(fromChapterListIndex, chapterListIndex)
        if (newPageTransition !in pageItems) pageItems.add(newPageTransition)

        downloadedChapterFlow(downloadedPaths[chapterListIndex])
            .onEach { urls ->
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, true) })
                pageItems.add(PageItem.ChapterTransition(chapterListIndex, chapterListIndex - 1))
                heatMapDao.upsertHeatMap()
            }
            .onCompletion {
                loadingChapters = loadingChapters - chapterListIndex
                addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
            }
            .launchIn(viewModelScope)
    }
}
```

- [ ] **Step 2: Add `prependDownloadedChapter`**

Add after `appendDownloadedChapter`:

```kotlin
private suspend fun prependDownloadedChapter(chapterListIndex: Int): Int {
    if (chapterListIndex < 0 || chapterListIndex >= downloadedPaths.size) return 0
    if (chapterListIndex in loadedChapterWindow) return 0

    if (loadedChapterWindow.size >= WINDOW_SIZE) {
        val dropped = loadedChapterWindow.removeLast()
        val removeFrom = pageItems.indexOfFirst {
            it is PageItem.ChapterTransition && it.toChapterListIndex == dropped
        }.takeIf { it >= 0 } ?: pageItems.indexOfFirst {
            it is PageItem.Page && it.chapterListIndex == dropped
        }
        if (removeFrom >= 0) {
            while (pageItems.size > removeFrom) pageItems.removeAt(removeFrom)
        }
    }

    val toChapterListIndex = loadedChapterWindow.first()
    loadedChapterWindow.addFirst(chapterListIndex)
    loadingChapters = loadingChapters + chapterListIndex

    val newPages = mutableListOf<PageItem>()
    downloadedChapterFlow(downloadedPaths[chapterListIndex])
        .firstOrNull()
        ?.let { urls ->
            newPages.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, true) })
            heatMapDao.upsertHeatMap()
        }

    loadingChapters = loadingChapters - chapterListIndex

    val insertedItems: List<PageItem> = newPages + PageItem.ChapterTransition(chapterListIndex, toChapterListIndex)
    pageItems.addAll(0, insertedItems)
    addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
    return insertedItems.size
}
```

- [ ] **Step 3: Add guards in `appendChapter` and `prependChapter`**

In `appendChapter` (around line 294), add at the very top of the function body, before the existing bounds check:

```kotlin
fun appendChapter(chapterListIndex: Int) {
    if (isDownloadedPathsMode) {
        appendDownloadedChapter(chapterListIndex)
        return
    }
    if (chapterListIndex < 0 || chapterListIndex > list.lastIndex) return
    // ... rest of existing appendChapter unchanged
```

In `prependChapter` (around line 341), add at the very top of the function body, before the existing bounds check:

```kotlin
suspend fun prependChapter(chapterListIndex: Int): Int {
    if (isDownloadedPathsMode) return prependDownloadedChapter(chapterListIndex)
    println("prependChapter: $chapterListIndex")
    // ... rest of existing prependChapter unchanged
```

- [ ] **Step 4: Verify compilation**

```
./gradlew :mangaworld:shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual smoke test — continuous scrolling**

Run the app. Navigate to Downloads. Tap a chapter that is NOT the last one downloaded. Scroll to the end of the chapter. Verify:
- A chapter transition card appears between chapters
- The next downloaded chapter loads automatically as you scroll past the transition
- The previous chapter is also accessible by scrolling back
- Repeat for at least 3 chapters to verify windowed loading (WINDOW_SIZE=3) evicts correctly

- [ ] **Step 6: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt
git commit -m "feat(reader): add downloaded-mode append/prepend chapter support for continuous reading"
```

---

### Task 6: Update `ReaderCompose.kt` — replace `viewModel.list` references

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt`

**Interfaces:**
- Consumes: `chapterCount: Int` and `fun chapterName(index: Int): String?` from Task 3

- [ ] **Step 1: Replace `viewModel.list` references**

In `ReaderCompose.kt`, make the following replacements (use the exact strings below):

**Replacement 1** — gestures guard (line ~332):
```
// Before:
gesturesEnabled = (viewModel.list.size > 1 && userGestureAllowed) || drawerState.isOpen
// After:
gesturesEnabled = (viewModel.chapterCount > 1 && userGestureAllowed) || drawerState.isOpen
```

**Replacement 2** — top bar chapter name fallback (line ~370):
```
// Before:
?: "Ch ${viewModel.list.size - viewModel.currentChapter}",
// After:
?: "Ch ${viewModel.chapterCount - viewModel.currentChapter}",
```

**Replacement 3** — bottom bar `chapterNumber` (line ~405):
```
// Before:
chapterNumber = (viewModel.list.size - viewModel.currentChapter).toString(),
// After:
chapterNumber = (viewModel.chapterCount - viewModel.currentChapter).toString(),
```

**Replacement 4** — bottom bar `chapterCount` (line ~406):
```
// Before:
chapterCount = viewModel.list.size.toString(),
// After:
chapterCount = viewModel.chapterCount.toString(),
```

**Replacement 5** — `previousButtonEnabled` (line ~409):
```
// Before:
previousButtonEnabled = viewModel.currentChapter < viewModel.list.lastIndex && viewModel.list.size > 1,
// After:
previousButtonEnabled = viewModel.currentChapter < viewModel.chapterCount - 1 && viewModel.chapterCount > 1,
```

**Replacement 6** — `nextButtonEnabled` (line ~410):
```
// Before:
nextButtonEnabled = viewModel.currentChapter > 0 && viewModel.list.size > 1,
// After:
nextButtonEnabled = viewModel.currentChapter > 0 && viewModel.chapterCount > 1,
```

**Replacement 7** — pager `ChapterTransition` names (lines ~576-577):
```
// Before:
fromChapterName = vm.list.getOrNull(item.fromChapterListIndex)?.name,
toChapterName = vm.list.getOrNull(item.toChapterListIndex)?.name,
// After:
fromChapterName = vm.chapterName(item.fromChapterListIndex),
toChapterName = vm.chapterName(item.toChapterListIndex),
```

**Replacement 8** — lazy list `ChapterTransition` names (lines ~695-696):
```
// Before:
fromChapterName = vm.list.getOrNull(item.fromChapterListIndex)?.name,
toChapterName = vm.list.getOrNull(item.toChapterListIndex)?.name,
// After:
fromChapterName = vm.chapterName(item.fromChapterListIndex),
toChapterName = vm.chapterName(item.toChapterListIndex),
```

- [ ] **Step 2: Verify no remaining `viewModel.list` or `vm.list` references related to chapter count/names**

```bash
grep -n "viewModel\.list\|vm\.list" mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt
```

Expected: no output (or only unrelated `.list` references not covered by this feature).

- [ ] **Step 3: Verify compilation**

```
./gradlew :mangaworld:shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual smoke test — UI controls work in downloaded mode**

Run the app. Navigate to Downloads. Tap a chapter (manga with multiple downloaded chapters). Verify in the reader:
- Top bar shows correct chapter name/number (not "Ch 0")
- Bottom bar chapter counter increments correctly as you scroll forward
- Previous/Next chapter buttons are enabled when adjacent chapters exist and disabled at the first/last chapter
- Chapter transition labels show chapter folder names between chapters
- Network-mode reading (open a chapter from the Details screen) still shows chapter names correctly — regression check

- [ ] **Step 5: Run all jvmTests**

```
./gradlew :mangaworld:shared:jvmTest
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt
git commit -m "feat(reader): replace list size/name references with chapterCount/chapterName for downloaded mode"
```
