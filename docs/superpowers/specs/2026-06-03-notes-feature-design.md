# Notes Feature Design

**Date:** 2026-06-03  
**Branch:** feat/notes  
**Status:** Approved

---

## Overview

Add a Notes feature to OtakuWorld (Android + JVM/Desktop via Compose Multiplatform). Users can create, edit, and delete plain-text notes tied to items. Notes are stored in a dedicated Room database. The feature surfaces on two screens: the Item Details screen (per-item notes) and a dedicated All Notes screen (global view with FTS search). A single reusable `NoteBottomSheet` composable handles all note interactions from both screens.

---

## Use Cases

| ID  | Trigger | Behavior |
|-----|---------|----------|
| UC1 | Tap "Add Note" on Details screen | Sheet opens, item title read-only, empty TextField. Dismiss → save if non-blank, skip if blank. |
| UC2 | Tap existing note on Details screen | Sheet opens pre-filled. Dismiss → update if non-blank, delete if blank. |
| UC3 | Tap delete icon inside sheet | AlertDialog confirmation. Confirm → delete note, close sheet. |
| UC4 | View = Edit | No separate view mode. All notes open as immediately editable TextField. |
| UC5 | All Notes screen | LazyVerticalStaggeredGrid of all notes across all items. |
| UC6 | Search on All Notes screen | FTS indexes content + itemTitle. Results update reactively. |
| UC7 | Tap note card on All Notes screen | Same NoteBottomSheet. Auto-save on dismiss. |
| UC8 | Delete from All Notes screen | Same sheet delete flow — confirmation dialog, then removal. |

---

## Architecture

### Approach: Two dedicated ViewModels (Approach B)

- `DetailsNotesViewModel` — scoped to Details screen, filters notes by item URL
- `AllNotesViewModel` — scoped to All Notes screen, all notes + FTS search
- Both inject `NotesDao` directly (no repository layer — CRUD only, no complex business logic)
- `NoteBottomSheet` is a pure UI composable; save/delete logic delegated to the calling screen's VM

### Out of scope

- Note export / sharing
- Rich text formatting
- Collaborative notes
- Unit tests

---

## Data Layer

**Module:** `favoritesdatabase` (follows `BookmarkDatabase` pattern — no new module)  
**File:** `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt`

### Entity

```kotlin
@Entity(tableName = "notes")
@Serializable
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "itemUrl")
    val itemUrl: String,
    @ColumnInfo(name = "itemTitle")
    val itemTitle: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
)
```

### FTS Entity

```kotlin
@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteItem::class)
data class NoteItemFts(
    val content: String,
    val itemTitle: String,
)
```

Indexes both `content` and `itemTitle` — supports searching by note text or item name.

### DAO

| Method | Query |
|--------|-------|
| `insertNote(note): Long` | `@Insert(REPLACE)`, returns generated id |
| `updateNote(note)` | `@Update` |
| `deleteNoteById(id)` | `DELETE FROM notes WHERE id = :id` |
| `getNotesForItem(itemUrl): Flow<List<NoteItem>>` | `WHERE itemUrl = :itemUrl ORDER BY timestamp DESC` |
| `getAllNotes(): Flow<List<NoteItem>>` | `ORDER BY timestamp DESC` |
| `searchNotes(query): Flow<List<NoteItem>>` | FTS MATCH rowid join, ORDER BY timestamp DESC |

### Database

```kotlin
@Database(entities = [NoteItem::class, NoteItemFts::class], version = 1, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): NotesDatabase =
            databaseBuilder.build<NotesDatabase>("notes.db").build()
    }
}
```

No `build.gradle.kts` changes needed — Room + KSP already configured in `favoritesdatabase`.

`DatabaseBuilder` is the platform expect type registered in the `databaseBuilder` Koin sub-module (see `DatabaseModule.kt` line: `includes(databaseBuilder)`). `get()` in `NotesDatabase.getInstance(get())` resolves to that same type — identical to every other database in the module.

---

## ViewModel Layer

### `DetailsNotesViewModel`

**Location:** `kmpuiviews/.../presentation/notes/DetailsNotesViewModel.kt`

```
Constructor:  NotesDao, SavedStateHandle
State:
  notes: StateFlow<List<NoteItem>>   ← getNotesForItem(itemUrl), stateIn(viewModelScope)
  itemUrl: String                    decoded from SavedStateHandle (same nav arg as DetailsViewModel)
  itemTitle: String                  decoded from SavedStateHandle

Methods:
  saveNote(id: Long?, content: String)
    id != null  → updateNote(existing.copy(content = content, timestamp = Clock.System.now()))
    id == null  → insertNote(NoteItem(itemUrl, itemTitle, content))
                  (timestamp uses entity default: Clock.System.now() at insert time)
  deleteNote(id: Long) → deleteNoteById(id)
```

Used as a second `koinViewModel()` in `DetailsScreen` alongside `DetailsViewModel`.

### `AllNotesViewModel`

**Location:** `kmpuiviews/.../presentation/notes/AllNotesViewModel.kt`

```
Constructor:  NotesDao
State:
  searchQuery: MutableStateFlow<String>   (initially "")
  notes: StateFlow<List<NoteItem>>        flatMapLatest:
                                            blank → getAllNotes()
                                            non-blank → searchNotes(query)
Methods:
  updateQuery(q: String)  → searchQuery.value = q
  saveNote(note: NoteItem, content: String)
    content blank  → deleteNoteById(note.id)
    content non-blank → updateNote(note.copy(content = content, timestamp = Clock.System.now()))
  deleteNote(id: Long)    → deleteNoteById(id)
```

`searchQuery` applies `debounce(300ms)` before `flatMapLatest` to avoid hammering the database on every keystroke.

---

## UI Layer

### `NoteBottomSheet`

**Location:** `kmpuiviews/.../presentation/notes/NoteBottomSheet.kt`

Single reusable composable used from both screens.

```
Parameters:
  note: NoteItem?           null = new note
  itemTitle: String         displayed read-only at top
  onDismiss: (content: String) -> Unit   caller applies save/update/delete/no-op logic
  onDelete: () -> Unit      caller handles delete + close
  onDismissRequest: () -> Unit

Internal state:
  content = mutableStateOf(note?.content ?: "")

Layout (ModalBottomSheet):
  Header row:   Text(itemTitle)  [read-only, label style]
                IconButton(Delete)  [visible only when note != null]
                  → AlertDialog confirmation → onDelete()
  Body:         OutlinedTextField(content)  [autofocus, no maxLines, fillMaxWidth]
  onDismissRequest → onDismiss(content.value) → onDismissRequest()
```

**Auto-save contract (enforced by callers, not the sheet):**

`onDismiss(content)` always fires on any dismiss (swipe, back, tap outside). The sheet does not implement save logic — it only delivers the final content string. Callers determine action based on whether `note` (the parameter passed in) was null:

| note param | content | Action |
|------------|---------|--------|
| `null` (new) | non-blank | `insertNote` |
| `null` (new) | blank | no-op — do not create a record |
| non-null (existing) | non-blank | `updateNote` |
| non-null (existing) | blank | `deleteNoteById` — treat clear-and-dismiss as delete |

### Details Screen Integration

**File edited:** `kmpuiviews/.../presentation/details/DetailsScreen.kt`

- Add `notesVm: DetailsNotesViewModel = koinViewModel()` parameter to `DetailsScreenInternal`
- Collect `notesVm.notes` as state
- Add sheet state: `showNoteSheet: Boolean`, `selectedNote: NoteItem?`
- Inject Notes section into the existing `DetailContent` LazyColumn after the description, before chapters:
  - "Notes" section header (matching Chapters header style)
  - `NoteItem` cards (item title label + 3-line preview, ellipsis)
  - "Add Note" row/button at bottom of section
- Tapping "Add Note" → `selectedNote = null`, `showNoteSheet = true`
- Tapping a card → `selectedNote = note`, `showNoteSheet = true`
- `NoteBottomSheet` `onDismiss(content)` → `notesVm.saveNote(id = selectedNote?.id, content = content)`
  - `selectedNote == null` + non-blank content → `insertNote` (new note)
  - `selectedNote == null` + blank → no-op
  - `selectedNote != null` + non-blank → `updateNote`
  - `selectedNote != null` + blank → `deleteNoteById`
- `NoteBottomSheet` `onDelete` → `notesVm.deleteNote(selectedNote!!.id)` (only reachable when `selectedNote != null`)

### `NotesScreen`

**Location:** `kmpuiviews/.../presentation/notes/NotesScreen.kt`

```
vm: AllNotesViewModel = koinViewModel()

Layout:
  OtakuScaffold + TopAppBar("Notes")
  SearchBar at top → vm.updateQuery()
  LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(160.dp))
  Each card (ElevatedCard):
    Text(note.itemTitle)   [labelSmall style]
    Text(note.content, maxLines = 5, overflow = Ellipsis)
  Tap card → selectedNote = note, showNoteSheet = true

Sheet state (hoisted in NotesScreen):
  showNoteSheet: Boolean
  selectedNote: NoteItem?

NoteBottomSheet onDismiss → vm.saveNote(selectedNote!!, content)
NoteBottomSheet onDelete  → vm.deleteNote(selectedNote!!.id)
```

---

## Navigation

**`Screen.kt`** — add:
```kotlin
@Serializable
data object NotesScreen : Screen("notes")
```

**`Nav3Graph.kt`** — add inside `entryProvider`:
```kotlin
entry<Screen.NotesScreen> { NotesScreen() }
```

**`NavigationActions.kt`** interface — add:
```kotlin
fun notes()
```

**`Navigation3Actions.kt`** — implement:
```kotlin
override fun notes() { navController.navigate(Screen.NotesScreen) }
```

Navigation entry point: Settings menu, same pattern as `BookmarkScreen`.  
**File:** `kmpuiviews/.../presentation/settings/SettingScreen.kt`  
Add `notesClick: () -> Unit = navigationActions::notes` parameter (mirrors `bookmarksClick` at line 105). Wire a new settings list item that calls `notesClick`.

---

## Dependency Injection

**`DatabaseModule.kt`** — add:
```kotlin
single<NotesDatabase> { NotesDatabase.getInstance(get()) }
single<NotesDao> { get<NotesDatabase>().notesDao() }
```

**`ViewModelModule.kt`** — add:
```kotlin
viewModelOf(::DetailsNotesViewModel)
viewModelOf(::AllNotesViewModel)
```

---

## File Changeset

| Action | File |
|--------|------|
| NEW | `favoritesdatabase/.../NotesDatabase.kt` |
| NEW | `kmpuiviews/.../presentation/notes/NoteBottomSheet.kt` |
| NEW | `kmpuiviews/.../presentation/notes/NotesScreen.kt` |
| NEW | `kmpuiviews/.../presentation/notes/DetailsNotesViewModel.kt` |
| NEW | `kmpuiviews/.../presentation/notes/AllNotesViewModel.kt` |
| EDIT | `kmpuiviews/.../presentation/Screen.kt` |
| EDIT | `kmpuiviews/.../presentation/navigation/Nav3Graph.kt` |
| EDIT | `kmpuiviews/.../presentation/navactions/NavigationActions.kt` |
| EDIT | `kmpuiviews/.../presentation/navactions/Navigation3Actions.kt` |
| EDIT | `kmpuiviews/.../di/DatabaseModule.kt` |
| EDIT | `kmpuiviews/.../di/ViewModelModule.kt` |
| EDIT | `kmpuiviews/.../presentation/details/DetailsScreen.kt` |
| EDIT | `kmpuiviews/.../presentation/settings/SettingScreen.kt` |
