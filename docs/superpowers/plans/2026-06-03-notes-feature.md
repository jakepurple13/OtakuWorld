# Notes Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent plain-text notes (per-item and global) with a reusable bottom sheet composable and Room FTS search.

**Architecture:** `NotesDatabase` added to `favoritesdatabase` module (no new module). `DetailsNotesViewModel` injected via `parametersOf(info.url, info.title)` in `DetailContent` and passed to portrait/landscape views. `AllNotesViewModel` drives the All Notes screen with debounced FTS search. `NoteBottomSheet` is shared; auto-save logic lives in the VM, triggered by the `onDismiss` callback.

**Tech Stack:** Room (KMP, `androidx.room3`), Koin, Compose Multiplatform, Navigation3 (`navBackStack.add()`), Kotlin Flow

---

> **Spec note — file correction:** The approved spec listed `DetailsScreen.kt` as the sole details-layer edit. After reading the code, the actual LazyColumn with items is in `DetailsPortrait.kt` (portrait) and `DetailsLandscape.kt` (landscape). `DetailsScreen.kt` receives a minimal change only: adding `notesVm` to `DetailContent` and passing it to both view composables.

---

## File Map

| Action | File |
|--------|------|
| NEW | `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/DetailsNotesViewModel.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/AllNotesViewModel.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NoteBottomSheet.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NotesScreen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsPortrait.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsLandscape.kt` |

---

## Task 1: NotesDatabase — data layer

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

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

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteItem::class)
data class NoteItemFts(
    val content: String,
    val itemTitle: String,
)

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem): Long

    @Update
    suspend fun updateNote(note: NoteItem)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM notes WHERE itemUrl = :itemUrl ORDER BY timestamp DESC")
    fun getNotesForItem(itemUrl: String): Flow<List<NoteItem>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Query("""
        SELECT * FROM notes WHERE rowid IN (
            SELECT rowid FROM notes_fts
            WHERE notes_fts MATCH :query
        ) ORDER BY timestamp DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteItem>>
}

@Database(
    entities = [NoteItem::class, NoteItemFts::class],
    version = 1,
    exportSchema = true,
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): NotesDatabase =
            databaseBuilder
                .build<NotesDatabase>("notes.db")
                .build()
    }
}
```

- [ ] **Step 2: Verify the project builds (Room KSP runs)**

```bash
./gradlew :favoritesdatabase:compileCommonMainKotlinMetadata
```

Expected: BUILD SUCCESSFUL. Room KSP will generate DAO implementations.

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt
git commit -m "feat(data): add NotesDatabase with NoteItem entity, FTS, and NotesDao"
```

---

## Task 2: Koin DI registration

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt`

- [ ] **Step 1: Add NotesDatabase and NotesDao to DatabaseModule.kt**

Add these two imports at the top of the imports block (after the existing BookmarkDao/BookmarkDatabase imports):

```kotlin
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.NotesDatabase
```

Add these two lines at the end of the `databases` module block, after the `BookmarkDao` line:

```kotlin
    single<NotesDatabase> { NotesDatabase.getInstance(get()) }
    single<NotesDao> { get<NotesDatabase>().notesDao() }
```

The final `databases` module block should end with:

```kotlin
    single<BookmarkDatabase> { BookmarkDatabase.getInstance(get()) }
    single<BookmarkDao> { get<BookmarkDatabase>().bookmarkDao() }
    single<NotesDatabase> { NotesDatabase.getInstance(get()) }
    single<NotesDao> { get<NotesDatabase>().notesDao() }
}
```

- [ ] **Step 2: Add DetailsNotesViewModel and AllNotesViewModel to ViewModelModule.kt**

Add these two imports after the existing `BookmarkChaptersViewModel` import:

```kotlin
import com.programmersbox.kmpuiviews.presentation.notes.AllNotesViewModel
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
```

Add these two lines after `viewModelOf(::BookmarkChaptersViewModel)` in the `viewModels` module:

```kotlin
    viewModelOf(::DetailsNotesViewModel)
    viewModelOf(::AllNotesViewModel)
```

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt
git commit -m "feat(di): register NotesDatabase, NotesDao, DetailsNotesViewModel, AllNotesViewModel"
```

---

## Task 3: Navigation wiring

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt`

- [ ] **Step 1: Add NotesScreen to Screen.kt**

Add after the `BookmarkScreen` line (line 157):

```kotlin
    @Serializable
    data object NotesScreen : Screen("notes")
```

- [ ] **Step 2: Add notes() to NavigationActions.kt**

Add after the `fun bookmarks()` line (line 60):

```kotlin
    fun notes()
```

- [ ] **Step 3: Implement notes() in Navigation3Actions.kt**

Add after the `override fun bookmarks()` block. Find the `bookmarks()` implementation which looks like:

```kotlin
    override fun bookmarks() {
        navBackStack.add(Screen.BookmarkScreen)
    }
```

Add immediately after its closing brace:

```kotlin
    override fun notes() {
        navBackStack.add(Screen.NotesScreen)
    }
```

- [ ] **Step 4: Add entry in Nav3Graph.kt**

Add the import at the top with the other screen imports:

```kotlin
import com.programmersbox.kmpuiviews.presentation.notes.NotesScreen
```

Add an entry after the `entry<Screen.BookmarkScreen>` block (around line 119):

```kotlin
    entry<Screen.NotesScreen> {
        val navActions = LocalNavActions.current
        NotesScreen(onBackPress = { navActions.popBackStack() })
    }
```

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt
git commit -m "feat(nav): add NotesScreen to navigation graph and actions"
```

---

## Task 4: DetailsNotesViewModel

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/DetailsNotesViewModel.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class DetailsNotesViewModel(
    private val itemUrl: String,
    private val itemTitle: String,
    private val notesDao: NotesDao,
) : ViewModel() {

    val notes: StateFlow<List<NoteItem>> = notesDao
        .getNotesForItem(itemUrl)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun saveNote(note: NoteItem?, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when {
                note != null && content.isBlank() -> notesDao.deleteNoteById(note.id)
                note != null -> notesDao.updateNote(
                    note.copy(
                        content = content,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
                content.isNotBlank() -> notesDao.insertNote(
                    NoteItem(
                        itemUrl = itemUrl,
                        itemTitle = itemTitle,
                        content = content,
                    )
                )
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.deleteNoteById(id)
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/DetailsNotesViewModel.kt
git commit -m "feat(vm): add DetailsNotesViewModel for per-item notes"
```

---

## Task 5: AllNotesViewModel

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/AllNotesViewModel.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

private fun String.toFtsQuery(): String =
    trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.joinToString(" ") { "$it*" }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AllNotesViewModel(
    private val notesDao: NotesDao,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val notes: StateFlow<List<NoteItem>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) notesDao.getAllNotes()
            else notesDao.searchNotes(query.toFtsQuery())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun updateQuery(q: String) {
        searchQuery.value = q
    }

    fun saveNote(note: NoteItem, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (content.isBlank()) {
                notesDao.deleteNoteById(note.id)
            } else {
                notesDao.updateNote(
                    note.copy(
                        content = content,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.deleteNoteById(id)
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/AllNotesViewModel.kt
git commit -m "feat(vm): add AllNotesViewModel with debounced FTS search"
```

---

## Task 6: NoteBottomSheet

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NoteBottomSheet.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.notes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.NoteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    note: NoteItem?,
    itemTitle: String,
    onDismiss: (content: String) -> Unit,
    onDelete: () -> Unit,
) {
    var content by remember(note) { mutableStateOf(note?.content ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss(content) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, bottom = 8.dp)
        ) {
            Text(
                text = itemTitle,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            if (note != null) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete note")
                }
            }
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Write a note…") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .focusRequester(focusRequester),
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NoteBottomSheet.kt
git commit -m "feat(ui): add NoteBottomSheet composable with auto-save and delete confirmation"
```

---

## Task 7: NotesScreen

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NotesScreen.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.NoteItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBackPress: () -> Unit = {},
    vm: AllNotesViewModel = koinViewModel(),
) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    var selectedNote by remember { mutableStateOf<NoteItem?>(null) }
    var showNoteSheet by remember { mutableStateOf(false) }

    if (showNoteSheet) {
        val note = selectedNote
        if (note != null) {
            NoteBottomSheet(
                note = note,
                itemTitle = note.itemTitle,
                onDismiss = { content ->
                    vm.saveNote(note = note, content = content)
                    showNoteSheet = false
                    selectedNote = null
                },
                onDelete = {
                    vm.deleteNote(note.id)
                    showNoteSheet = false
                    selectedNote = null
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        var searchQuery by remember { mutableStateOf("") }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { q ->
                searchQuery = q
                vm.updateQuery(q)
            },
            placeholder = { Text("Search notes…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 72.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
            ),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notes, key = { it.id }) { note ->
                ElevatedCard(
                    onClick = {
                        selectedNote = note
                        showNoteSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.itemTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
                    )
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
```
```

- [ ] **Step 2: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NotesScreen.kt
git commit -m "feat(ui): add NotesScreen with staggered grid and FTS search"
```

---

## Task 8: Settings entry point

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt`

- [ ] **Step 1: Add `notesClick` parameter to the public `SettingScreen` composable**

Find the parameter list of `SettingScreen` (around line 86). Add `notesClick` after `bookmarksClick`:

```kotlin
    bookmarksClick: () -> Unit = navigationActions::bookmarks,
    notesClick: () -> Unit = navigationActions::notes,
    accountSettings: @Composable () -> Unit,
```

- [ ] **Step 2: Pass `notesClick` down to `SettingsScreen`**

In the `SettingScreen` body, find the `SettingsScreen(...)` call (around line 127). Add `notesClick = notesClick,` after `bookmarksClick = bookmarksClick,`:

```kotlin
                bookmarksClick = bookmarksClick,
                notesClick = notesClick,
```

- [ ] **Step 3: Add `notesClick` parameter to the private `SettingsScreen` composable**

Find the `private fun SettingsScreen(` declaration (around line 153). Add the parameter after `bookmarksClick`:

```kotlin
    bookmarksClick: () -> Unit,
    notesClick: () -> Unit,
```

- [ ] **Step 4: Add the Notes list item in the first `CategoryGroupListItem` block**

Find the Bookmarks `segmentedListItem` call (around line 232):

```kotlin
        segmentedListItem(
            content = { Text("Bookmarks") },
            leadingContent = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            onClick = bookmarksClick,
        )
```

Add the Notes item immediately after it:

```kotlin
        segmentedListItem(
            content = { Text("Notes") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = notesClick,
        )
```

Also add the `Icons.Default.Edit` import to the file's import block. The existing icons import block starts with `import androidx.compose.material.icons.Icons`. Add:

```kotlin
import androidx.compose.material.icons.filled.Edit
```

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt
git commit -m "feat(settings): add Notes entry point in settings menu"
```

---

## Task 9: Details screen integration

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsPortrait.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsLandscape.kt`

### DetailsScreen.kt changes

- [ ] **Step 1: Add imports to DetailsScreen.kt**

Add after the existing import block (near the other `kmpuiviews.presentation` imports):

```kotlin
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import org.koin.core.parameter.parametersOf
```

- [ ] **Step 2: Add `notesVm` to `DetailContent` and pass it to both view composables**

Find the `private fun DetailContent(` signature. Add `notesVm` as the last parameter with a default:

```kotlin
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMediaQueryApi::class
)
@Composable
private fun DetailContent(
    dao: ItemDao,
    details: DetailsViewModel,
    scope: CoroutineScope,
    state: DetailState.Success,
    windowSize: WindowSizeClass,
    shareChapter: Boolean,
    showDownload: Boolean,
    detailsActions: DetailsActions,
    notesVm: DetailsNotesViewModel = koinViewModel { parametersOf(state.info.url, state.info.title) },
) {
```

Then in the `when` block, add `notesVm = notesVm` to both `DetailsViewLandscape(...)` and `DetailsView(...)` calls:

For `DetailsViewLandscape`:
```kotlin
        WindowWidthSizeClass.Expanded -> {
            DetailsViewLandscape(
                info = state.info,
                isSaved = isSaved,
                shareChapter = shareChapter,
                isFavorite = state.action is DetailFavoriteAction.Remove,
                chapters = details.chapters,
                description = details.description,
                onTranslateDescription = details::translateDescription,
                showDownloadButton = { showDownload },
                canNotify = details.dbModel?.shouldCheckForUpdate == true,
                onPaletteSet = { details.palette = it },
                blurHash = details.blurHash,
                onBitmapSet = { details.imageBitmap = it },
                detailsActions = detailsActions,
                notesVm = notesVm,
            )
        }
```

For `DetailsView`:
```kotlin
        else -> {
            DetailsView(
                info = state.info,
                isSaved = isSaved,
                shareChapter = shareChapter,
                isFavorite = state.action is DetailFavoriteAction.Remove,
                chapters = details.chapters,
                description = details.description,
                onTranslateDescription = details::translateDescription,
                showDownloadButton = { showDownload },
                canNotify = details.dbModel?.shouldCheckForUpdate == true,
                onPaletteSet = { details.palette = it },
                onBitmapSet = { details.imageBitmap = it },
                blurHash = details.blurHash,
                detailsActions = detailsActions,
                notesVm = notesVm,
            )
        }
```

### DetailsPortrait.kt changes

- [ ] **Step 3: Add imports to DetailsPortrait.kt**

Add to the import block:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.TextButton
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.presentation.notes.NoteBottomSheet
```

- [ ] **Step 4: Add `notesVm` parameter to `DetailsView`**

Find the `fun DetailsView(` signature. Add `notesVm: DetailsNotesViewModel` as the last parameter before the closing `)`:

```kotlin
    detailsActions: DetailsActions,
    notificationRepository: NotificationRepository = koinInject(),
    notesVm: DetailsNotesViewModel,
)
```

- [ ] **Step 5: Add notes state and sheet state inside `DetailsView`**

Add after the existing `var fabMenuExpanded` line (around line 141):

```kotlin
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    var showNoteSheet by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteItem?>(null) }
```

- [ ] **Step 6: Add NoteBottomSheet call inside `DetailsView`, before `ModalNavigationDrawer`**

Find the `ModalNavigationDrawer(` call. Add the sheet immediately before it:

```kotlin
    if (showNoteSheet) {
        NoteBottomSheet(
            note = selectedNote,
            itemTitle = info.title,
            onDismiss = { content ->
                notesVm.saveNote(note = selectedNote, content = content)
                showNoteSheet = false
                selectedNote = null
            },
            onDelete = {
                selectedNote?.let { notesVm.deleteNote(it.id) }
                showNoteSheet = false
                selectedNote = null
            }
        )
    }

    ModalNavigationDrawer(
```

- [ ] **Step 7: Add notes section in the LazyColumn in `DetailsView`**

Find the `if (info.description.isNotEmpty())` item block in the LazyColumn. The notes section goes between the description `item { }` block and the `stickyHeader { ChapterListHeader(...) }`.

After the closing `}` of the description item block and before `stickyHeader {`, add:

```kotlin
                item(key = "notes_header") {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(notes, key = { "note_${it.id}" }) { note ->
                    ElevatedCard(
                        onClick = {
                            selectedNote = note
                            showNoteSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = note.itemTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
                        )
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item(key = "add_note") {
                    TextButton(
                        onClick = {
                            selectedNote = null
                            showNoteSheet = true
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Note")
                    }
                }
```

Also add the `width` import to the import block:

```kotlin
import androidx.compose.foundation.layout.width
```

### DetailsLandscape.kt changes

- [ ] **Step 8: Add imports to DetailsLandscape.kt**

Add to the import block:

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.presentation.notes.NoteBottomSheet
```

- [ ] **Step 9: Add `notesVm` parameter to `DetailsViewLandscape`**

Find `fun DetailsViewLandscape(`. Add `notesVm: DetailsNotesViewModel` as the last parameter before the closing `)`:

```kotlin
    detailsActions: DetailsActions,
    notesVm: DetailsNotesViewModel,
)
```

- [ ] **Step 10: Add notes state and sheet state inside `DetailsViewLandscape`**

Add after the `var hostState` and `var listState` lines (near the top of the function body):

```kotlin
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    var showNoteSheet by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteItem?>(null) }
```

- [ ] **Step 11: Add `NoteBottomSheet` in `DetailsViewLandscape`**

Find the first major layout composable (e.g., the outer `NormalOtakuScaffold` or `OtakuScaffold`). Add the sheet call before it:

```kotlin
    if (showNoteSheet) {
        NoteBottomSheet(
            note = selectedNote,
            itemTitle = info.title,
            onDismiss = { content ->
                notesVm.saveNote(note = selectedNote, content = content)
                showNoteSheet = false
                selectedNote = null
            },
            onDelete = {
                selectedNote?.let { notesVm.deleteNote(it.id) }
                showNoteSheet = false
                selectedNote = null
            }
        )
    }
```

- [ ] **Step 12: Add notes section in the landscape LazyColumn**

Find the landscape `LazyColumn` (around line 416 of `DetailsLandscape.kt`). It starts with `stickyHeader { ChapterListHeader(...) }`. Add the notes section before the `stickyHeader`:

```kotlin
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxHeight(),
                state = listState
            ) {
                item(key = "notes_header") {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(notes, key = { "note_${it.id}" }) { note ->
                    ElevatedCard(
                        onClick = {
                            selectedNote = note
                            showNoteSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = note.itemTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
                        )
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item(key = "add_note") {
                    TextButton(
                        onClick = {
                            selectedNote = null
                            showNoteSheet = true
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Note")
                    }
                }

                stickyHeader {
                    ChapterListHeader(
```

- [ ] **Step 13: Build to verify**

```bash
./gradlew :kmpuiviews:compileCommonMainKotlinMetadata
```

Expected: BUILD SUCCESSFUL. Fix any import or type errors before committing.

- [ ] **Step 14: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsPortrait.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsLandscape.kt
git commit -m "feat(details): integrate notes section and NoteBottomSheet into item details screen"
```

---

## Final verification

- [ ] **Full build**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Smoke test checklist (manual)**

1. Open any item's Details screen → Notes section visible below description
2. Tap "Add Note" → sheet opens, item title shown read-only, empty TextField
3. Type content, swipe sheet down → note appears in list
4. Tap note → sheet opens pre-filled
5. Edit content, dismiss → content updated
6. Clear content in sheet, dismiss → note removed from list
7. Open note, tap delete icon → confirmation dialog → confirm → note gone
8. Open Settings → "Notes" entry visible → tap → All Notes screen opens
9. All Notes screen → notes from multiple items appear in staggered grid
10. Search by note content → results filter
11. Search by item title → results filter
12. Tap note on All Notes screen → sheet opens, editable, auto-saves on dismiss
