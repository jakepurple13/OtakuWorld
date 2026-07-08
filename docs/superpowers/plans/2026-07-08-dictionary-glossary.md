# Dictionary & Glossary Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a global, user-managed Dictionary/Glossary — create/edit/delete/search/sort entries — backed by a brand-new Room 3 (KMP) database, with a quick-add entry point in the manga reader and full management in Settings → Library.

**Architecture:** New `DictionaryDatabase` (entity + DAO) in `:favoritesdatabase`, alongside a `TranslationService`/`StubTranslationService` pair and a `DictionaryRepository` that fronts both. Three per-screen ViewModels (`DictionaryListViewModel`, `DictionaryDetailViewModel`, `DictionaryFormViewModel`) live in `:kmpuiviews`, injected into three new Navigation3 screens (`Screen.DictionaryScreen`, `.Detail`, `.Form`). The manga reader (`:mangaworld:shared`) gets one new top-bar icon that pushes `Screen.DictionaryScreen.Form(id = null)`.

**Tech Stack:** Room 3 (`androidx.room3`, KMP), Koin, Compose Multiplatform / Material 3, Kotlin Coroutines + Flow, Navigation3 (`navBackStack.add()`).

## Global Constraints

- Do not modify any existing database file, entity, or DAO in `:favoritesdatabase` — `DictionaryDatabase` is a brand-new, separate `RoomDatabase` with its own file (`dictionary_database`).
- All entries are global — no per-manga-series association, no foreign key to any other table.
- `dateAdded` is set once at creation (`Clock.System.now().toEpochMilliseconds()`) and is never a user-editable form field, including on edit.
- No cloud sync, no bundled/pre-populated data, no sharing, no iOS target, no import/export, no grouping by series — do not add any code toward these.
- `TranslationService` is a stub only — `StubTranslationService` returns canned data; do not implement a real translation call.
- Follow this repo's existing Room convention: bundle an entity + its `@Dao` + its `@Database` class in one file (see `NotesDatabase.kt`, `SyncPreferences.kt`) rather than splitting into three files.
- Follow this repo's existing test convention: real Room databases (`Room.databaseBuilder<T>(...).setDriver(BundledSQLiteDriver())`) and hand-written fakes for interfaces — **no mocking library** (`mockk` is not a dependency anywhere in this repo; do not add it).
- Quick-add from the reader must feel lightweight: it is a plain navigation push to the existing `DictionaryFormScreen` with `id = null`; do not build separate bottom-sheet UI for this in `:mangaworld:shared`.
- The `DictionaryFormScreen` is reused for both create (`id == null`) and edit (`id != null`) — do not create two separate form screens.

---

## File Map

| Action | File |
|--------|------|
| NEW | `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryDatabase.kt` |
| NEW | `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/DictionaryDaoTest.kt` |
| NEW | `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/TranslationService.kt` |
| NEW | `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepository.kt` |
| NEW | `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepositoryTest.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DictionaryModule.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/KmpModule.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListViewModel.kt` |
| NEW | `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListViewModelTest.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailViewModel.kt` |
| NEW | `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailViewModelTest.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormViewModel.kt` |
| NEW | `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormViewModelTest.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListScreen.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailScreen.kt` |
| NEW | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormScreen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/library/LibraryScreen.kt` |
| EDIT | `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt` |
| EDIT | `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/AppBars.kt` |
| EDIT | `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt` |
| EDIT | `README.md` |

---

### Task 1: `DictionaryDatabase` (entity + DAO + database class)

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryDatabase.kt`
- Test: `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/DictionaryDaoTest.kt`

**Interfaces:**
- Produces: `DictionaryEntry(id: Long = 0, term: String, definition: String? = null, reading: String? = null, category: String? = null, notes: String? = null, language: String? = null, dateAdded: Long = <now>)`; `DictionaryDao` with `suspend fun insert(entry: DictionaryEntry): Long`, `suspend fun update(entry: DictionaryEntry)`, `suspend fun delete(entry: DictionaryEntry)`, `fun getById(id: Long): Flow<DictionaryEntry?>`, `fun getAllByTerm(): Flow<List<DictionaryEntry>>`, `fun getAllByDateAdded(): Flow<List<DictionaryEntry>>`, `fun getAllByCategory(): Flow<List<DictionaryEntry>>`, `fun search(query: String): Flow<List<DictionaryEntry>>`; `DictionaryDatabase : RoomDatabase` with `abstract fun dictionaryDao(): DictionaryDao` and `companion object { fun getInstance(databaseBuilder: DatabaseBuilder): DictionaryDatabase }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var dao: DictionaryDao

    private fun entry(
        term: String,
        definition: String? = null,
        category: String? = null,
        dateAdded: Long = 0L,
    ) = DictionaryEntry(
        term = term,
        definition = definition,
        category = category,
        dateAdded = dateAdded,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("dictionary-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.dictionaryDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `insert then getById returns the entry`() = runTest {
        val id = dao.insert(entry("Sensei"))

        val result = dao.getById(id).first()

        assertEquals("Sensei", result?.term)
    }

    @Test
    fun `getById returns null when no row exists`() = runTest {
        assertNull(dao.getById(999L).first())
    }

    @Test
    fun `update modifies an existing entry`() = runTest {
        val id = dao.insert(entry("Sensei", definition = "Teacher"))
        val stored = dao.getById(id).first()!!

        dao.update(stored.copy(definition = "Master"))

        assertEquals("Master", dao.getById(id).first()?.definition)
    }

    @Test
    fun `delete removes the entry`() = runTest {
        val id = dao.insert(entry("Sensei"))
        val stored = dao.getById(id).first()!!

        dao.delete(stored)

        assertNull(dao.getById(id).first())
    }

    @Test
    fun `getAllByTerm orders alphabetically ignoring case`() = runTest {
        dao.insert(entry("zephyr"))
        dao.insert(entry("Apple"))
        dao.insert(entry("banana"))

        val all = dao.getAllByTerm().first()

        assertEquals(listOf("Apple", "banana", "zephyr"), all.map { it.term })
    }

    @Test
    fun `getAllByDateAdded orders newest first`() = runTest {
        dao.insert(entry("First", dateAdded = 100L))
        dao.insert(entry("Second", dateAdded = 300L))
        dao.insert(entry("Third", dateAdded = 200L))

        val all = dao.getAllByDateAdded().first()

        assertEquals(listOf("Second", "Third", "First"), all.map { it.term })
    }

    @Test
    fun `getAllByCategory orders by category then term`() = runTest {
        dao.insert(entry("Zulu", category = "Verbs"))
        dao.insert(entry("Alpha", category = "Nouns"))
        dao.insert(entry("Beta", category = "Nouns"))

        val all = dao.getAllByCategory().first()

        assertEquals(listOf("Alpha", "Beta", "Zulu"), all.map { it.term })
    }

    @Test
    fun `search matches term`() = runTest {
        dao.insert(entry("Sensei"))
        dao.insert(entry("Gakusei"))

        val results = dao.search("Sen").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }

    @Test
    fun `search matches definition`() = runTest {
        dao.insert(entry("Sensei", definition = "A wise teacher"))
        dao.insert(entry("Gakusei", definition = "A student"))

        val results = dao.search("teacher").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }

    @Test
    fun `search matches category`() = runTest {
        dao.insert(entry("Sensei", category = "Honorifics"))
        dao.insert(entry("Gakusei", category = "People"))

        val results = dao.search("Honorific").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.DictionaryDaoTest"`
Expected: FAIL to compile — `DictionaryDatabase`, `DictionaryDao`, `DictionaryEntry` are unresolved references.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

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

@Dao
interface DictionaryDao {
    @Insert
    suspend fun insert(entry: DictionaryEntry): Long

    @Update
    suspend fun update(entry: DictionaryEntry)

    @Delete
    suspend fun delete(entry: DictionaryEntry)

    @Query("SELECT * FROM dictionary_entries WHERE id = :id")
    fun getById(id: Long): Flow<DictionaryEntry?>

    @Query("SELECT * FROM dictionary_entries ORDER BY term COLLATE NOCASE ASC")
    fun getAllByTerm(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries ORDER BY dateAdded DESC")
    fun getAllByDateAdded(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries ORDER BY category COLLATE NOCASE ASC, term COLLATE NOCASE ASC")
    fun getAllByCategory(): Flow<List<DictionaryEntry>>

    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE term LIKE '%' || :query || '%'
           OR definition LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<DictionaryEntry>>
}

@Database(
    entities = [DictionaryEntry::class],
    version = 1,
    exportSchema = true,
)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): DictionaryDatabase =
            databaseBuilder
                .build<DictionaryDatabase>("dictionary_database")
                .build()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.DictionaryDaoTest"`
Expected: PASS (10 tests)

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryDatabase.kt \
        favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/DictionaryDaoTest.kt
git commit -m "feat: add DictionaryDatabase with DictionaryEntry entity and DictionaryDao"
```

---

### Task 2: `TranslationService` + `StubTranslationService`

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/TranslationService.kt`

**Interfaces:**
- Produces: `TranslationResult(translatedTerm: String, translatedDefinition: String?, reading: String?)`; `interface TranslationService { suspend fun translateTerm(term: String, sourceLanguage: String, targetLanguage: String): TranslationResult }`; `class StubTranslationService : TranslationService`.

This is a stub with no branching logic to unit-test meaningfully beyond "it returns a result" — that behavior is covered indirectly by `DictionaryRepositoryTest` in Task 3, which injects a purpose-built fake instead of this stub (so the test controls the returned value). No dedicated test file for this task.

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.favoritesdatabase

data class TranslationResult(
    val translatedTerm: String,
    val translatedDefinition: String?,
    val reading: String?,
)

interface TranslationService {
    suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult
}

class StubTranslationService : TranslationService {
    override suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult = TranslationResult(
        translatedTerm = term,
        translatedDefinition = "Stub translation not yet implemented.",
        reading = null,
    )
}
```

- [ ] **Step 2: Verify the module still compiles**

Run: `./gradlew :favoritesdatabase:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/TranslationService.kt
git commit -m "feat: add TranslationService interface and StubTranslationService"
```

---

### Task 3: `DictionaryRepository`

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepository.kt`
- Test: `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepositoryTest.kt`

**Interfaces:**
- Consumes: `DictionaryDao` (Task 1), `TranslationService` / `TranslationResult` (Task 2).
- Produces: `enum class DictionarySort { Term, DateAdded, Category }`; `DictionaryRepository(dao: DictionaryDao, translationService: TranslationService)` with `fun getById(id: Long): Flow<DictionaryEntry?>`, `fun getAll(sort: DictionarySort): Flow<List<DictionaryEntry>>`, `fun search(query: String): Flow<List<DictionaryEntry>>`, `suspend fun save(entry: DictionaryEntry): Long`, `suspend fun delete(entry: DictionaryEntry)`, `suspend fun translateTerm(term: String, sourceLanguage: String, targetLanguage: String): TranslationResult`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeTranslationService(
    private val result: TranslationResult = TranslationResult(
        translatedTerm = "fake",
        translatedDefinition = "fake definition",
        reading = "fake reading",
    ),
) : TranslationService {
    var lastCall: Triple<String, String, String>? = null

    override suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult {
        lastCall = Triple(term, sourceLanguage, targetLanguage)
        return result
    }
}

class DictionaryRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var translationService: FakeTranslationService
    private lateinit var repository: DictionaryRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("dictionary-repository-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        translationService = FakeTranslationService()
        repository = DictionaryRepository(database.dictionaryDao(), translationService)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `save with id 0 inserts a new entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val stored = repository.getById(id).first()
        assertEquals("Sensei", stored?.term)
    }

    @Test
    fun `save with a non-zero id updates the existing entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))
        val stored = repository.getById(id).first()!!

        repository.save(stored.copy(term = "Sensei-updated"))

        assertEquals("Sensei-updated", repository.getById(id).first()?.term)
    }

    @Test
    fun `delete removes the entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))
        val stored = repository.getById(id).first()!!

        repository.delete(stored)

        assertNull(repository.getById(id).first())
    }

    @Test
    fun `getAll with Term sort delegates to the term-ordered query`() = runTest {
        repository.save(DictionaryEntry(term = "Zulu"))
        repository.save(DictionaryEntry(term = "Alpha"))

        val all = repository.getAll(DictionarySort.Term).first()

        assertEquals(listOf("Alpha", "Zulu"), all.map { it.term })
    }

    @Test
    fun `getAll with DateAdded sort delegates to the date-ordered query`() = runTest {
        repository.save(DictionaryEntry(term = "First", dateAdded = 100L))
        repository.save(DictionaryEntry(term = "Second", dateAdded = 200L))

        val all = repository.getAll(DictionarySort.DateAdded).first()

        assertEquals(listOf("Second", "First"), all.map { it.term })
    }

    @Test
    fun `search delegates to the dao search query`() = runTest {
        repository.save(DictionaryEntry(term = "Sensei"))
        repository.save(DictionaryEntry(term = "Gakusei"))

        val results = repository.search("Sen").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }

    @Test
    fun `translateTerm delegates to the translation service with the given args`() = runTest {
        val result = repository.translateTerm("Sensei", "ja", "en")

        assertEquals("fake", result.translatedTerm)
        assertEquals(Triple("Sensei", "ja", "en"), translationService.lastCall)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.DictionaryRepositoryTest"`
Expected: FAIL to compile — `DictionaryRepository`, `DictionarySort` are unresolved references.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.favoritesdatabase

import kotlinx.coroutines.flow.Flow

enum class DictionarySort { Term, DateAdded, Category }

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
        if (entry.id == 0L) {
            dao.insert(entry)
        } else {
            dao.update(entry)
            entry.id
        }

    suspend fun delete(entry: DictionaryEntry) = dao.delete(entry)

    suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult = translationService.translateTerm(term, sourceLanguage, targetLanguage)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.DictionaryRepositoryTest"`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepository.kt \
        favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/DictionaryRepositoryTest.kt
git commit -m "feat: add DictionaryRepository abstracting DictionaryDao and TranslationService"
```

---

### Task 4: Koin DI — database + repository + translation service

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DictionaryModule.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/KmpModule.kt`

**Interfaces:**
- Consumes: `DictionaryDatabase`, `DictionaryDao` (Task 1), `TranslationService`, `StubTranslationService` (Task 2), `DictionaryRepository` (Task 3).
- Produces: Koin singles resolvable via `get<DictionaryDao>()`, `get<TranslationService>()`, `get<DictionaryRepository>()`.

- [ ] **Step 1: Add `DictionaryDatabase`/`DictionaryDao` to `DatabaseModule.kt`**

Add these two imports after the existing `NotesDao`/`NotesDatabase` imports:

```kotlin
import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.DictionaryDatabase
```

Add these two lines at the end of the `databases` module block, after the `NotesDao` line:

```kotlin
    single<DictionaryDatabase> { DictionaryDatabase.getInstance(get()) }
    single<DictionaryDao> { get<DictionaryDatabase>().dictionaryDao() }
```

The final `databases` module block should end with:

```kotlin
    single<NotesDatabase> { NotesDatabase.getInstance(get()) }
    single<NotesDao> { get<NotesDatabase>().notesDao() }
    single<DictionaryDatabase> { DictionaryDatabase.getInstance(get()) }
    single<DictionaryDao> { get<DictionaryDatabase>().dictionaryDao() }
}
```

- [ ] **Step 2: Create `DictionaryModule.kt`**

```kotlin
package com.programmersbox.kmpuiviews.di

import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.StubTranslationService
import com.programmersbox.favoritesdatabase.TranslationService
import org.koin.core.module.Module
import org.koin.dsl.module

val dictionaryModule: Module = module {
    single<TranslationService> { StubTranslationService() }
    single { DictionaryRepository(get(), get()) }
}
```

- [ ] **Step 3: Register `dictionaryModule` in `KmpModule.kt`**

`databases` and `viewModels` are both pulled into the app-wide Koin graph via a single
`includes(...)` call in
`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/KmpModule.kt`:

```kotlin
package com.programmersbox.kmpuiviews.di

import org.koin.dsl.module

val kmpModule = module {
    includes(
        appModule,
        databases,
        repositories,
        viewModels,
        aiModule,
        navigationModule
    )
}
```

Add `dictionaryModule` to that list, after `databases`:

```kotlin
package com.programmersbox.kmpuiviews.di

import org.koin.dsl.module

val kmpModule = module {
    includes(
        appModule,
        databases,
        dictionaryModule,
        repositories,
        viewModels,
        aiModule,
        navigationModule
    )
}
```

- [ ] **Step 4: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DictionaryModule.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/KmpModule.kt
git commit -m "feat(di): register DictionaryDatabase, DictionaryDao, DictionaryRepository, TranslationService"
```

---

### Task 5: `Screen.DictionaryScreen` navigation types

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt`

**Interfaces:**
- Produces: `Screen.DictionaryScreen : Screen("dictionary")` (the list screen itself), `Screen.DictionaryScreen.Detail(val id: Long) : Screen("dictionary_detail")`, `Screen.DictionaryScreen.Form(val id: Long? = null) : Screen("dictionary_form")`.

- [ ] **Step 1: Add `DictionaryScreen` to `Screen.kt`**

Add after the `NotesScreen` line (line 176):

```kotlin

    @Serializable
    data object DictionaryScreen : Screen("dictionary") {
        @Serializable
        data class Detail(val id: Long) : Screen("dictionary_detail")

        @Serializable
        data class Form(val id: Long? = null) : Screen("dictionary_form")
    }
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt
git commit -m "feat(nav): add Screen.DictionaryScreen, .Detail, .Form navigation types"
```

---

### Task 6: `NavigationActions` dictionary methods

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt`

**Interfaces:**
- Consumes: `Screen.DictionaryScreen`, `.Detail`, `.Form` (Task 5).
- Produces: `NavigationActions.dictionary()`, `.dictionaryDetail(id: Long)`, `.dictionaryForm(id: Long? = null)`.

- [ ] **Step 1: Add methods to the `NavigationActions` interface**

Add after the `fun notes()` line (line 61):

```kotlin
    fun dictionary()
    fun dictionaryDetail(id: Long)
    fun dictionaryForm(id: Long? = null)
```

- [ ] **Step 2: Implement them in `Navigation3Actions`**

Add after the `override fun notes()` block:

```kotlin
    override fun dictionary() {
        navBackStack.add(Screen.DictionaryScreen)
    }

    override fun dictionaryDetail(id: Long) {
        navBackStack.add(Screen.DictionaryScreen.Detail(id))
    }

    override fun dictionaryForm(id: Long?) {
        navBackStack.add(Screen.DictionaryScreen.Form(id))
    }
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL — this also confirms there are no other implementors of `NavigationActions` left with unimplemented abstract methods.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt
git commit -m "feat(nav): add dictionary navigation actions"
```

---

### Task 7: `DictionaryListViewModel`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListViewModel.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListViewModelTest.kt`

**Interfaces:**
- Consumes: `DictionaryRepository`, `DictionarySort`, `DictionaryEntry` (Task 3), `TranslationService`/`StubTranslationService` (Task 2, for test setup only).
- Produces: `DictionaryListViewModel(repository: DictionaryRepository)` with `val entries: StateFlow<List<DictionaryEntry>>`, `fun updateQuery(q: String)`, `fun updateSort(sort: DictionarySort)`, `fun delete(entry: DictionaryEntry)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.DictionarySort
import com.programmersbox.favoritesdatabase.StubTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictionaryListViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var repository: DictionaryRepository

    // Room's Flow emits on its own dispatcher, not the test dispatcher's virtual clock,
    // so wait for state changes with real time instead of advancing test time.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel() = DictionaryListViewModel(repository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("dictionary-list-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = DictionaryRepository(database.dictionaryDao(), StubTranslationService())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun `starts with no entries`() = runTest {
        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        assertTrue(vm.entries.value.isEmpty())
    }

    @Test fun `existing entries show up after collection sorted by term`() = runTest {
        repository.save(DictionaryEntry(term = "Zulu"))
        repository.save(DictionaryEntry(term = "Alpha"))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        assertEquals(listOf("Alpha", "Zulu"), vm.entries.value.map { it.term })
    }

    @Test fun `updateQuery filters entries by term`() = runTest {
        repository.save(DictionaryEntry(term = "Sensei"))
        repository.save(DictionaryEntry(term = "Gakusei"))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        vm.updateQuery("Sen")
        awaitCondition { vm.entries.value.size == 1 }

        assertEquals("Sensei", vm.entries.value[0].term)
    }

    @Test fun `blank query resets to all entries`() = runTest {
        repository.save(DictionaryEntry(term = "Sensei"))
        repository.save(DictionaryEntry(term = "Gakusei"))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        vm.updateQuery("Sen")
        awaitCondition { vm.entries.value.size == 1 }

        vm.updateQuery("")
        awaitCondition { vm.entries.value.size == 2 }
    }

    @Test fun `updateSort switches ordering to date added`() = runTest {
        repository.save(DictionaryEntry(term = "First", dateAdded = 100L))
        repository.save(DictionaryEntry(term = "Second", dateAdded = 200L))

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.size == 2 }

        vm.updateSort(DictionarySort.DateAdded)
        awaitCondition { vm.entries.value.map { it.term } == listOf("Second", "First") }
    }

    @Test fun `delete removes the entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))
        val stored = repository.getById(id).first()

        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.entries.collect {} }
        awaitCondition { vm.entries.value.isNotEmpty() }

        vm.delete(stored!!)

        awaitCondition { vm.entries.value.isEmpty() }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryListViewModelTest"`
Expected: FAIL to compile — `DictionaryListViewModel` is an unresolved reference.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.DictionarySort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DictionaryListViewModel(
    private val repository: DictionaryRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sort = MutableStateFlow(DictionarySort.Term)

    val entries: StateFlow<List<DictionaryEntry>> = searchQuery
        .debounce(300)
        .combine(sort) { query, sort -> query to sort }
        .flatMapLatest { (query, sort) ->
            if (query.isBlank()) {
                repository.getAll(sort)
            } else {
                repository.search(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun updateQuery(q: String) {
        searchQuery.value = q
    }

    fun updateSort(newSort: DictionarySort) {
        sort.value = newSort
    }

    fun delete(entry: DictionaryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entry)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryListViewModelTest"`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListViewModel.kt \
        kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListViewModelTest.kt
git commit -m "feat(vm): add DictionaryListViewModel with debounced search and sort"
```

---

### Task 8: `DictionaryDetailViewModel`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailViewModel.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `DictionaryRepository`, `DictionaryEntry` (Task 3).
- Produces: `DictionaryDetailViewModel(id: Long, repository: DictionaryRepository)` with `val entry: StateFlow<DictionaryEntry?>`, `fun delete()`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.StubTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictionaryDetailViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var repository: DictionaryRepository

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(id: Long) = DictionaryDetailViewModel(id, repository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("dictionary-detail-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = DictionaryRepository(database.dictionaryDao(), StubTranslationService())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun `entry emits the stored value for the given id`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        assertEquals("Sensei", vm.entry.value?.term)
    }

    @Test fun `delete removes the entry and entry becomes null`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        vm.delete()

        awaitCondition { vm.entry.value == null }
        assertNull(vm.entry.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryDetailViewModelTest"`
Expected: FAIL to compile — `DictionaryDetailViewModel` is an unresolved reference.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DictionaryDetailViewModel(
    private val id: Long,
    private val repository: DictionaryRepository,
) : ViewModel() {

    val entry: StateFlow<DictionaryEntry?> = repository
        .getById(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun delete() {
        val current = entry.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(current)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryDetailViewModelTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailViewModel.kt \
        kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailViewModelTest.kt
git commit -m "feat(vm): add DictionaryDetailViewModel"
```

---

### Task 9: `DictionaryFormViewModel`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormViewModel.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormViewModelTest.kt`

**Interfaces:**
- Consumes: `DictionaryRepository`, `DictionaryEntry` (Task 3).
- Produces: `DictionaryFormViewModel(id: Long?, repository: DictionaryRepository)` with `val entry: StateFlow<DictionaryEntry?>` (pre-fill source; `null` for create), `fun save(term: String, definition: String?, reading: String?, category: String?, notes: String?, language: String?)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.StubTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictionaryFormViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var repository: DictionaryRepository

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(id: Long?) = DictionaryFormViewModel(id, repository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("dictionary-form-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = DictionaryRepository(database.dictionaryDao(), StubTranslationService())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun `entry is null when id is null`() = runTest {
        val vm = viewModel(null)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        assertNull(vm.entry.value)
    }

    @Test fun `entry emits the stored value when id is set`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        assertEquals("Sensei", vm.entry.value?.term)
    }

    @Test fun `save with null id inserts a new entry with dateAdded set`() = runTest {
        val vm = viewModel(null)

        vm.save(
            term = "Sensei",
            definition = "Teacher",
            reading = "せんせい",
            category = "Honorifics",
            notes = null,
            language = "ja",
        )

        awaitCondition { repository.getAll(com.programmersbox.favoritesdatabase.DictionarySort.Term).first().isNotEmpty() }
        val stored = repository.getAll(com.programmersbox.favoritesdatabase.DictionarySort.Term).first().first()

        assertEquals("Sensei", stored.term)
        assertEquals("Teacher", stored.definition)
        assertEquals("せんせい", stored.reading)
        assertEquals("Honorifics", stored.category)
        assertEquals("ja", stored.language)
    }

    @Test fun `save with non-null id updates the existing entry without changing dateAdded`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei", dateAdded = 12345L))

        val vm = viewModel(id)
        val __sub = backgroundScope.launch { vm.entry.collect {} }
        awaitCondition { vm.entry.value != null }

        vm.save(
            term = "Sensei-updated",
            definition = null,
            reading = null,
            category = null,
            notes = null,
            language = null,
        )

        awaitCondition { vm.entry.value?.term == "Sensei-updated" }
        assertEquals(12345L, vm.entry.value?.dateAdded)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryFormViewModelTest"`
Expected: FAIL to compile — `DictionaryFormViewModel` is an unresolved reference.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class DictionaryFormViewModel(
    private val id: Long?,
    private val repository: DictionaryRepository,
) : ViewModel() {

    val entry: StateFlow<DictionaryEntry?> = (id?.let { repository.getById(it) } ?: flowOf(null))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun save(
        term: String,
        definition: String?,
        reading: String?,
        category: String?,
        notes: String?,
        language: String?,
    ) {
        val existing = entry.value
        val toSave = existing?.copy(
            term = term,
            definition = definition,
            reading = reading,
            category = category,
            notes = notes,
            language = language,
        ) ?: DictionaryEntry(
            term = term,
            definition = definition,
            reading = reading,
            category = category,
            notes = notes,
            language = language,
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.save(toSave)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryFormViewModelTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormViewModel.kt \
        kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormViewModelTest.kt
git commit -m "feat(vm): add DictionaryFormViewModel for create/edit"
```

---

### Task 10: Register ViewModels in `ViewModelModule.kt`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt`

**Interfaces:**
- Consumes: `DictionaryListViewModel` (Task 7), `DictionaryDetailViewModel` (Task 8), `DictionaryFormViewModel` (Task 9).

- [ ] **Step 1: Add imports**

Add after the existing `AllNotesViewModel`/`DetailsNotesViewModel` imports:

```kotlin
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryDetailViewModel
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryFormViewModel
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryListViewModel
```

- [ ] **Step 2: Register the three ViewModels**

Add after `viewModelOf(::AllNotesViewModel)` in the `viewModels` module:

```kotlin
    viewModelOf(::DictionaryListViewModel)
    viewModelOf(::DictionaryDetailViewModel)
    viewModelOf(::DictionaryFormViewModel)
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt
git commit -m "feat(di): register dictionary ViewModels"
```

---

### Task 11: `DictionaryListScreen`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListScreen.kt`

**Interfaces:**
- Consumes: `DictionaryListViewModel` (Task 7), `DictionaryEntry`, `DictionarySort` (Task 3).
- Produces: `@Composable fun DictionaryListScreen(onBackPress: () -> Unit = {}, onEntryClick: (Long) -> Unit = {}, onAddClick: () -> Unit = {}, vm: DictionaryListViewModel = koinViewModel())`.

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionarySort
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryListScreen(
    onBackPress: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {},
    onAddClick: () -> Unit = {},
    vm: DictionaryListViewModel = koinViewModel(),
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var entryPendingDelete by remember { mutableStateOf<DictionaryEntry?>(null) }

    entryPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Delete \"${pending.term}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(pending)
                    entryPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionary") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Term (A-Z)") },
                            onClick = {
                                vm.updateSort(DictionarySort.Term)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date Added") },
                            onClick = {
                                vm.updateSort(DictionarySort.DateAdded)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Category") },
                            onClick = {
                                vm.updateSort(DictionarySort.Category)
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "New Entry")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    vm.updateQuery(q)
                },
                placeholder = { Text("Search term, definition, or category…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.term) },
                        supportingContent = entry.definition?.let {
                            { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        },
                        leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { entryPendingDelete = entry }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${entry.term}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntryClick(entry.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryListScreen.kt
git commit -m "feat(ui): add DictionaryListScreen with search, sort, and delete confirmation"
```

---

### Task 12: `DictionaryDetailScreen`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailScreen.kt`

**Interfaces:**
- Consumes: `DictionaryDetailViewModel` (Task 8), `DictionaryEntry` (Task 3).
- Produces: `@Composable fun DictionaryDetailScreen(onBackPress: () -> Unit = {}, onEditClick: (Long) -> Unit = {}, vm: DictionaryDetailViewModel)`.

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryDetailScreen(
    onBackPress: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    vm: DictionaryDetailViewModel,
) {
    val entry by vm.entry.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onBackPress()
    }

    val current = entry

    if (showDeleteDialog && current != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete entry?") },
            text = { Text("Delete \"${current.term}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete()
                    showDeleteDialog = false
                    deleted = true
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.term.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { onEditClick(current.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (current != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                DetailField("Reading", current.reading)
                DetailField("Definition", current.definition)
                DetailField("Category", current.category)
                DetailField("Notes", current.notes)
                DetailField("Language", current.language)
                DetailField("Date Added", current.dateAdded.toString())
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryDetailScreen.kt
git commit -m "feat(ui): add DictionaryDetailScreen"
```

---

### Task 13: `DictionaryFormScreen`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormScreen.kt`

**Interfaces:**
- Consumes: `DictionaryFormViewModel` (Task 9), `DictionaryEntry` (Task 3).
- Produces: `@Composable fun DictionaryFormScreen(onDone: () -> Unit = {}, vm: DictionaryFormViewModel)`.

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryFormScreen(
    onDone: () -> Unit = {},
    vm: DictionaryFormViewModel,
) {
    val entry by vm.entry.collectAsStateWithLifecycle()
    val isEdit = entry != null

    var term by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var reading by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(entry) {
        val current = entry
        if (current != null && !prefilled) {
            term = current.term
            definition = current.definition.orEmpty()
            reading = current.reading.orEmpty()
            category = current.category.orEmpty()
            notes = current.notes.orEmpty()
            language = current.language.orEmpty()
            prefilled = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Entry" else "New Entry") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        enabled = term.isNotBlank(),
                        onClick = {
                            vm.save(
                                term = term,
                                definition = definition.ifBlank { null },
                                reading = reading.ifBlank { null },
                                category = category.ifBlank { null },
                                notes = notes.ifBlank { null },
                                language = language.ifBlank { null },
                            )
                            onDone()
                        }
                    ) { Text("Save") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                label = { Text("Term *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = reading,
                onValueChange = { reading = it },
                label = { Text("Reading") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                label = { Text("Definition") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Language") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            if (isEdit) {
                Text(
                    text = "Added: ${entry?.dateAdded}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dictionary/DictionaryFormScreen.kt
git commit -m "feat(ui): add DictionaryFormScreen for create and edit"
```

---

### Task 14: Wire the three screens into `Nav3Graph.kt`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt`

**Interfaces:**
- Consumes: `Screen.DictionaryScreen`, `.Detail`, `.Form` (Task 5); `DictionaryListScreen` (Task 11), `DictionaryDetailScreen` (Task 12), `DictionaryFormScreen` (Task 13).

- [ ] **Step 1: Add imports**

Add after the existing `com.programmersbox.kmpuiviews.presentation.notes.NotesScreen` import:

```kotlin
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryDetailScreen
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryFormScreen
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryListScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
```

(If `koinViewModel` or `parametersOf` are already imported in this file, skip the duplicate — check the existing import block first.)

- [ ] **Step 2: Add entries after the `entry<Screen.NotesScreen>` block**

```kotlin

    entry<Screen.DictionaryScreen> {
        val navActions = LocalNavActions.current
        DictionaryListScreen(
            onBackPress = { navActions.popBackStack() },
            onEntryClick = { id -> navActions.dictionaryDetail(id) },
            onAddClick = { navActions.dictionaryForm(null) },
        )
    }

    entry<Screen.DictionaryScreen.Detail> {
        val navActions = LocalNavActions.current
        DictionaryDetailScreen(
            onBackPress = { navActions.popBackStack() },
            onEditClick = { id -> navActions.dictionaryForm(id) },
            vm = koinViewModel { parametersOf(it.id) },
        )
    }

    entry<Screen.DictionaryScreen.Form> {
        val navActions = LocalNavActions.current
        DictionaryFormScreen(
            onDone = { navActions.popBackStack() },
            vm = koinViewModel { parametersOf(it.id) },
        )
    }
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt
git commit -m "feat(nav): wire dictionary screens into Nav3Graph"
```

---

### Task 15: Settings entry point (Library screen row + search registry)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/library/LibraryScreen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt`

**Interfaces:**
- Consumes: `navActions::dictionary` (Task 6), `Screen.DictionaryScreen` (Task 5).

- [ ] **Step 1: Add the "Dictionary" row to `LibraryScreen.kt`**

Add the icon import:

```kotlin
import androidx.compose.material.icons.filled.MenuBook
```

Add the row after the `Notes` `segmentedListItem` block (after line 63):

```kotlin
            segmentedListItem(
                content = { Text("Dictionary") },
                leadingContent = { Icon(Icons.Default.MenuBook, null) },
                onClick = navActions::dictionary,
            )
```

- [ ] **Step 2: Add a searchable entry to `SettingsSearchRegistry.kt`**

Add after the `Notes` `SettingSearchItem` block (after line 77):

```kotlin
            SettingSearchItem(
                displayName = "Dictionary",
                keywords = listOf("dictionary", "glossary", "term", "vocabulary"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.DictionaryScreen),
                targetScreen = Screen.DictionaryScreen,
                highlightKey = "dictionary",
            ),
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :kmpuiviews:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/library/LibraryScreen.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt
git commit -m "feat(settings): add Dictionary entry point to Library settings and search registry"
```

---

### Task 16: Reader quick-add (`:mangaworld:shared`)

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/AppBars.kt`
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt`

**Interfaces:**
- Consumes: `navActions.dictionaryForm(id: Long?)` (Task 6), `LocalNavActions` (existing, `com.programmersbox.kmpuiviews.utils.LocalNavActions`).

- [ ] **Step 1: Add the icon import and new parameter to `ReaderTopBar` in `AppBars.kt`**

Add the icon import (alongside the existing `androidx.compose.material.icons.filled.*` imports):

```kotlin
import androidx.compose.material.icons.filled.MenuBook
```

Find the `ReaderTopBar` signature:

```kotlin
fun ReaderTopBar(
    currentChapter: String,
    onSettingsClick: () -> Unit,
    showBlur: Boolean,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean,
    onBookmarkClick: (Boolean) -> Unit = {},
    allowBookmark: Boolean = true,
    onRefreshClick: () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
```

Replace it with (adds `onAddToDictionaryClick`):

```kotlin
fun ReaderTopBar(
    currentChapter: String,
    onSettingsClick: () -> Unit,
    showBlur: Boolean,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean,
    onBookmarkClick: (Boolean) -> Unit = {},
    allowBookmark: Boolean = true,
    onRefreshClick: () -> Unit = {},
    onAddToDictionaryClick: () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
```

- [ ] **Step 2: Add the icon button to the `actions` block**

Find:

```kotlin
            IconButton(
                onClick = onRefreshClick,
            ) { Icon(Icons.Default.Refresh, null) }
            IconButton(
                onClick = onSettingsClick,
            ) { Icon(Icons.Default.Settings, null) }
```

Replace it with:

```kotlin
            IconButton(
                onClick = onRefreshClick,
            ) { Icon(Icons.Default.Refresh, null) }
            IconButton(
                onClick = onAddToDictionaryClick,
            ) { Icon(Icons.Default.MenuBook, contentDescription = "Add to Dictionary") }
            IconButton(
                onClick = onSettingsClick,
            ) { Icon(Icons.Default.Settings, null) }
```

- [ ] **Step 3: Wire it up in `ReaderCompose.kt`**

Add the import (alongside the existing `com.programmersbox.kmpuiviews.*` imports):

```kotlin
import com.programmersbox.kmpuiviews.utils.LocalNavActions
```

Add a local val in `ReadView`, right after `val scope = rememberCoroutineScope()`:

```kotlin
    val navActions = LocalNavActions.current
```

Find the `ReaderTopBar(...)` call (inside the `topBar = { AnimatedVisibility(... ) { ReaderTopBar( ... ) } }` block) and add `onAddToDictionaryClick` alongside the other callbacks:

```kotlin
                    ReaderTopBar(
                        currentChapter = viewModel
                            .currentChapterModel
                            ?.name
                            ?: remember(viewModel.currentChapter) { viewModel.chapterName(viewModel.currentChapter) }
                            ?: "Ch ${viewModel.chapterCount - viewModel.currentChapter}",
                        onSettingsClick = { settingsPopup = true },
                        onRefreshClick = viewModel::refresh,
                        onAddToDictionaryClick = { navActions.dictionaryForm(null) },
                        showBlur = blurKind.showBlur,
                        isBookmarked = isBookmarked,
                        onBookmarkClick = viewModel::toggleBookmark,
                        allowBookmark = !viewModel.currentChapterIsDownloaded,
                        windowInsets = if (includeInsets) TopAppBarDefaults.windowInsets else WindowInsets(0.dp),
                        modifier = Modifier.setBlurKind(
                            blurKindState = blurKind,
                            hazeScope = {
                                progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                                alpha = scrollAlpha
                            }
                        )
                    )
```

- [ ] **Step 4: Verify the module compiles**

Run: `./gradlew :mangaworld:shared:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/AppBars.kt \
        mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt
git commit -m "feat(reader): add Add to Dictionary quick-add button to reader top bar"
```

---

### Task 17: README update

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add a bullet under "Shared Features"**

Find:

```markdown
### Shared Features
- Log in to save your favorites and watched episodes from device to device
- Favorite to be alerted of any updates
- Share Anime/Manga and open in app!
```

Replace it with:

```markdown
### Shared Features
- Log in to save your favorites and watched episodes from device to device
- Favorite to be alerted of any updates
- Share Anime/Manga and open in app!
- Build a personal Dictionary/Glossary of terms — manage entries from Settings, or quick-add a term without leaving the page while reading in MangaWorld
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: mention the Dictionary/Glossary feature in the README"
```

---

## Final verification

- [ ] **Full test suite for the touched modules**

```bash
./gradlew :favoritesdatabase:jvmTest :kmpuiviews:jvmTest
```

Expected: BUILD SUCCESSFUL, all `Dictionary*Test` classes pass alongside the existing suite.

- [ ] **Full build**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Smoke test checklist (manual)**

1. Settings → Library → "Dictionary" row visible → tap → empty list screen opens.
2. Tap the FAB → form opens with empty fields → fill in Term only → Save → returns to list, new entry visible.
3. Tap the entry → detail screen shows all filled fields, blank fields hidden, "Date Added" shown.
4. Tap Edit → form pre-filled (including unchanged Term) → change Definition → Save → detail screen reflects the change, Date Added unchanged.
5. Tap Delete on the detail screen → confirmation dialog names the term → confirm → returns to (now empty) list.
6. Add 2+ entries with different terms/categories → search box filters as you type (across term/definition/category) → clearing search restores full list.
7. Use the sort menu → switch between Term / Date Added / Category → list re-orders accordingly.
8. Delete an entry directly from the list screen's trailing delete icon → confirmation dialog names the term → confirm → entry removed from list.
9. Open a manga chapter in the reader → tap the new dictionary icon in the top bar → form opens with an empty Term field → Save (or back) → returns directly to the reader, still on the same page.
