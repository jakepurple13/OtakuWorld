# Bookmark Chapters — Design Spec

**Date:** 2026-06-03
**Branch:** feat/chapter-bookmarks
**Status:** Approved

---

## Overview

Users can bookmark individual manga chapters, view all bookmarks on a dedicated screen accessible from Settings, and export/import their bookmark data using the existing ZIP backup infrastructure.

---

## Use Cases

1. Tap bookmark icon on any chapter row in the Details screen to bookmark it (toggle — tap again to remove).
2. Navigate to the Bookmarks screen from Settings to see all bookmarked chapters grouped by manga.
3. Each manga group on the Bookmarks screen shows a cached cover image and a collapsible list of bookmarked chapters.
4. Remove a bookmark from the Bookmarks screen via swipe-to-dismiss or trailing delete icon.
5. Toggle a bookmark off from the Details screen via the bookmark icon or long-press menu.
6. Bookmarked chapters display a filled bookmark icon in their chapter row on the Details screen; non-bookmarked chapters show an outlined icon.
7. Sort bookmarks by date added (desc/asc) or alphabetically (chapter title, manga title).
8. Filter bookmarks via full-text search (FTS4) across manga titles and chapter names.
9. Export all bookmarks to a file via the Bookmarks screen top bar action.
10. Import bookmarks from a previously exported file via the Bookmarks screen top bar action.
11. Each bookmark records a timestamp (epoch millis) at creation time for sort-by-date.

---

## Out of Scope

- Cross-device sync or cloud backup
- Bookmark folders, categories, or tags
- Bookmark limits
- Screens other than Bookmarks screen and Details screen

---

## Architecture: Approach 1 — Full Isolated Stack

All new code follows existing conventions in the project. No existing files are restructured; only targeted additions and minimal changes to Details screen.

---

## Section 1: Data Layer

### Location
`favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/`

### Entities

```kotlin
@Entity(tableName = "bookmarked_chapters")
@Serializable
data class BookmarkedChapter(
    @PrimaryKey val chapterUrl: String,
    val chapterName: String,
    val parentUrl: String,
    val parentTitle: String,
    val parentImageUrl: String,
    val source: String,
    val timestamp: Long,  // epoch millis
)

@Entity(tableName = "bookmarked_chapters_fts")
@Fts4(contentEntity = BookmarkedChapter::class)
data class BookmarkedChapterFts(
    val chapterName: String,
    val parentTitle: String,
)
```

### DAO

```kotlin
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
        SELECT b.* FROM bookmarked_chapters b
        JOIN bookmarked_chapters_fts f ON b.rowid = f.rowid
        WHERE bookmarked_chapters_fts MATCH :query
        ORDER BY b.timestamp DESC
    """)
    fun searchBookmarks(query: String): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters")
    suspend fun getAllBookmarksSync(): List<BookmarkedChapter>
}
```

### Database

```kotlin
@Database(
    entities = [BookmarkedChapter::class, BookmarkedChapterFts::class],
    version = 1,
)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder<BookmarkDatabase>): BookmarkDatabase =
            databaseBuilder.build()
    }
}
```

New file: `BookmarkDatabase.kt` in `favoritesdatabase` module.

### Repository

New file: `kmpuiviews/src/commonMain/.../repository/BookmarkRepository.kt`

Wraps `BookmarkDao`. Exposes `Flow`s for all read queries and suspend functions for mutations. Repository-level `deleteBookmark(chapterUrl: String)` calls `dao.deleteBookmarkByUrl(chapterUrl)` so callers never need the full entity. No Firebase sync — bookmarks are local-only.

### DI Wiring

- `BookmarkDatabase` singleton added to `DatabaseModule`
- `BookmarkDao` singleton added to `DatabaseModule`
- `BookmarkRepository` added to `RepositoryModule`

---

## Section 2: Details Screen Changes

### `DetailsViewModel`

Two additions only — no restructuring:

```kotlin
var bookmarkedChapterUrls: Set<String> by mutableStateOf(emptySet())
    private set

// Collected in init alongside existing chapter state
bookmarkRepository.getBookmarksForDetail(details.url)
    .collect { bookmarkedChapterUrls = it.map { b -> b.chapterUrl }.toHashSet() }

fun toggleBookmark(chapter: KmpChapterModel) {
    viewModelScope.launch(Dispatchers.IO) {
        if (chapter.url in bookmarkedChapterUrls) {
            bookmarkRepository.deleteBookmark(chapter.url)
        } else {
            bookmarkRepository.insertBookmark(
                BookmarkedChapter(
                    chapterUrl = chapter.url,
                    chapterName = chapter.name,
                    parentUrl = details.url,
                    parentTitle = details.title,
                    parentImageUrl = details.imageUrl,
                    source = details.source,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }
}
```

### `DetailsActions`

One new lambda added to the existing data class:

```kotlin
val bookmarkChapter: (KmpChapterModel) -> Unit
```

### `ChapterItem` Composable

Two additions:

1. **Trailing bookmark icon**: `Icons.Filled.Bookmark` (primary tint) when bookmarked, `Icons.Outlined.BookmarkBorder` (muted) when not. Single tap calls `detailsActions.bookmarkChapter(c)`. Placed alongside the existing download icon.

2. **Long-press menu entry**: "Bookmark" / "Remove bookmark" item added to the existing options bottom sheet, alongside Mark as Read, Download, Share.

No other changes to `DetailsPortrait`, `DetailsLandscape`, or `DetailsHeader`.

---

## Section 3: Bookmarks Screen + Navigation

### `BookmarkSortOrder`

```kotlin
enum class BookmarkSortOrder { DATE_DESC, DATE_ASC, TITLE_AZ, MANGA_AZ }
```

### `BookmarkChaptersViewModel`

New file: `kmpuiviews/src/commonMain/.../presentation/bookmarks/BookmarkChaptersViewModel.kt`

```kotlin
class BookmarkChaptersViewModel(
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    var sortOrder by mutableStateOf(BookmarkSortOrder.DATE_DESC)
        private set
    var searchQuery by mutableStateOf("")
        private set

    val bookmarks: StateFlow<Map<String, List<BookmarkedChapter>>> =
        combine(
            snapshotFlow { searchQuery }
                .flatMapLatest { q ->
                    if (q.isBlank()) bookmarkRepository.getAllBookmarks()
                    else bookmarkRepository.searchBookmarks(q.toFtsQuery())
                },
            snapshotFlow { sortOrder },
        ) { list, sort -> list.sortedBy(sort).groupByparent() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setSort(sort: BookmarkSortOrder) { sortOrder = sort }
    fun setSearch(query: String) { searchQuery = query }

    fun removeBookmark(bookmark: BookmarkedChapter) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepository.deleteBookmark(bookmark.chapterUrl)
        }
    }
}
```

`toFtsQuery()` is a simple extension that appends `*` for prefix matching.
`groupByParent()` groups the sorted list by `parentUrl`, keyed by `parentTitle`.

### `BookmarkScreen` Composable

New file: `kmpuiviews/src/commonMain/.../presentation/bookmarks/BookmarkScreen.kt`

Structure:
- `Scaffold` with Material 3 `TopAppBar`
  - Title: "Bookmarks"
  - Navigation icon: back arrow
  - Actions: Export button, Import button
- Below top bar: `OutlinedTextField` / Material 3 `SearchBar` for FTS query — calls `vm.setSearch()`
- Sort chips row: `FilterChip` for each `BookmarkSortOrder` value
- `LazyColumn`:
  - Each manga group is an expandable header + chapter rows
  - **Group header**: `CustomKamelImage` (cover, 48×68dp) + manga title + bookmark count + expand/collapse chevron. Tapping the header toggles expansion.
  - **Chapter rows** (visible when expanded): bookmark icon + chapter name + relative timestamp + trailing delete `IconButton` calling `vm.removeBookmark()`
- **Empty state**: centered illustration + "No bookmarks yet" message

### Navigation

```kotlin
// Screen.kt — new NavKey
@Serializable
data object BookmarkScreen : Screen("bookmarks")

// Nav3Graph.kt — new entry
entry<Screen.BookmarkScreen> {
    BookmarkScreen()
}

// NavigationActions — new method
fun bookmarks() { navBackStack.add(Screen.BookmarkScreen) }
```

**Settings entry point**: new `ListItem` row in the existing Settings screen with a bookmark icon, "Bookmarks" label, and trailing chevron. Calls `navigationActions.bookmarks()`. Follows the same pattern as other settings navigation rows.

**ViewModel registration**: `viewModelOf(::BookmarkChaptersViewModel)` added to `ViewModelModule`.

---

## Section 4: Export / Import

### Mechanism

`Zipper.kt` (Android `androidMain`) has `additionalHandlers(): Map<String, ZipHandler>` returning `emptyMap()` by default. Each app subclass overrides it.

kmpuiviews Zipper adds to handlers:

```kotlin
"bookmarked_chapters.json" to ZipHandler(
    output = { stream ->
        dataToOutputStream(bookmarkDao.getAllBookmarksSync(), stream)
    },
    input = { stream ->
        Json.decodeFromString<List<BookmarkedChapter>>(stream.reader().readText())
            .forEach { bookmarkDao.insertBookmark(it) }
    }
)
```

`BookmarkedChapter` is `@Serializable` (defined in Section 1), so `dataToOutputStream` and `Json.decodeFromString` work with no additional setup.

### UI

Export and Import actions appear as icon buttons in the Bookmarks screen `TopAppBar`. They trigger the existing `FileKit` / `PlatformFile` file picker flow already used in Settings backup. No new file picker infrastructure introduced.

### Desktop

The JVM actual of `Zipper` (`jvmMain`) follows the same handler map pattern using `java.io` streams. `BookmarkedChapter` being `@Serializable` makes serialization identical on both platforms. No new serialization libraries introduced.

---

## File Summary

| File | Change |
|------|--------|
| `favoritesdatabase/.../BookmarkDatabase.kt` | New — entities, DAO, database |
| `kmpuiviews/.../repository/BookmarkRepository.kt` | New |
| `kmpuiviews/.../di/DatabaseModule.kt` | Add BookmarkDatabase + BookmarkDao singletons |
| `kmpuiviews/.../di/RepositoryModule.kt` | Add BookmarkRepository |
| `kmpuiviews/.../di/ViewModelModule.kt` | Add BookmarkChaptersViewModel |
| `kmpuiviews/.../presentation/bookmarks/BookmarkChaptersViewModel.kt` | New |
| `kmpuiviews/.../presentation/bookmarks/BookmarkScreen.kt` | New |
| `kmpuiviews/.../presentation/Screen.kt` | Add BookmarkScreen NavKey |
| `kmpuiviews/.../presentation/navigation/Nav3Graph.kt` | Add BookmarkScreen entry |
| `kmpuiviews/.../presentation/navactions/Navigation3Actions.kt` | Add bookmarks() method |
| `kmpuiviews/.../presentation/details/DetailsViewModel.kt` | Add bookmark state + toggleBookmark() |
| `kmpuiviews/.../presentation/details/DetailsScreen.kt` | Add trailing icon + long-press entry to ChapterItem; update DetailsActions |
| `UIViews/.../settings/SettingsScreen.kt` (or equivalent) | Add Bookmarks nav row |
| `mangaworld/.../Zipper.kt` (or subclass) | Override additionalHandlers() for bookmark export/import |
