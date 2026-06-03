# Bookmark Chapters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add chapter bookmarking to the manga reader — users bookmark chapters from the Details screen, view them in a dedicated screen accessible from Settings, and export/import them via the existing ZIP backup.

**Architecture:** New `BookmarkDatabase` (Room + FTS4) added to the `favoritesdatabase` module following the exact same pattern as `HistoryDatabase`. `BookmarkRepository` lives in `kmpuiviews/repository`. `DetailsViewModel` gains bookmark state and a toggle method. `BookmarkChaptersViewModel` drives the new expandable-group Bookmarks screen navigable from Settings. Zipper export/import uses the existing `handlers` map with a new `"bookmarked_chapters.json"` entry.

**Tech Stack:** Room KMP, `@Fts4`, Kotlin Coroutines/Flow, Koin (`singleOf`/`viewModelOf`), Compose Multiplatform, Nav3 (`TopLevelBackStack`), `CustomKamelImage`, kotlinx.serialization, Material 3.

---

## File Map

### New files
| File | Responsibility |
|------|----------------|
| `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/BookmarkDatabase.kt` | `BookmarkedChapter` entity, `BookmarkedChapterFts` FTS entity, `BookmarkDao`, `BookmarkDatabase` class |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/BookmarkRepository.kt` | Wraps `BookmarkDao`; exposes Flows + suspend mutations; local-only (no Firebase) |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/bookmarks/BookmarkChaptersViewModel.kt` | `MutableStateFlow` for search/sort, grouped `StateFlow<Map<String, List<BookmarkedChapter>>>`, remove action |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/bookmarks/BookmarkScreen.kt` | Bookmarks screen — expandable manga groups, search bar, sort chips, empty state, export/import actions |

### Modified files
| File | Change summary |
|------|----------------|
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt` | Add `BookmarkDatabase` + `BookmarkDao` singletons |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.kt` | Add `BookmarkRepository` singleton |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt` | Register `BookmarkChaptersViewModel` |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt` | Add `BookmarkScreen` NavKey |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt` | Add `entry<Screen.BookmarkScreen>` |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt` | Add `fun bookmarks()` to interface |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt` | Implement `bookmarks()` |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt` | Add `BookmarkRepository` param, `bookmarkedChapterUrls` state, `toggleBookmark()` |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt` | Add `bookmarkChapter` to `DetailsActions`; add `isBookmarked` param + trailing icon + long-press entry to `ChapterItem` |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt` | Add Bookmarks `segmentedListItem` row |
| `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt` | Add `BookmarkDao` constructor param + `"bookmarked_chapters.json"` handler |

---

## Task 1: BookmarkDatabase — Entities, DAO, Database Class

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/BookmarkDatabase.kt`

- [ ] **Step 1: Create `BookmarkDatabase.kt`**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "bookmarked_chapters")
@Serializable
data class BookmarkedChapter(
    @PrimaryKey val chapterUrl: String,
    val chapterName: String,
    val parentUrl: String,
    val parentTitle: String,
    val parentImageUrl: String,
    val source: String,
    val timestamp: Long, // epoch millis
)

@Entity(tableName = "bookmarked_chapters_fts")
@Fts4(contentEntity = BookmarkedChapter::class)
data class BookmarkedChapterFts(
    val chapterName: String,
    val parentTitle: String,
)

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedChapter)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkedChapter)

    @Query("DELETE FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl")
    suspend fun deleteBookmarkByUrl(chapterUrl: String)

    @Query("SELECT * FROM bookmarked_chapters ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters WHERE parentUrl = :parentUrl")
    fun getBookmarksForDetail(parentUrl: String): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl")
    fun getBookmark(chapterUrl: String): Flow<BookmarkedChapter?>

    @Query("SELECT * FROM bookmarked_chapters WHERE chapterUrl IN (:urls)")
    fun getBookmarksForChapters(urls: List<String>): Flow<List<BookmarkedChapter>>

    @Query("""
        SELECT * FROM bookmarked_chapters WHERE rowid IN (
            SELECT rowid FROM bookmarked_chapters_fts
            WHERE bookmarked_chapters_fts MATCH :query
        ) ORDER BY timestamp DESC
    """)
    fun searchBookmarks(query: String): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters")
    suspend fun getAllBookmarksSync(): List<BookmarkedChapter>
}

@Database(
    entities = [BookmarkedChapter::class, BookmarkedChapterFts::class],
    version = 1,
    exportSchema = true,
)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): BookmarkDatabase =
            databaseBuilder
                .build<BookmarkDatabase>("bookmarks.db")
                .build()
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :favoritesdatabase:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. If Room annotation processor errors appear, check that `@Fts4` import is `androidx.room.Fts4` and that both entities are listed in `@Database(entities = [...])`.

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/BookmarkDatabase.kt
git commit -m "feat(bookmarks): add BookmarkDatabase, BookmarkDao, and BookmarkedChapter entities"
```

---

## Task 2: BookmarkRepository

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/BookmarkRepository.kt`

- [ ] **Step 1: Create `BookmarkRepository.kt`**

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val dao: BookmarkDao) {

    fun getAllBookmarks(): Flow<List<BookmarkedChapter>> = dao.getAllBookmarks()

    fun getBookmarksForDetail(parentUrl: String): Flow<List<BookmarkedChapter>> =
        dao.getBookmarksForDetail(parentUrl)

    fun getBookmark(chapterUrl: String): Flow<BookmarkedChapter?> =
        dao.getBookmark(chapterUrl)

    fun searchBookmarks(query: String): Flow<List<BookmarkedChapter>> =
        dao.searchBookmarks(query)

    suspend fun insertBookmark(bookmark: BookmarkedChapter) =
        dao.insertBookmark(bookmark)

    suspend fun deleteBookmark(chapterUrl: String) =
        dao.deleteBookmarkByUrl(chapterUrl)

    suspend fun getAllBookmarksSync(): List<BookmarkedChapter> =
        dao.getAllBookmarksSync()
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/BookmarkRepository.kt
git commit -m "feat(bookmarks): add BookmarkRepository"
```

---

## Task 3: DI Wiring

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt`

- [ ] **Step 1: Add `BookmarkDatabase` and `BookmarkDao` to `DatabaseModule.kt`**

In `DatabaseModule.kt`, inside the `databases` module block, add after the last existing `single` line:

```kotlin
single<BookmarkDatabase> { BookmarkDatabase.getInstance(get()) }
single<BookmarkDao> { get<BookmarkDatabase>().bookmarkDao() }
```

Add import at top of file: `import com.programmersbox.favoritesdatabase.BookmarkDatabase`
Add import at top of file: `import com.programmersbox.favoritesdatabase.BookmarkDao`

- [ ] **Step 2: Add `BookmarkRepository` to `RepositoryModule.kt`**

In `RepositoryModule.kt`, inside the `repositories` module block, add:

```kotlin
singleOf(::BookmarkRepository)
```

Add import: `import com.programmersbox.kmpuiviews.repository.BookmarkRepository`

- [ ] **Step 3: Add `BookmarkChaptersViewModel` to `ViewModelModule.kt`**

In `ViewModelModule.kt`, inside the module block, add:

```kotlin
viewModelOf(::BookmarkChaptersViewModel)
```

Add import: `import com.programmersbox.kmpuiviews.presentation.bookmarks.BookmarkChaptersViewModel`

(The class doesn't exist yet — this will cause a compile error until Task 7. Add the line now and it will resolve then. If the build must stay green, add this line in Task 7 instead.)

- [ ] **Step 4: Verify DI wiring compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL` (or fails only on the missing `BookmarkChaptersViewModel` class if you added that line early).

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt
git commit -m "feat(bookmarks): wire BookmarkDatabase, BookmarkRepository, and BookmarkChaptersViewModel into DI"
```

---

## Task 4: Navigation — Screen Key, Nav Graph Entry, NavigationActions

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt`

- [ ] **Step 1: Add `BookmarkScreen` NavKey to `Screen.kt`**

Inside the `Screen` sealed class, alongside the other `data object` entries, add:

```kotlin
@Serializable
data object BookmarkScreen : Screen("bookmarks")
```

- [ ] **Step 2: Add `fun bookmarks()` to `NavigationActions` interface**

Open `NavigationActions.kt`. Add to the interface:

```kotlin
fun bookmarks()
```

- [ ] **Step 3: Implement `bookmarks()` in `Navigation3Actions`**

In `Navigation3Actions.kt`, add:

```kotlin
override fun bookmarks() {
    navBackStack.add(Screen.BookmarkScreen)
}
```

- [ ] **Step 4: Add nav graph entry in `Nav3Graph.kt`**

Inside the `entryProvider` / `entryGraph` DSL block, alongside the other `entry<>` calls, add:

```kotlin
entry<Screen.BookmarkScreen> {
    BookmarkScreen(
        onBackPress = { navBackStack.removeLastOrNull() },
    )
}
```

Add import: `import com.programmersbox.kmpuiviews.presentation.bookmarks.BookmarkScreen`

`navBackStack` is available in the Nav3Graph DSL scope — check how other entries invoke back navigation (e.g., `navBackStack.removeLastOrNull()`) and use the same call.

(The `BookmarkScreen` composable doesn't exist yet — this will resolve in Task 8. If the build must stay green, add this line in Task 8 instead.)

- [ ] **Step 5: Verify navigation layer compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL` (fails only on missing `BookmarkScreen` composable if added early).

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt
git commit -m "feat(bookmarks): add BookmarkScreen NavKey and navigation wiring"
```

---

## Task 5: DetailsViewModel — Bookmark State and Toggle

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt`

**Context:** `DetailsViewModel` receives `details: Screen.DetailsScreen.Details?` where `details.url` is URL-encoded. It creates `val itemModel: KmpItemModel? = details?.toItemModel(sourceRepository)` where `itemModel.url` is the decoded manga URL. Always use `itemModel?.url` (decoded) when reading/writing bookmarks so stored URLs are consistent.

- [ ] **Step 1: Add `BookmarkRepository` to `DetailsViewModel` constructor**

Add `private val bookmarkRepository: BookmarkRepository` to the constructor parameter list. Koin's `viewModelOf(::DetailsViewModel)` injects all parameters by type — no other Koin changes needed here since `BookmarkRepository` is already registered from Task 3.

Add import: `import com.programmersbox.kmpuiviews.repository.BookmarkRepository`

- [ ] **Step 2: Add `bookmarkedChapterUrls` state**

Inside the class body, alongside the existing `var info`, `var palette`, etc.:

```kotlin
var bookmarkedChapterUrls: Set<String> by mutableStateOf(emptySet())
    private set
```

- [ ] **Step 3: Collect bookmark state in `init`**

Inside the `init` block, after the existing flow chains, add:

```kotlin
itemModel?.url?.let { mangaUrl ->
    bookmarkRepository.getBookmarksForDetail(mangaUrl)
        .onEach { bookmarks ->
            bookmarkedChapterUrls = bookmarks.map { it.chapterUrl }.toHashSet()
        }
        .launchIn(viewModelScope)
}
```

Add imports:
```kotlin
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
```

(These are likely already imported — check before adding duplicates.)

- [ ] **Step 4: Add `toggleBookmark()`**

Add this method to the class:

```kotlin
fun toggleBookmark(chapter: KmpChapterModel) {
    val mangaUrl = itemModel?.url ?: return
    viewModelScope.launch(Dispatchers.IO) {
        if (chapter.url in bookmarkedChapterUrls) {
            bookmarkRepository.deleteBookmark(chapter.url)
        } else {
            bookmarkRepository.insertBookmark(
                BookmarkedChapter(
                    chapterUrl = chapter.url,
                    chapterName = chapter.name,
                    parentUrl = mangaUrl,
                    parentTitle = itemModel?.title ?: "",
                    parentImageUrl = itemModel?.imageUrl ?: "",
                    source = itemModel?.source?.serviceName ?: "",
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }
}
```

Add imports:
```kotlin
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
```

(Check for existing imports before adding.)

- [ ] **Step 5: Verify DetailsViewModel compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt
git commit -m "feat(bookmarks): add bookmark state and toggleBookmark() to DetailsViewModel"
```

---

## Task 6: DetailsScreen — DetailsActions + ChapterItem Bookmark UI

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt`

**Context:** `DetailsActions` is a data class in this file. `ChapterItem` is a Composable in this file (around line 589–770) that uses `detailsActions` and renders each chapter row. The chapter row uses `ListItem` with `trailingContent`. The long-press / options sheet is controlled by a state variable created via `chapterItemOptions(...)`. Read lines 589–770 of this file before editing to understand the exact structure of `trailingContent` and the options mechanism.

- [ ] **Step 1: Add `bookmarkChapter` to `DetailsActions`**

Find the `DetailsActions` data class in `DetailsScreen.kt`. Add the new field with a default so all existing construction sites compile before they're updated:

```kotlin
data class DetailsActions(
    // ... existing fields unchanged ...
    val bookmarkChapter: (KmpChapterModel) -> Unit = {},
)
```

- [ ] **Step 2: Add `isBookmarked: Boolean` parameter to `ChapterItem`**

Find the `ChapterItem` function signature and add:

```kotlin
@Composable
fun ChapterItem(
    c: KmpChapterModel,
    read: List<ChapterWatched>,
    isBookmarked: Boolean,          // add this
    showDownload: () -> Boolean,
    swipeBehavior: DetailsChapterSwipeBehaviorHandle,
    detailsActions: DetailsActions,
    downloadUiState: ChapterDownloadUiState = ChapterDownloadUiState.None,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 3: Add trailing bookmark `IconButton` inside `ChapterItem`**

In `ChapterItem`, inside the `trailingContent` lambda of `ListItem`, add the bookmark icon button alongside the existing download/share content. The exact position depends on the current content — place it as the last icon before or after the existing trailing content:

```kotlin
IconButton(onClick = { detailsActions.bookmarkChapter(c) }) {
    Icon(
        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
        contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark chapter",
        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

Add imports:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
```

(Check for existing imports before adding.)

- [ ] **Step 4: Add bookmark entry to the long-press options**

Read the `chapterItemOptions` implementation carefully. It returns some form of options state. Find where `markAsRead`, `download`, and `share` options are listed and add alongside them:

If the options use a `DropdownMenu` with `DropdownMenuItem`:
```kotlin
DropdownMenuItem(
    text = { Text(if (isBookmarked) "Remove bookmark" else "Bookmark") },
    leadingIcon = {
        Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = null,
        )
    },
    onClick = {
        detailsActions.bookmarkChapter(c)
        // dismiss the menu using whatever dismiss mechanism chapterItemOptions provides
    },
)
```

If the options use a `ModalBottomSheet` with `ListItem` rows, follow the same pattern as the existing `ListItem` rows in the sheet.

- [ ] **Step 5: Update all `ChapterItem` call sites to pass `isBookmarked`**

Run:
```bash
grep -rn "ChapterItem(" kmpuiviews/
```

For each call site (typically in `DetailsPortrait.kt` or `DetailsLandscape.kt` inside `items(info.chapters)`), add the `isBookmarked` argument. The `bookmarkedChapterUrls` set from the ViewModel is available at the call site:

```kotlin
ChapterItem(
    c = chapter,
    read = viewModel.chapters,
    isBookmarked = chapter.url in viewModel.bookmarkedChapterUrls,  // add this
    // ... rest unchanged
)
```

- [ ] **Step 6: Update `DetailsActions` construction sites to pass `bookmarkChapter`**

Run:
```bash
grep -rn "DetailsActions(" kmpuiviews/
```

For each construction site, add:
```kotlin
DetailsActions(
    // ... existing fields unchanged ...
    bookmarkChapter = { viewModel.toggleBookmark(it) },
)
```

- [ ] **Step 7: Verify Details screen compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt
# Add any other files changed (DetailsPortrait, DetailsLandscape, etc.)
git commit -m "feat(bookmarks): add trailing bookmark icon and long-press option to ChapterItem"
```

---

## Task 7: BookmarkChaptersViewModel

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/bookmarks/BookmarkChaptersViewModel.kt`

- [ ] **Step 1: Create `BookmarkChaptersViewModel.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BookmarkSortOrder { DATE_DESC, DATE_ASC, TITLE_AZ, MANGA_AZ }

fun String.toFtsQuery(): String =
    trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.joinToString(" ") { "$it*" }

fun List<BookmarkedChapter>.sortedByOrder(order: BookmarkSortOrder): List<BookmarkedChapter> =
    when (order) {
        BookmarkSortOrder.DATE_DESC -> sortedByDescending { it.timestamp }
        BookmarkSortOrder.DATE_ASC -> sortedBy { it.timestamp }
        BookmarkSortOrder.TITLE_AZ -> sortedBy { it.chapterName }
        BookmarkSortOrder.MANGA_AZ -> sortedBy { it.parentTitle }
    }

fun List<BookmarkedChapter>.groupByManga(): Map<String, List<BookmarkedChapter>> =
    groupBy { it.parentTitle }

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkChaptersViewModel(
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(BookmarkSortOrder.DATE_DESC)

    var searchQuery: String
        get() = _searchQuery.value
        set(value) { _searchQuery.value = value }

    var sortOrder: BookmarkSortOrder
        get() = _sortOrder.value
        set(value) { _sortOrder.value = value }

    val bookmarks: StateFlow<Map<String, List<BookmarkedChapter>>> =
        combine(
            _searchQuery.flatMapLatest { q ->
                if (q.isBlank()) bookmarkRepository.getAllBookmarks()
                else bookmarkRepository.searchBookmarks(q.toFtsQuery())
            },
            _sortOrder,
        ) { list, sort -> list.sortedByOrder(sort).groupByManga() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap(),
            )

    fun removeBookmark(chapterUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepository.deleteBookmark(chapterUrl)
        }
    }
}
```

- [ ] **Step 2: Add `BookmarkChaptersViewModel` to `ViewModelModule.kt` (if not done in Task 3)**

```kotlin
viewModelOf(::BookmarkChaptersViewModel)
```

Add import: `import com.programmersbox.kmpuiviews.presentation.bookmarks.BookmarkChaptersViewModel`

- [ ] **Step 3: Verify ViewModel compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/bookmarks/BookmarkChaptersViewModel.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt
git commit -m "feat(bookmarks): add BookmarkChaptersViewModel with search, sort, and remove"
```

---

## Task 8: BookmarkScreen Composable

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/bookmarks/BookmarkScreen.kt`

**Context:** `CustomKamelImage` is at `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/composables/imageloaders/CustomKamelImage.kt`. It takes `imageUrl: String`, `name: String`, `modifier`, `placeHolder`, `onError`, `contentScale`. Use it for manga cover images.

- [ ] **Step 1: Create `BookmarkScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.bookmarks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.CustomKamelImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    onBackPress: () -> Unit = {},
    vm: BookmarkChaptersViewModel = koinViewModel(),
) {
    val bookmarks by vm.bookmarks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Search bar
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.searchQuery = it },
                placeholder = { Text("Search bookmarks…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )

            // Sort chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookmarkSortOrder.entries.forEach { sort ->
                    FilterChip(
                        selected = vm.sortOrder == sort,
                        onClick = { vm.sortOrder = sort },
                        label = {
                            Text(
                                when (sort) {
                                    BookmarkSortOrder.DATE_DESC -> "Newest"
                                    BookmarkSortOrder.DATE_ASC -> "Oldest"
                                    BookmarkSortOrder.TITLE_AZ -> "Chapter A–Z"
                                    BookmarkSortOrder.MANGA_AZ -> "Manga A–Z"
                                }
                            )
                        },
                    )
                }
            }

            if (bookmarks.isEmpty()) {
                BookmarksEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    bookmarks.forEach { (mangaTitle, chapters) ->
                        item(key = mangaTitle) {
                            MangaBookmarkGroup(
                                mangaTitle = mangaTitle,
                                chapters = chapters,
                                onRemove = { vm.removeBookmark(it.chapterUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "No bookmarks yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Bookmark chapters from the manga details screen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MangaBookmarkGroup(
    mangaTitle: String,
    chapters: List<BookmarkedChapter>,
    onRemove: (BookmarkedChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }
    val coverUrl = chapters.firstOrNull()?.parentImageUrl.orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        // Group header
        ListItem(
            headlineContent = {
                Text(
                    mangaTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            supportingContent = {
                Text(
                    "${chapters.size} bookmark${if (chapters.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                CustomKamelImage(
                    imageUrl = coverUrl,
                    name = mangaTitle,
                    modifier = Modifier
                        .width(40.dp)
                        .height(56.dp),
                    placeHolder = {
                        rememberVectorPainter(Icons.Filled.Bookmark)
                    },
                    onError = {
                        rememberVectorPainter(Icons.Filled.Bookmark)
                    },
                    contentScale = ContentScale.Crop,
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                                  else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            },
            modifier = Modifier.clickable { expanded = !expanded },
        )

        HorizontalDivider()

        // Chapter rows
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                chapters.forEach { bookmark ->
                    BookmarkedChapterRow(
                        bookmark = bookmark,
                        onRemove = { onRemove(bookmark) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkedChapterRow(
    bookmark: BookmarkedChapter,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                bookmark.chapterName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                formatRelativeTime(bookmark.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        modifier = modifier.padding(start = 16.dp),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
```

**Note on imports:** `rememberVectorPainter` requires `androidx.compose.ui.graphics.vector.rememberVectorPainter`. `Icons.AutoMirrored.Filled.ArrowBack` requires the `material-icons-extended` dependency or use `Icons.Default.ArrowBack`. `Icons.Default.Search` similarly. Check `gradle/libs.versions.toml` and existing imports in the project for the correct icon set in use. Replace with project-standard alternatives if needed.

- [ ] **Step 2: Add `BookmarkScreen` entry to Nav3Graph (if not done in Task 4)**

In `Nav3Graph.kt`, confirm this entry is present (matches the entry added in Task 4):

```kotlin
entry<Screen.BookmarkScreen> {
    BookmarkScreen(
        onBackPress = { navBackStack.removeLastOrNull() },
    )
}
```

Look at other `entry<>` blocks in the same file to confirm the exact back-navigation call (e.g., `navBackStack.removeLastOrNull()` or a similar pop method).

- [ ] **Step 3: Verify Bookmarks screen compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. Fix any import errors for icons or Kamel.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/bookmarks/BookmarkScreen.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt
git commit -m "feat(bookmarks): add BookmarkScreen composable with expandable groups, search, and sort"
```

---

## Task 9: Settings Entry Point

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt`

**Context:** The Settings screen uses `CategoryGroupListItem { segmentedListItem(...) }` blocks. Look for how `NavigationActions` is accessed — it is either passed as a lambda parameter (e.g., `onNotificationsClick: () -> Unit`) or injected via `koinInject<NavigationActions>()` inside the composable. Follow whichever pattern is already used.

- [ ] **Step 1: Read `SettingScreen.kt` to find the access pattern**

```bash
grep -n "navigationActions\|koinInject\|NavigationActions\|onClick = " \
  kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt | head -40
```

Identify: (a) how NavigationActions is obtained, (b) where to add the Bookmarks row (alongside notifications, favorites, etc.).

- [ ] **Step 2: Add the Bookmarks navigation row**

Inside the appropriate `CategoryGroupListItem` block (alongside notifications, favorites, global search, history, etc.), add:

```kotlin
segmentedListItem(
    content = { Text("Bookmarks") },
    leadingContent = {
        Icon(Icons.Filled.Bookmark, contentDescription = null)
    },
    supportingContent = { Text("View and manage bookmarked chapters") },
    onClick = { navigationActions.bookmarks() },
)
```

If the settings screen uses lambda parameters instead of injected `NavigationActions`, add `onBookmarksClick: () -> Unit` to the composable signature and use `onClick = onBookmarksClick`. Then update the Nav3Graph call site for `SettingScreen` to pass `onBookmarksClick = { navigationActions.bookmarks() }`.

- [ ] **Step 3: Verify Settings screen compiles**

```bash
./gradlew :kmpuiviews:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Full app build check**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt
git commit -m "feat(bookmarks): add Bookmarks entry point in Settings screen"
```

---

## Task 10: Export / Import via Zipper

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt`

**Context:** `Zipper` is `actual open class Zipper(...)` in `androidMain`. It has a `private val handlers = mapOf<String, ZipHandler>(...)`. It also has `protected open fun additionalHandlers(): Map<String, ZipHandler> = emptyMap()` which is merged into `handlers` via `*additionalHandlers().toList().toTypedArray()`. The pattern is: add a new entry directly to the `handlers` map by adding `BookmarkDao` as a constructor parameter, or use `additionalHandlers()` override in a subclass. The simplest approach that avoids subclassing is to add `BookmarkDao` to the `Zipper` constructor directly and add the handler entry inline.

- [ ] **Step 1: Add `BookmarkDao` to `Zipper` constructor**

Find the constructor of `actual open class Zipper(...)` and add:

```kotlin
private val bookmarkDao: BookmarkDao,
```

Add import: `import com.programmersbox.favoritesdatabase.BookmarkDao`

Koin registration for `Zipper` uses `singleOf(::Zipper)` (or equivalent) and auto-injects all constructor parameters by type. Since `BookmarkDao` is already registered in `DatabaseModule` (Task 3), no other Koin changes are needed.

- [ ] **Step 2: Add bookmark handler to the `handlers` map**

In the `handlers` map inside `Zipper`, add a new entry alongside the existing ones:

```kotlin
"bookmarked_chapters.json" to ZipHandler(
    output = { stream ->
        runCatching { dataToOutputStream(bookmarkDao.getAllBookmarksSync(), stream) }
            .logFailureToDatabase()
    },
    input = { stream ->
        runCatching {
            Json.decodeFromString<List<BookmarkedChapter>>(stream.reader().readText())
                .forEach { bookmarkDao.insertBookmark(it) }
        }.logFailureToDatabase()
    },
),
```

Add imports:
```kotlin
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
```

(Check existing imports — `Json` and `decodeFromString` are almost certainly already imported.)

- [ ] **Step 3: Check for JVM Zipper actual**

```bash
find kmpuiviews/src/jvmMain -name "Zipper.kt" 2>/dev/null
```

If a JVM actual exists, apply the same `BookmarkDao` constructor addition and handler entry to it. If no JVM actual exists, skip.

- [ ] **Step 4: Verify Zipper compiles**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/Zipper.kt
git commit -m "feat(bookmarks): add bookmarked_chapters.json export/import to Zipper"
```

---

## Final Verification

- [ ] **Full app debug build**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: `BUILD SUCCESSFUL` with no warnings about unresolved references.

- [ ] **Run unit tests**

```bash
./gradlew :kmpuiviews:test
./gradlew :favoritesdatabase:test
```

Expected: All existing tests pass.

- [ ] **Manual smoke test checklist**

Install the debug APK on a device or emulator and verify:

1. Open any manga → Details screen
2. Tap the bookmark icon on a chapter — icon fills, no crash
3. Tap again — icon becomes outline (toggle works)
4. Long-press a chapter — "Bookmark" / "Remove bookmark" appears in the options sheet
5. Navigate to Settings → tap Bookmarks row → Bookmarks screen opens
6. Bookmarks screen shows the bookmarked chapter under the correct manga group
7. Tap the manga group header — chapter list collapses and expands
8. Type in the search bar — list filters by chapter name / manga title
9. Tap a sort chip — list re-orders
10. Tap the delete icon on a chapter row — bookmark is removed
11. Verify the bookmark icon on the Details screen reflects removal
12. Navigate to Settings → Backup → Export — verify `bookmarked_chapters.json` is in the ZIP
13. Clear bookmarks, import the ZIP — bookmarks are restored
