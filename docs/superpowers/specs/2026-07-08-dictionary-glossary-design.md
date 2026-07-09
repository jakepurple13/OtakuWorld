# Dictionary & Glossary Feature Design

**Date:** 2026-07-08
**Branch:** feat/dictionary
**Status:** Approved

---

## Overview

Add a user-managed Dictionary/Glossary feature to OtakuWorld (Android + JVM/Desktop via Compose
Multiplatform). Users manually create, edit, delete, search, filter, and sort global dictionary
entries — not tied to any manga series. A brand-new, separate Room 3 (KMP) database backs the
feature; the existing favorites database is untouched. A quick-add entry point lives in the manga
reader (`:mangaworld:shared`); full management (list/search/edit/delete) lives in Settings →
Library. A `TranslationService` interface + stub implementation is wired through Koin for future
AI-powered translation.

---

## Use Cases

| ID  | Trigger | Behavior |
|-----|---------|----------|
| UC1 | "Add to Dictionary" button in reader top bar | Navigates to `DictionaryFormScreen` with `id = null`, term empty. Save or cancel → pops back to the reader immediately. |
| UC2 | "Dictionary" row in Settings → Library | Navigates to `DictionaryListScreen`. |
| UC3 | Tap "New Entry" on list screen | Navigates to `DictionaryFormScreen(id = null)`. |
| UC4 | Fill form, tap Save (create) | Inserts entry with `dateAdded = Clock.System.now()`, pops back. |
| UC5 | Tap entry on list screen | Navigates to `DictionaryDetailScreen(id)` — read-only view of all fields. |
| UC6 | Tap Edit on detail screen | Navigates to `DictionaryFormScreen(id)`, form pre-filled, `dateAdded` shown read-only, not editable. |
| UC7 | Tap Delete (list swipe/menu or detail screen) | `AlertDialog` names the term ("Delete \"<term>\"?") before deleting. |
| UC8 | Type in list screen search bar | Debounced (300ms) search across `term`, `definition`, `category`; results update reactively. |
| UC9 | Change sort control on list screen | Re-sorts by term (A–Z), date added (newest first), or category. |

---

## Architecture

### Approach: Repository-backed MVVM (per-screen ViewModels)

- `DictionaryDao` — CRUD + search Flows only, no sorting logic (sorting is a query parameter
  applied by the Repository/ViewModel, not baked into separate DAO queries).
- `DictionaryRepository` — sits between DAO and ViewModels; abstracts both `DictionaryDao` and
  `TranslationService` (unlike the Notes feature, which lets its ViewModels call the DAO directly —
  this feature explicitly needs a repository seam because it also fronts the translation stub).
- Three ViewModels, one per screen: `DictionaryListViewModel`, `DictionaryDetailViewModel`,
  `DictionaryFormViewModel`.
- One `DictionaryFormScreen` handles both create (`id = null`) and edit (`id` set) — no separate
  create/edit screens.

### Out of scope (per product spec)

- Cloud sync / remote database
- Pre-populated / bundled dictionary data
- Sharing glossaries between users
- iOS target
- Import/export
- Grouping entries by manga series
- Real translation implementation (stub only)

---

## Data Layer

**Module:** `favoritesdatabase` (new file, existing databases untouched)
**File:** `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryDatabase.kt`

### Entity

```kotlin
@Entity(tableName = "dictionary_entries")
@Serializable
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "term")
    val term: String,
    @ColumnInfo(name = "definition")
    val definition: String? = null,
    @ColumnInfo(name = "reading")
    val reading: String? = null,
    @ColumnInfo(name = "category")
    val category: String? = null,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "language")
    val language: String? = null,
    @ColumnInfo(name = "dateAdded")
    val dateAdded: Long = Clock.System.now().toEpochMilliseconds(),
)
```

`dateAdded` defaults at construction time (mirrors `NoteItem.timestamp`) and is never included as a
user-editable field in the form.

### DAO

| Method | Query |
|--------|-------|
| `insert(entry): Long` | `@Insert`, returns generated id |
| `update(entry)` | `@Update` |
| `delete(entry)` | `@Delete` |
| `getById(id): Flow<DictionaryEntry?>` | `WHERE id = :id` |
| `getAllByTerm(): Flow<List<DictionaryEntry>>` | `ORDER BY term COLLATE NOCASE ASC` |
| `getAllByDateAdded(): Flow<List<DictionaryEntry>>` | `ORDER BY dateAdded DESC` |
| `getAllByCategory(): Flow<List<DictionaryEntry>>` | `ORDER BY category COLLATE NOCASE ASC, term COLLATE NOCASE ASC` |
| `search(query): Flow<List<DictionaryEntry>>` | `WHERE term LIKE '%'\|\|:query\|\|'%' OR definition LIKE '%'\|\|:query\|\|'%' OR category LIKE '%'\|\|:query\|\|'%'`, ordered by term |

Plain `SQL LIKE`, not FTS4 — simpler, no existing precedent requires FTS for this table, and match
quality on short terms/categories doesn't need it.

### Database

```kotlin
@Database(entities = [DictionaryEntry::class], version = 1, exportSchema = true)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): DictionaryDatabase =
            databaseBuilder.build<DictionaryDatabase>("dictionary_database").build()
    }
}
```

Reuses the existing `expect/actual DatabaseBuilder` (same Koin `databaseBuilder` sub-module every
other database in this module resolves through) — no new platform code, no `build.gradle.kts`
changes (Room + KSP already configured in `favoritesdatabase`).

### TranslationService

**File:** `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/TranslationService.kt`

```kotlin
data class TranslationResult(
    val translatedTerm: String,
    val translatedDefinition: String?,
    val reading: String?,
)

interface TranslationService {
    suspend fun translateTerm(term: String, sourceLanguage: String, targetLanguage: String): TranslationResult
}

class StubTranslationService : TranslationService {
    override suspend fun translateTerm(term: String, sourceLanguage: String, targetLanguage: String) =
        TranslationResult(
            translatedTerm = term,
            translatedDefinition = "Stub translation not yet implemented.",
            reading = null,
        )
}
```

### Repository

**File:** `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepository.kt`

```kotlin
class DictionaryRepository(
    private val dao: DictionaryDao,
    private val translationService: TranslationService,
) {
    fun getById(id: Long): Flow<DictionaryEntry?> = dao.getById(id)

    fun getAll(sort: DictionarySort): Flow<List<DictionaryEntry>> = when (sort) {
        DictionarySort.Term -> dao.getAllByTerm()
        DictionarySort.DateAdded -> dao.getAllByDateAdded()
        DictionarySort.Category -> dao.getAllByCategory()
    }

    fun search(query: String): Flow<List<DictionaryEntry>> = dao.search(query)

    suspend fun save(entry: DictionaryEntry): Long =
        if (entry.id == 0L) dao.insert(entry) else dao.update(entry).let { entry.id }

    suspend fun delete(entry: DictionaryEntry) = dao.delete(entry)

    suspend fun translateTerm(term: String, sourceLanguage: String, targetLanguage: String) =
        translationService.translateTerm(term, sourceLanguage, targetLanguage)
}

enum class DictionarySort { Term, DateAdded, Category }
```

---

## ViewModel Layer

All in `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/`.

### `DictionaryListViewModel`

```
Constructor: DictionaryRepository
State:
  searchQuery: MutableStateFlow<String>        (initially "")
  sort: MutableStateFlow<DictionarySort>       (initially Term)
  entries: StateFlow<List<DictionaryEntry>>    combine(searchQuery.debounce(300), sort) →
                                                  blank query → repository.getAll(sort)
                                                  non-blank  → repository.search(query)
                                                  (client-side re-sort applied to search results
                                                   to respect the active `sort` selection)
Methods:
  updateQuery(q: String)
  updateSort(sort: DictionarySort)
  delete(entry: DictionaryEntry)
```

### `DictionaryDetailViewModel`

```
Constructor: DictionaryRepository, SavedStateHandle
State:
  entry: StateFlow<DictionaryEntry?>   ← repository.getById(id), stateIn(viewModelScope)
Methods:
  delete() → repository.delete(entry.value!!)
```

### `DictionaryFormViewModel`

```
Constructor: DictionaryRepository, SavedStateHandle
State:
  id: Long?                             decoded from SavedStateHandle nav arg (null = create)
  entry: StateFlow<DictionaryEntry?>    id != null → repository.getById(id); id == null → null
Methods:
  save(term, definition, reading, category, notes, language)
    id == null → repository.save(DictionaryEntry(term = term, ...))
    id != null → repository.save(existing.copy(term = term, ...))   // dateAdded untouched
```

---

## UI Layer

### `DictionaryListScreen`

```
vm: DictionaryListViewModel = koinViewModel()

Layout:
  OtakuScaffold + TopAppBar("Dictionary") with FAB → navActions.dictionaryForm(id = null)
  SearchBar at top → vm.updateQuery()
  Sort dropdown/segmented control (Term / Date Added / Category) → vm.updateSort()
  LazyColumn of entries:
    ListItem(headline = term, supporting = definition ellipsized, trailing = category chip if present)
    onClick → navActions.dictionaryDetail(entry.id)
    swipe-to-delete or overflow menu → AlertDialog("Delete \"${entry.term}\"?") → vm.delete(entry)
```

### `DictionaryDetailScreen`

```
vm: DictionaryDetailViewModel = koinViewModel()

Layout:
  OtakuScaffold + TopAppBar(entry.term, actions = [Edit, Delete])
  Field rows for reading, definition, category, notes, language, dateAdded (formatted, read-only)
  Edit icon    → navActions.dictionaryForm(id = entry.id)
  Delete icon  → AlertDialog("Delete \"${entry.term}\"?") → vm.delete() → popBackStack()
```

### `DictionaryFormScreen`

```
vm: DictionaryFormViewModel = koinViewModel()

Layout:
  OtakuScaffold + TopAppBar(if id == null "New Entry" else "Edit Entry")
  OutlinedTextFields: term (required), reading, definition, category, notes, language
  dateAdded NOT shown as an input; shown as read-only text only on edit
  Save button (enabled when term.isNotBlank()) → vm.save(...) → navActions.popBackStack()
  Cancel / back → navActions.popBackStack() with no save
```

---

## Reader Integration (`:mangaworld:shared`)

**File edited:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/AppBars.kt`

`ReaderTopBar` gets one more action, alongside bookmark/refresh/settings:

```kotlin
IconButton(onClick = onAddToDictionaryClick) {
    Icon(Icons.AutoMirrored.Default.MenuBook, contentDescription = "Add to Dictionary")
}
```

**File edited:** `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt`

`ReaderTopBar(... onAddToDictionaryClick = { navActions.dictionaryForm(id = null) } ...)` using
`LocalNavActions.current` (already resolvable — `:mangaworld:shared` depends on `:kmpuiviews`).
Pushing/popping the existing `DictionaryFormScreen` is what makes the round-trip lightweight; no
new bottom-sheet plumbing is needed in `:mangaworld:shared`.

---

## Navigation

**`Screen.kt`** — add:
```kotlin
@Serializable
sealed class DictionaryScreen(path: String) : Screen(path) {
    @Serializable
    data object List : DictionaryScreen("dictionary")

    @Serializable
    data class Detail(val id: Long) : DictionaryScreen("dictionary/detail")

    @Serializable
    data class Form(val id: Long? = null) : DictionaryScreen("dictionary/form")
}
```

**`Nav3Graph.kt`** — add inside `entryProvider`:
```kotlin
entry<Screen.DictionaryScreen.List> { DictionaryListScreen() }
entry<Screen.DictionaryScreen.Detail> { DictionaryDetailScreen() }
entry<Screen.DictionaryScreen.Form> { DictionaryFormScreen() }
```

**`NavigationActions.kt`** interface — add:
```kotlin
fun dictionary()
fun dictionaryDetail(id: Long)
fun dictionaryForm(id: Long? = null)
```

**`Navigation3Actions.kt`** — implement, same style as `notes()`/`bookmarks()`:
```kotlin
override fun dictionary() { navBackStack.add(Screen.DictionaryScreen.List) }
override fun dictionaryDetail(id: Long) { navBackStack.add(Screen.DictionaryScreen.Detail(id)) }
override fun dictionaryForm(id: Long?) { navBackStack.add(Screen.DictionaryScreen.Form(id)) }
```

Settings entry point — same pattern as Bookmarks/Notes.
**File edited:** `kmpuiviews/.../presentation/settings/library/LibraryScreen.kt`
Add a `segmentedListItem` row ("Dictionary", book icon) with `onClick = navActions::dictionary`.

**File edited:** `kmpuiviews/.../presentation/settings/search/SettingsSearchRegistry.kt`
Add a searchable entry, same shape as the Bookmarks/Notes entries (`breadcrumb = [Settings,
Settings.Library, Screen.DictionaryScreen.List]`, `targetScreen = Screen.DictionaryScreen.List`).

---

## Dependency Injection

**`DatabaseModule.kt`** — add:
```kotlin
single<DictionaryDatabase> { DictionaryDatabase.getInstance(get()) }
single<DictionaryDao> { get<DictionaryDatabase>().dictionaryDao() }
```

**New `DictionaryModule.kt`** (`kmpuiviews/.../di/DictionaryModule.kt`), included alongside the
existing modules at app startup:
```kotlin
val dictionaryModule: Module = module {
    single<TranslationService> { StubTranslationService() }
    single { DictionaryRepository(get(), get()) }
}
```

**`ViewModelModule.kt`** — add:
```kotlin
viewModelOf(::DictionaryListViewModel)
viewModelOf(::DictionaryDetailViewModel)
viewModelOf(::DictionaryFormViewModel)
```

---

## Testing

Same style as `NotesDaoTest` / `AllNotesViewModelTest` — real Room DB via
`Room.databaseBuilder<DictionaryDatabase>(name = tempFile.absolutePath).setDriver(BundledSQLiteDriver())`
in `jvmTest`, no mocking framework.

- **`DictionaryDaoTest`** (`favoritesdatabase/src/jvmTest/.../DictionaryDaoTest.kt`) — insert/update/delete,
  each sort query, search across term/definition/category.
- **`DictionaryRepositoryTest`** (`favoritesdatabase/src/jvmTest/.../DictionaryRepositoryTest.kt`) —
  real DAO + a small fake `TranslationService` (not `StubTranslationService`, so the test controls
  the returned value) — verifies `save()` insert-vs-update branching and correct delegation for
  sort/search/translate.
- **`DictionaryListViewModelTest`**, **`DictionaryFormViewModelTest`**
  (`kmpuiviews/src/jvmTest/.../presentation/dictionary/`) — StateFlow assertions following the
  `awaitCondition` pattern in `AllNotesViewModelTest` (Room's Flow emits on its own dispatcher, so
  tests poll with real time rather than a virtual test-dispatcher clock).

---

## README

Add a short paragraph (no architecture/stub-swap details) describing: global, user-managed
dictionary/glossary entries; access via Settings → Library → Dictionary for full management, and
via the "Add to Dictionary" button in the manga reader for quick-adding a term while reading.

---

## File Changeset

| Action | File |
|--------|------|
| NEW | `favoritesdatabase/.../DictionaryDatabase.kt` |
| NEW | `favoritesdatabase/.../TranslationService.kt` |
| NEW | `favoritesdatabase/.../DictionaryRepository.kt` |
| NEW | `favoritesdatabase/src/jvmTest/.../DictionaryDaoTest.kt` |
| NEW | `favoritesdatabase/src/jvmTest/.../DictionaryRepositoryTest.kt` |
| NEW | `kmpuiviews/.../presentation/dictionary/DictionaryListScreen.kt` |
| NEW | `kmpuiviews/.../presentation/dictionary/DictionaryListViewModel.kt` |
| NEW | `kmpuiviews/.../presentation/dictionary/DictionaryDetailScreen.kt` |
| NEW | `kmpuiviews/.../presentation/dictionary/DictionaryDetailViewModel.kt` |
| NEW | `kmpuiviews/.../presentation/dictionary/DictionaryFormScreen.kt` |
| NEW | `kmpuiviews/.../presentation/dictionary/DictionaryFormViewModel.kt` |
| NEW | `kmpuiviews/.../di/DictionaryModule.kt` |
| NEW | `kmpuiviews/src/jvmTest/.../presentation/dictionary/DictionaryListViewModelTest.kt` |
| NEW | `kmpuiviews/src/jvmTest/.../presentation/dictionary/DictionaryFormViewModelTest.kt` |
| EDIT | `kmpuiviews/.../presentation/Screen.kt` |
| EDIT | `kmpuiviews/.../presentation/navigation/Nav3Graph.kt` |
| EDIT | `kmpuiviews/.../presentation/navactions/NavigationActions.kt` |
| EDIT | `kmpuiviews/.../presentation/navactions/Navigation3Actions.kt` |
| EDIT | `kmpuiviews/.../presentation/settings/library/LibraryScreen.kt` |
| EDIT | `kmpuiviews/.../presentation/settings/search/SettingsSearchRegistry.kt` |
| EDIT | `kmpuiviews/.../di/DatabaseModule.kt` |
| EDIT | `kmpuiviews/.../di/ViewModelModule.kt` |
| EDIT | `mangaworld/shared/.../reader/AppBars.kt` |
| EDIT | `mangaworld/shared/.../reader/ReaderCompose.kt` |
| EDIT | `README.md` |
