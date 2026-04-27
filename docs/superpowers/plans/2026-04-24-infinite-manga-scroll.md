# Infinite Manga Scroll Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-chapter reader with a continuous infinite scroll that lazily loads adjacent chapters as transition markers become visible, keeping a 3-chapter sliding window in memory.

**Architecture:** A new `PageItem` sealed class replaces the flat `pageList: List<String>`. The ViewModel manages a `loadedChapterWindow: ArrayDeque<Int>` (max 3) and `pageItems: SnapshotStateList<PageItem>`. Both `ListView` and `PagerView` iterate `pageItems` and trigger append/prepend when a `ChapterTransition` item becomes visible.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose Multiplatform, Compose `LazyColumn`/`VerticalPager`, Kotlin Coroutines, Koin

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/PageItem.kt` | **Create** | Sealed data model for page list items |
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt` | **Modify** | Replace `pageList` with `pageItems`, add sliding window load methods |
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/Pages.kt` | **Modify** | Add `ChapterTransitionItem`, remove `ChangeChapterSwipe` |
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt` | **Modify** | Update `ListView`, `PagerView`, trigger effects, bottom bar page counters |

---

## Task 1: Create `PageItem.kt`

**Files:**
- Create: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/PageItem.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.manga.shared.reader

sealed class PageItem {
    data class Page(val url: String, val chapterIndex: Int) : PageItem()
    data class ChapterTransition(val fromChapter: Int, val toChapter: Int) : PageItem()
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :mangaworld:shared:compileKotlinJvm --quiet
```

Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 3: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/PageItem.kt
git commit -m "feat: add PageItem sealed class for infinite scroll data model"
```

---

## Task 2: Update `ReadViewModel.kt`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt`

The goal is to replace `pageList: SnapshotStateList<String>` and `isLoadingPages: Boolean` with `pageItems: SnapshotStateList<PageItem>`, `loadingChapters: Set<Int>`, and a `loadedChapterWindow: ArrayDeque<Int>`. All chapter loading goes through new focused methods.

- [ ] **Step 1: Replace the field declarations**

Remove these lines (roughly lines 121–122):
```kotlin
val pageList = mutableStateListOf<String>()
var isLoadingPages by mutableStateOf(false)
```

Add in their place:
```kotlin
val pageItems = mutableStateListOf<PageItem>()
var loadingChapters by mutableStateOf(emptySet<Int>())
val isLoadingPages: Boolean get() = loadingChapters.isNotEmpty()

val loadedChapterWindow = ArrayDeque<Int>()
private companion object {
    const val WINDOW_SIZE = 3
}
```

- [ ] **Step 2: Replace `loadPages()` with `loadInitialChapter()`**

Remove the existing `loadPages()` method and replace with:

```kotlin
private fun loadInitialChapter() {
    val flow = modelPath ?: return
    val chapterIndex = currentChapter
    loadedChapterWindow.clear()
    loadedChapterWindow.addLast(chapterIndex)
    flow
        .onStart {
            loadingChapters = loadingChapters + chapterIndex
            pageItems.clear()
        }
        .catch { exceptionDao.insertException(it) }
        .onEach { urls ->
            pageItems.addAll(urls.map { PageItem.Page(it, chapterIndex) })
            heatMapDao.upsertHeatMap()
        }
        .onCompletion { loadingChapters = loadingChapters - chapterIndex }
        .launchIn(viewModelScope)
}
```

- [ ] **Step 3: Update `init` to call `loadInitialChapter()`**

Change line (currently calls `loadPages(modelPath)`):
```kotlin
loadPages(modelPath)
```
to:
```kotlin
loadInitialChapter()
```

- [ ] **Step 4: Add `appendChapter()`**

Add this method after `loadInitialChapter()`:

```kotlin
fun appendChapter(chapterIndex: Int) {
    if (chapterIndex < 0 || chapterIndex > list.lastIndex) return
    if (chapterIndex in loadedChapterWindow) return

    viewModelScope.launch {
        // Evict oldest loaded chapter if window is full
        if (loadedChapterWindow.size >= WINDOW_SIZE) {
            val dropped = loadedChapterWindow.removeFirst()
            val firstKeptIdx = pageItems.indexOfFirst {
                it is PageItem.Page && it.chapterIndex != dropped
            }
            if (firstKeptIdx > 0) repeat(firstKeptIdx) { pageItems.removeAt(0) }
        }

        val fromChapter = loadedChapterWindow.last()
        loadedChapterWindow.addLast(chapterIndex)
        loadingChapters = loadingChapters + chapterIndex

        pageItems.add(PageItem.ChapterTransition(fromChapter, chapterIndex))

        list.getOrNull(chapterIndex)
            ?.getChapterInfo()
            ?.map { storages ->
                headers.putAll(storages.flatMap { h -> h.headers.toList() })
                storages.mapNotNull(KmpStorage::link)
            }
            ?.catch { exceptionDao.insertException(it) }
            ?.onEach { urls ->
                pageItems.addAll(urls.map { PageItem.Page(it, chapterIndex) })
                heatMapDao.upsertHeatMap()
                list.getOrNull(chapterIndex)?.let { item ->
                    if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                        favoritesRepository.addWatched(
                            ChapterWatched(item.url, item.name, mangaUrl)
                        )
                    }
                }
            }
            ?.onCompletion { loadingChapters = loadingChapters - chapterIndex }
            ?.launchIn(viewModelScope)

        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
    }
}
```

- [ ] **Step 5: Add `prependChapter()`**

Add this suspend method after `appendChapter()`:

```kotlin
suspend fun prependChapter(chapterIndex: Int): Int {
    if (chapterIndex < 0 || chapterIndex > list.lastIndex) return 0
    if (chapterIndex in loadedChapterWindow) return 0

    // Evict newest loaded chapter if window is full
    if (loadedChapterWindow.size >= WINDOW_SIZE) {
        val dropped = loadedChapterWindow.removeLast()
        // Find the ChapterTransition that leads into the dropped chapter and remove from there onward
        val removeFrom = pageItems.indexOfFirst {
            it is PageItem.ChapterTransition && it.toChapter == dropped
        }.takeIf { it >= 0 } ?: pageItems.indexOfFirst {
            it is PageItem.Page && it.chapterIndex == dropped
        }
        if (removeFrom >= 0) {
            while (pageItems.size > removeFrom) pageItems.removeAt(removeFrom)
        }
    }

    val toChapter = loadedChapterWindow.first()
    loadedChapterWindow.addFirst(chapterIndex)
    loadingChapters = loadingChapters + chapterIndex

    val newPages = mutableListOf<PageItem>()
    list.getOrNull(chapterIndex)
        ?.getChapterInfo()
        ?.map { storages ->
            headers.putAll(storages.flatMap { h -> h.headers.toList() })
            storages.mapNotNull(KmpStorage::link)
        }
        ?.catch { exceptionDao.insertException(it) }
        ?.collect { urls ->
            newPages.addAll(urls.map { PageItem.Page(it, chapterIndex) })
            heatMapDao.upsertHeatMap()
            list.getOrNull(chapterIndex)?.let { item ->
                if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                    favoritesRepository.addWatched(
                        ChapterWatched(item.url, item.name, mangaUrl)
                    )
                }
            }
        }

    loadingChapters = loadingChapters - chapterIndex

    val insertedItems: List<PageItem> = newPages + PageItem.ChapterTransition(chapterIndex, toChapter)
    pageItems.addAll(0, insertedItems)
    addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
    return insertedItems.size
}
```

- [ ] **Step 6: Add `updateCurrentChapter()`**

```kotlin
fun updateCurrentChapter(chapterIndex: Int) {
    if (chapterIndex == currentChapter) return
    currentChapter = chapterIndex
}
```

- [ ] **Step 7: Rewrite `addChapterToWatched()` for jump navigation**

Replace the existing `addChapterToWatched()` body with a full reset + reload:

```kotlin
fun addChapterToWatched(newChapter: Int, chapter: () -> Unit) {
    currentChapter = newChapter
    addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
    list.getOrNull(newChapter)?.let { item ->
        viewModelScope.launch {
            if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                favoritesRepository.addWatched(ChapterWatched(item.url, item.name, mangaUrl))
            }
            withContext(Dispatchers.Main) { chapter() }
        }

        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(newChapter)
        item.getChapterInfo()
            .map { storages ->
                headers.putAll(storages.flatMap { h -> h.headers.toList() })
                storages.mapNotNull(KmpStorage::link)
            }
            .onStart {
                loadingChapters = loadingChapters + newChapter
                pageItems.clear()
            }
            .catch { exceptionDao.insertException(it) }
            .onEach { urls ->
                pageItems.addAll(urls.map { PageItem.Page(it, newChapter) })
                heatMapDao.upsertHeatMap()
            }
            .onCompletion { loadingChapters = loadingChapters - newChapter }
            .launchIn(viewModelScope)
    }
}
```

- [ ] **Step 8: Rewrite `refresh()`**

```kotlin
fun refresh() {
    headers.clear()
    val chapterIndex = currentChapter
    loadedChapterWindow.clear()
    loadedChapterWindow.addLast(chapterIndex)
    list.getOrNull(chapterIndex)
        ?.getChapterInfo()
        ?.map { storages ->
            headers.putAll(storages.flatMap { h -> h.headers.toList() })
            storages.mapNotNull(KmpStorage::link)
        }
        ?.onStart {
            loadingChapters = loadingChapters + chapterIndex
            pageItems.clear()
        }
        ?.catch { exceptionDao.insertException(it) }
        ?.onEach { urls ->
            pageItems.addAll(urls.map { PageItem.Page(it, chapterIndex) })
            heatMapDao.upsertHeatMap()
        }
        ?.onCompletion { loadingChapters = loadingChapters - chapterIndex }
        ?.launchIn(viewModelScope)
}
```

- [ ] **Step 9: Add missing imports**

Ensure these are present at the top of `ReadViewModel.kt`:
```kotlin
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
```

- [ ] **Step 10: Verify it compiles**

```bash
./gradlew :mangaworld:shared:compileKotlinJvm --quiet
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReadViewModel.kt
git commit -m "feat: replace pageList with pageItems and add infinite scroll load methods"
```

---

## Task 3: Add `ChapterTransitionItem` to `Pages.kt`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/Pages.kt`

- [ ] **Step 1: Add `ChapterTransitionItem` composable**

Add this new composable at the top of `Pages.kt` (after imports, before `LastPageReached`):

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChapterTransitionItem(
    fromChapterName: String,
    toChapterName: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Finished",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    fromChapterName,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            when {
                toChapterName == null -> Text(
                    "No more chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Next",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            toChapterName,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isLoading) {
                        CircularWavyProgressIndicator()
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add `HorizontalDivider` import**

At the top of `Pages.kt`, ensure this import is present:
```kotlin
import androidx.compose.material3.HorizontalDivider
```

- [ ] **Step 3: Remove `ChangeChapterSwipe` and `SwipeUpGesture`**

Delete the entire `ChangeChapterSwipe` composable and the `SwipeUpGesture` enum (they are no longer used — infinite scroll replaces the swipe gesture for chapter navigation at chapter end).

Delete these blocks:
- The `@Composable internal fun ChangeChapterSwipe(...)` function (roughly 100 lines)
- `enum class SwipeUpGesture { Up, Settled, }`

- [ ] **Step 4: Remove `LastPageReached`**

Delete the `@Composable internal fun LastPageReached(...)` function — `ChapterTransitionItem` replaces it entirely.

- [ ] **Step 5: Remove now-unused imports from `Pages.kt`**

Remove these imports that were only used by the deleted composables:
```kotlin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pages
import androidx.compose.ui.geometry.Offset
```

- [ ] **Step 6: Verify it compiles**

```bash
./gradlew :mangaworld:shared:compileKotlinJvm --quiet
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/Pages.kt
git commit -m "feat: add ChapterTransitionItem, remove LastPageReached and ChangeChapterSwipe"
```

---

## Task 4: Update `ReaderCompose.kt`

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt`

This is the largest task. Work through it section by section.

- [ ] **Step 1: Update `pagerState` page count**

Find:
```kotlin
val pagerState = rememberPagerState(
    initialPage = 0,
    initialPageOffsetFraction = 0f
) { pages.size + 1 }
```

Replace with:
```kotlin
val pagerState = rememberPagerState(
    initialPage = 0,
    initialPageOffsetFraction = 0f
) { viewModel.pageItems.size.coerceAtLeast(1) }
```

- [ ] **Step 2: Remove the local `pages` variable**

Find and remove this line (which delegates to `viewModel.pageList`):
```kotlin
val pages = viewModel.pageList
```

The composable will now reference `viewModel.pageItems` directly everywhere.

- [ ] **Step 3: Update `currentPage` derivation**

The existing `currentPage` gives the global list index. For the bottom bar we now want the within-chapter page. Replace the `currentPage` block and add a `currentChapterPageOffset` helper:

```kotlin
val currentChapterPageOffset by remember {
    derivedStateOf {
        viewModel.pageItems.indexOfFirst {
            it is PageItem.Page && it.chapterIndex == viewModel.currentChapter
        }.coerceAtLeast(0)
    }
}

val currentPage by remember {
    derivedStateOf {
        val globalIndex = when (readerType) {
            ReaderType.List, ReaderType.FlipPager -> listState.firstVisibleItemIndex
            ReaderType.Pager -> pagerState.currentPage
            ReaderType.CurlPager -> curlState.current
        }
        (globalIndex - currentChapterPageOffset).coerceAtLeast(0)
    }
}

val pagesInCurrentChapter by remember {
    derivedStateOf {
        viewModel.pageItems.count {
            it is PageItem.Page && it.chapterIndex == viewModel.currentChapter
        }.coerceAtLeast(1)
    }
}
```

- [ ] **Step 4: Update `listShowItems` and `pagerShowItems`**

Find:
```kotlin
val listShowItems by remember { derivedStateOf { listState.isScrolledToTheEnd() && readerType == ReaderType.List } }
val pagerShowItems by remember { derivedStateOf { pagerState.currentPage >= pages.size && readerType != ReaderType.List } }
```

Replace with:
```kotlin
val listShowItems by remember { derivedStateOf { listState.isScrolledToTheEnd() && readerType == ReaderType.List } }
val pagerShowItems by remember {
    derivedStateOf {
        readerType != ReaderType.List &&
        viewModel.pageItems.getOrNull(pagerState.currentPage) is PageItem.ChapterTransition
    }
}
```

- [ ] **Step 5: Update `listIndex` LaunchedEffect to use `PageItem`**

Find the `LaunchedEffect(listIndex, pagerState.currentPage, viewModel.showInfo)` block:
```kotlin
val listIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 } }
LaunchedEffect(listIndex, pagerState.currentPage, viewModel.showInfo) {
    if (viewModel.firstScroll && (listIndex > 0 || pagerState.currentPage > 0)) {
        viewModel.showInfo = false
        viewModel.firstScroll = false
    }
}
```

Keep this block unchanged — it still works correctly with the global item index.

- [ ] **Step 6: Add infinite scroll trigger `LaunchedEffect` for `ListView`**

After the existing `LaunchedEffect(curlState)` block, add:

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo }
        .collect { visibleItems ->
            visibleItems.forEach { itemInfo ->
                val item = viewModel.pageItems.getOrNull(itemInfo.index) ?: return@forEach
                if (item is PageItem.ChapterTransition) {
                    when {
                        item.toChapter < item.fromChapter -> viewModel.appendChapter(item.toChapter)
                        item.toChapter > item.fromChapter -> {
                            val inserted = viewModel.prependChapter(item.toChapter)
                            if (inserted > 0) {
                                listState.requestScrollToItem(
                                    index = listState.firstVisibleItemIndex + inserted,
                                    scrollOffset = listState.firstVisibleItemScrollOffset
                                )
                            }
                        }
                    }
                }
            }
        }
}
```

- [ ] **Step 7: Add infinite scroll trigger `LaunchedEffect` for `PagerView`**

Also after the existing `LaunchedEffect(curlState)` block, add:

```kotlin
LaunchedEffect(pagerState, viewModel.pageItems.size) {
    snapshotFlow { pagerState.currentPage }
        .collect { page ->
            val item = viewModel.pageItems.getOrNull(page)
            if (item is PageItem.ChapterTransition) {
                when {
                    item.toChapter < item.fromChapter -> viewModel.appendChapter(item.toChapter)
                    item.toChapter > item.fromChapter -> {
                        val inserted = viewModel.prependChapter(item.toChapter)
                        if (inserted > 0) {
                            pagerState.scrollToPage(pagerState.currentPage + inserted)
                        }
                    }
                }
            }
        }
}
```

- [ ] **Step 8: Add `currentChapter` derivation `LaunchedEffect`**

After the trigger effects, add:

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index }
        .collect { index ->
            (viewModel.pageItems.getOrNull(index ?: return@collect) as? PageItem.Page)
                ?.chapterIndex
                ?.let { viewModel.updateCurrentChapter(it) }
        }
}
```

> **Note:** Multiple `LaunchedEffect(listState)` blocks are allowed — each is an independent composable with its own coroutine.

- [ ] **Step 9: Update `FloatingBottomBar` call site**

Find the `FloatingBottomBar(...)` call and update the `pages` parameter from `animateIntAsState(pages.size).value` to use `pagesInCurrentChapter`:

```kotlin
pages = animateIntAsState(pagesInCurrentChapter).value,
```

- [ ] **Step 10: Update the `ModalBottomSheet` `SheetView` call**

Find the `SheetView(...)` call inside the `ModalBottomSheet`. Update the `pages` parameter to pass only the current chapter's page URLs, and fix `onPageChange` to convert local index to global index:

```kotlin
ModalBottomSheet(
    onDismissRequest = { showBottomSheet = false },
    containerColor = MaterialTheme.colorScheme.surface,
) {
    val currentChapterPageUrls by remember {
        derivedStateOf {
            viewModel.pageItems
                .filterIsInstance<PageItem.Page>()
                .filter { it.chapterIndex == viewModel.currentChapter }
                .map { it.url }
        }
    }
    SheetView(
        readVm = viewModel,
        onSheetHide = { showBottomSheet = false },
        currentPage = currentPage,
        pages = currentChapterPageUrls,
        onPageChange = { localIndex ->
            val globalIndex = currentChapterPageOffset + localIndex
            when (readerType) {
                ReaderType.List, ReaderType.FlipPager -> listState.animateScrollToItem(globalIndex)
                ReaderType.Pager -> pagerState.animateScrollToPage(globalIndex)
                ReaderType.CurlPager -> curlState.snapTo(globalIndex)
            }
        },
    )
}
```

- [ ] **Step 11: Update `ListView` call site**

Find the `ReaderType.List ->` branch and update the `ListView` call to pass `pageItems` instead of `pages`:

```kotlin
ReaderType.List -> {
    ListView(
        listState = listState,
        pageItems = viewModel.pageItems,
        readVm = viewModel,
        itemSpacing = spacing,
        colorFilter = colorFilter,
        paddingValues = PaddingValues(
            top = if (viewModel.pageItems.isNotEmpty()) 0.dp else p.calculateTopPadding(),
            bottom = p.calculateBottomPadding()
        ).animate(),
        imageLoaderType = imageLoaderType,
    )
}
```

- [ ] **Step 12: Update `PagerView` call site**

Find the `ReaderType.Pager ->` branch and update the `PagerView` call to pass `pageItems`:

```kotlin
ReaderType.Pager -> {
    PagerView(
        pagerState = pagerState,
        pageItems = viewModel.pageItems,
        vm = viewModel,
        colorFilter = colorFilter,
        itemSpacing = spacing,
        imageLoaderType = imageLoaderType,
    )
}
```

- [ ] **Step 13: Rewrite `ListView` composable signature and body**

Replace the entire `ListView` composable:

```kotlin
@Composable
fun ListView(
    listState: LazyListState,
    pageItems: List<PageItem>,
    readVm: ReadViewModel,
    itemSpacing: Dp,
    paddingValues: PaddingValues,
    imageLoaderType: ImageLoaderType,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        contentPadding = paddingValues,
    ) { reader(pageItems, readVm, imageLoaderType, colorFilter) }
}
```

- [ ] **Step 14: Rewrite `PagerView` composable signature and body**

Replace the entire `PagerView` composable:

```kotlin
@Composable
fun PagerView(
    pagerState: PagerState,
    pageItems: List<PageItem>,
    vm: ReadViewModel,
    itemSpacing: Dp,
    imageLoaderType: ImageLoaderType,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier,
) {
    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        pageSpacing = itemSpacing,
        beyondViewportPageCount = 1,
        key = { it }
    ) { page ->
        when (val item = pageItems.getOrNull(page)) {
            is PageItem.Page -> ChapterPage(
                chapterLink = { item.url },
                isDownloaded = vm.isDownloaded,
                headers = vm.headers,
                contentScale = ContentScale.Fit,
                imageLoaderType = imageLoaderType,
                colorFilter = colorFilter
            )
            is PageItem.ChapterTransition -> Box(modifier = Modifier.fillMaxSize()) {
                ChapterTransitionItem(
                    fromChapterName = vm.list.getOrNull(item.fromChapter)?.name.orEmpty(),
                    toChapterName = vm.list.getOrNull(item.toChapter)?.name,
                    isLoading = vm.loadingChapters.contains(item.toChapter),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            null -> Box(modifier = Modifier.fillMaxSize())
        }
    }
}
```

- [ ] **Step 15: Rewrite `reader()` LazyListScope extension**

Replace the `private fun LazyListScope.reader(...)` function:

```kotlin
private fun LazyListScope.reader(
    pageItems: List<PageItem>,
    vm: ReadViewModel,
    imageLoaderType: ImageLoaderType,
    colorFilter: ColorFilter? = null,
) {
    pageItems.forEachIndexed { index, item ->
        when (item) {
            is PageItem.Page -> item(
                key = "${item.url}${item.chapterIndex}$index",
                contentType = "page"
            ) {
                ChapterPage(
                    chapterLink = { item.url },
                    isDownloaded = vm.isDownloaded,
                    headers = vm.headers,
                    contentScale = ContentScale.FillWidth,
                    imageLoaderType = imageLoaderType,
                    colorFilter = colorFilter
                )
            }
            is PageItem.ChapterTransition -> item(
                key = "transition_${item.fromChapter}_${item.toChapter}",
                contentType = "transition"
            ) {
                ChapterTransitionItem(
                    fromChapterName = vm.list.getOrNull(item.fromChapter)?.name.orEmpty(),
                    toChapterName = vm.list.getOrNull(item.toChapter)?.name,
                    isLoading = vm.loadingChapters.contains(item.toChapter),
                )
            }
        }
    }
}
```

- [ ] **Step 16: Remove the `FlipPagerView` and `CurlPagerView` `LastPageReached` references**

`FlipPagerView` and `CurlPagerView` still use `pages: List<String>` (they are out of scope for infinite scroll). They currently call `LastPageReached` which has been deleted. Replace those `LastPageReached` calls with a simple placeholder that shows the chapter name and loading state:

In `FlipPagerView`, change the `?: Box(...)` block to:
```kotlin
?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    if (vm.isLoadingPages) {
        CircularWavyProgressIndicator()
    } else {
        Text(
            "End of chapter",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
```

Do the same for `CurlPagerView`.

Also update the `FlipPagerView` and `CurlPagerView` call sites in `ReadView` to pass `pages` from `viewModel.pageItems`:

```kotlin
// Derive a flat list for FlipPager and CurlPager (single-chapter, out of scope for infinite)
val pages by remember {
    derivedStateOf {
        viewModel.pageItems.filterIsInstance<PageItem.Page>()
            .filter { it.chapterIndex == viewModel.currentChapter }
            .map { it.url }
    }
}
```

Add this `val pages` derivation above the `Crossfade` block, then use it in the `FlipPagerView` and `CurlPagerView` call sites (their signatures are unchanged).

- [ ] **Step 17: Add `CircularWavyProgressIndicator` and `Text` imports if missing**

Ensure `ReaderCompose.kt` has:
```kotlin
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Text
```

- [ ] **Step 18: Final compile check**

```bash
./gradlew :mangaworld:shared:compileKotlinJvm --quiet
```

Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 19: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt
git commit -m "feat: wire infinite scroll into ListView and PagerView"
```

---

## Self-Review Notes

**Spec coverage check:**
- ✅ `PageItem` sealed class — Task 1
- ✅ `pageItems` replaces `pageList` — Task 2 Step 1
- ✅ `loadingChapters` replaces `isLoadingPages` — Task 2 Step 1
- ✅ `loadedChapterWindow` + `WINDOW_SIZE` — Task 2 Step 1
- ✅ `loadInitialChapter()` — Task 2 Steps 2–3
- ✅ `appendChapter()` with eviction — Task 2 Step 4
- ✅ `prependChapter()` with eviction + insertedCount — Task 2 Step 5
- ✅ `updateCurrentChapter()` — Task 2 Step 6
- ✅ `addChapterToWatched()` reset behavior — Task 2 Step 7
- ✅ `refresh()` reset behavior — Task 2 Step 8
- ✅ `ChapterTransitionItem` — Task 3 Step 1
- ✅ `ChangeChapterSwipe` removed — Task 3 Steps 3–4
- ✅ `ListView` trigger + `currentChapter` derivation — Task 4 Steps 6, 8
- ✅ `PagerView` trigger — Task 4 Step 7
- ✅ Scroll jump compensation (list + pager) — Task 4 Steps 6, 7
- ✅ Bottom bar page count within-chapter — Task 4 Steps 3, 9
- ✅ Page thumbnail sheet filtered to current chapter — Task 4 Step 10
- ✅ FlipPager/CurlPager out of scope but compile-safe — Task 4 Step 16
