# Unit Testing Wave 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish real unit-test coverage and conventions across every architectural layer named in the testing spec (service/source-plugin layer, model serialization, Room DAO, repository, ViewModel, Koin DI graph) using a foundational, representative class per layer — plus wire up the missing test dependencies and document the conventions in the README.

**Architecture:** The project already has an established, if sparse, test convention: plain `kotlin.test` (not JUnit), backtick-named test functions, `kotlinx-coroutines-test`'s `runTest`, Ktor `MockEngine` for HTTP, and **hand-rolled fakes/stubs instead of a mocking framework** (see `sharedutils/kmpextensionloader/src/jvmTest/.../JvmModelMapperTest.kt` and `mangaworld/shared/src/jvmTest/.../DownloadCoreTest.kt`). This plan follows that convention rather than introducing MockK, Mockito, or Robolectric — none of the classes touched here need them. Room DAO and repository/ViewModel tests that need a real database run in `jvmTest` (not `androidUnitTest`) using Room's KMP JVM target with `BundledSQLiteDriver`, so no emulator/Robolectric is required.

**Deviation from strict TDD:** every class under test in this plan already exists and is already correct (this is a test-backfill effort, not new-feature development). So each task's steps are "write the test → run it → confirm it passes → commit", not "write a failing test → make it pass" — there is no red step because there's no production code left to write. The two dependency-wiring tasks (Task 1, and the jvmTest source-set additions inside Tasks 4/6) are build-config changes verified by a successful Gradle sync/compile, not by a test result.

**Tech Stack:** Kotlin Multiplatform, `kotlin.test`, `kotlinx-coroutines-test`, Turbine (new), Ktor `ktor-client-mock` (existing), Koin `koin-test` (new), Room KMP (`androidx.room3`) with `BundledSQLiteDriver` for JVM-target in-memory-equivalent (temp-file) databases.

## Global Constraints

- Follow the existing test convention exactly: `kotlin.test` assertions, backtick test names (e.g. `` `adds favorite when not incognito` ``), no mocking library — write small hand-rolled fakes for interfaces instead.
- Do not modify any production code in this plan. If a class is genuinely untestable as written (e.g. requires Android `Context`/WebView), skip it and say so — do not refactor production code to make it testable.
- Kotlin `2.4.0`, coroutines `1.11.0`, Koin BOM `4.2.2`, Room `3.0.0-rc01` (package `androidx.room3`), jvmToolchain `17` — use APIs compatible with these versions.
- New test files go in the correct source set: `commonTest` for platform-agnostic logic, `jvmTest` for anything needing the Room JVM driver or a real `viewModelScope`/`Dispatchers.Main`.
- New Gradle catalog entries go in `gradle/common.versions.toml` under `[versions]`/`[libraries]`, following the existing alias-naming style (e.g. `turbine`, `commonLibs.turbine`).

---

## Task 1: Add Turbine and Koin-test to the version catalog

**Files:**
- Modify: `gradle/common.versions.toml`

**Interfaces:**
- Produces: `commonLibs.turbine` and `commonLibs.koin.test` catalog aliases, consumed by Tasks 5, 6, 7, 8.

- [ ] **Step 1: Add the `turbine` version and library alias**

In `gradle/common.versions.toml`, add to `[versions]` (alphabetically near `sqlite`/`scanner`):

```toml
turbine = "1.2.0"
```

Add to `[libraries]`, in the `# Kotlinx` section right after `kotlin-test`:

```toml
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

- [ ] **Step 2: Add the `koin-test` library alias**

In `gradle/common.versions.toml` `[libraries]`, in the `# Koin` section right after `koinViewModelNavigation`:

```toml
koin-test = { module = "io.insert-koin:koin-test" }
```

(No `version.ref` needed — like `koinCores`, it resolves through `commonLibs.koin.bom`.)

- [ ] **Step 3: Verify the catalog parses**

Run: `./gradlew help`
Expected: no Gradle catalog/version-catalog errors (a `libs.versions.toml` typo fails fast at configuration time with a clear "Invalid TOML" or "problem in version catalog" error).

- [ ] **Step 4: Commit**

```bash
git add gradle/common.versions.toml
git commit -m "test: add Turbine and Koin-test to the version catalog"
```

---

## Task 2: kmpmodels — test the ExampleService reference source plugin

**Files:**
- Create: `kmpmodels/src/commonTest/kotlin/com/programmersbox/kmpmodels/ExampleServiceTest.kt`

**Interfaces:**
- Consumes: `com.programmersbox.kmpmodels.ExampleService` (`kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ExampleService.kt`), `KmpItemModel`/`KmpInfoModel`/`KmpChapterModel`/`KmpStorage` (`Models.kt`).

No catalog/build changes needed — `kmpmodels/build.gradle.kts` already wires `commonLibs.kotlin.test` into `commonTest`.

- [ ] **Step 1: Write the test**

```kotlin
package com.programmersbox.kmpmodels

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleServiceTest {

    private val service = ExampleService()

    @Test fun `baseUrl is the example domain`() {
        assertEquals("https://example.com/", service.baseUrl)
    }

    @Test fun `recent returns a single example item pointing back at the service`() = runTest {
        val items = service.recent(page = 1)
        assertEquals(1, items.size)
        assertEquals("Example", items[0].title)
        assertEquals(service, items[0].source)
    }

    @Test fun `itemInfo returns 10 chapters in reverse order`() = runTest {
        val item = service.recent(1)[0]
        val info = service.itemInfo(item)
        assertEquals(10, info.chapters.size)
        assertEquals("Example 9", info.chapters.first().name)
        assertEquals("Example 0", info.chapters.last().name)
    }

    @Test fun `chapterInfo returns 3 pages of storage links`() = runTest {
        val chapter = service.itemInfo(service.recent(1)[0]).chapters.first()
        val storage = service.chapterInfo(chapter)
        assertEquals(3, storage.size)
        assertEquals(listOf("Page 1", "Page 2", "Page 3"), storage.map { it.filename })
        assertEquals(chapter.url, storage[0].source)
    }

    @Test fun `getSourceInformation exposes the example package name`() {
        val info = ExampleService.getSourceInformation()
        assertEquals("com.example", info.packageName)
        assertEquals("Example", info.name)
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :kmpmodels:jvmTest --tests "com.programmersbox.kmpmodels.ExampleServiceTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 3: Commit**

```bash
git add kmpmodels/src/commonTest/kotlin/com/programmersbox/kmpmodels/ExampleServiceTest.kt
git commit -m "test: cover ExampleService as the source-plugin testing reference"
```

---

## Task 3: kmpmodels — KmpStorage serialization round-trip

**Files:**
- Create: `kmpmodels/src/commonTest/kotlin/com/programmersbox/kmpmodels/ModelsSerializationTest.kt`

**Interfaces:**
- Consumes: `com.programmersbox.kmpmodels.KmpStorage` (the only `@Serializable` model in `Models.kt`).

- [ ] **Step 1: Write the test**

```kotlin
package com.programmersbox.kmpmodels

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelsSerializationTest {

    @Test fun `KmpStorage round-trips through JSON with all fields set`() {
        val original = KmpStorage(
            sub = "English",
            source = "https://example.com/chapter/1",
            link = "https://example.com/page1.jpg",
            quality = "1080p",
            filename = "Page 1",
        )

        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<KmpStorage>(json)

        assertEquals(original, decoded)
    }

    @Test fun `KmpStorage round-trips through JSON with all fields null`() {
        val original = KmpStorage()

        val decoded = Json.decodeFromString<KmpStorage>(Json.encodeToString(original))

        assertEquals(original, decoded)
        assertNull(decoded.sub)
        assertNull(decoded.link)
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :kmpmodels:jvmTest --tests "com.programmersbox.kmpmodels.ModelsSerializationTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add kmpmodels/src/commonTest/kotlin/com/programmersbox/kmpmodels/ModelsSerializationTest.kt
git commit -m "test: verify KmpStorage JSON round-trip"
```

---

## Task 4: favoritesdatabase — wire up jvmTest and cover ItemDao

**Files:**
- Modify: `favoritesdatabase/build.gradle.kts`
- Create: `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/ItemDaoTest.kt`

**Interfaces:**
- Consumes: `ItemDao`, `ItemDatabase` (`favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/{ItemDao,ItemDatabase}.kt`), `DbModel`/`ChapterWatched`/`IncognitoSource` (`ItemModels.kt`).
- Produces: the `createInMemoryItemDatabase(): ItemDatabase` test helper pattern (temp-file Room DB + `BundledSQLiteDriver`, no migrations needed since the file starts empty) — Task 6 reuses this exact pattern in `kmpuiviews`.

- [ ] **Step 1: Add a jvmTest source set to favoritesdatabase**

In `favoritesdatabase/build.gradle.kts`, inside the `sourceSets { }` block, add after `jvmMain.dependencies { ... }`:

```kotlin
        jvmTest.dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(commonLibs.coroutinesTest)
        }
```

- [ ] **Step 2: Write the DAO test**

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

class ItemDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    private fun favorite(url: String, title: String = "Title", numChapters: Int = 0) = DbModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
        numChapters = numChapters,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("item-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.itemDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertFavorite then getAllFavoritesSync returns it`() = runTest {
        dao.insertFavorite(favorite("https://example.com/1"))

        val all = dao.getAllFavoritesSync()

        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].url)
    }

    @Test fun `getAllFavorites flow excludes soft-deleted rows`() = runTest {
        dao.insertFavorite(favorite("https://example.com/1"))
        dao.insertFavorite(favorite("https://example.com/2"))
        dao.softDeleteFavorite("https://example.com/1", timestamp = 1_000L)

        val visible = dao.getAllFavorites().first()

        assertEquals(1, visible.size)
        assertEquals("https://example.com/2", visible[0].url)
    }

    @Test fun `deleteFavorite hard-removes the row`() = runTest {
        val item = favorite("https://example.com/1")
        dao.insertFavorite(item)

        dao.deleteFavorite(item)

        assertNull(dao.getDbModelSync("https://example.com/1"))
    }

    @Test fun `containsItem reflects presence after insert and delete`() = runTest {
        val item = favorite("https://example.com/1")

        assertEquals(false, dao.containsItem("https://example.com/1").first())

        dao.insertFavorite(item)
        assertEquals(true, dao.containsItem("https://example.com/1").first())

        dao.deleteFavorite(item)
        assertEquals(false, dao.containsItem("https://example.com/1").first())
    }

    @Test fun `insertChapter and getAllChapters round-trip`() = runTest {
        val chapter = ChapterWatched(
            url = "https://example.com/1/ch1",
            name = "Chapter 1",
            favoriteUrl = "https://example.com/1",
        )

        dao.insertChapter(chapter)
        val chapters = dao.getAllChapters("https://example.com/1").first()

        assertEquals(1, chapters.size)
        assertEquals("Chapter 1", chapters[0].name)
    }

    @Test fun `insertIncognitoSource then getIncognitoSourceSync round-trips`() = runTest {
        dao.insertIncognitoSource(IncognitoSource(source = "ExampleService", name = "Example", isIncognito = true))

        val result = dao.getIncognitoSourceSync("ExampleService")

        assertTrue(result != null && result.isIncognito)
    }
}
```

- [ ] **Step 3: Run the test**

Run: `./gradlew :favoritesdatabase:jvmTest --tests "com.programmersbox.favoritesdatabase.ItemDaoTest"`
Expected: `BUILD SUCCESSFUL`, 6 tests passed.

- [ ] **Step 4: Commit**

```bash
git add favoritesdatabase/build.gradle.kts favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/ItemDaoTest.kt
git commit -m "test: add jvmTest source set and cover ItemDao with a real Room database"
```

---

## Task 5: kmpuiviews — cover CurrentSourceRepository with Turbine

**Files:**
- Modify: `kmpuiviews/build.gradle.kts`
- Create: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/repository/CurrentSourceRepositoryTest.kt`

**Interfaces:**
- Consumes: `com.programmersbox.kmpuiviews.repository.CurrentSourceRepository` (`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/CurrentSourceRepository.kt`), `com.programmersbox.kmpmodels.KmpApiService`, `ExampleService` (`kmpmodels`, already a project dependency of `kmpuiviews`).

- [ ] **Step 1: Add Turbine to the commonTest dependencies**

In `kmpuiviews/build.gradle.kts`, change the existing `commonTest` block:

```kotlin
        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
            }
        }
```

to:

```kotlin
        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.turbine)
            }
        }
```

- [ ] **Step 2: Write the test**

```kotlin
package com.programmersbox.kmpuiviews.repository

import app.cash.turbine.test
import com.programmersbox.kmpmodels.ExampleService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

class CurrentSourceRepositoryTest {

    @Test fun `asFlow starts with null`() = runTest {
        val repository = CurrentSourceRepository()

        repository.asFlow().test {
            assertNull(awaitItem())
        }
    }

    @Test fun `emit publishes the new source to asFlow`() = runTest {
        val repository = CurrentSourceRepository()
        val service = ExampleService()

        repository.asFlow().test {
            assertNull(awaitItem())
            repository.emit(service)
            val emitted = awaitItem()
            assert(emitted === service)
        }
    }

    @Test fun `tryEmit publishes synchronously without suspending`() = runTest {
        val repository = CurrentSourceRepository()
        val service = ExampleService()

        repository.tryEmit(service)

        repository.asFlow().test {
            assert(awaitItem() === service)
        }
    }
}
```

- [ ] **Step 3: Run the test**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.CurrentSourceRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/build.gradle.kts kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/repository/CurrentSourceRepositoryTest.kt
git commit -m "test: add Turbine and cover CurrentSourceRepository"
```

---

## Task 6: kmpuiviews — wire up jvmTest, add shared fakes, cover FavoritesRepository

**Files:**
- Modify: `kmpuiviews/build.gradle.kts`
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt`
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesRepositoryTest.kt`

**Interfaces:**
- Consumes: `FavoritesRepository` (`kmpuiviews/.../repository/FavoritesRepository.kt`), `KmpFirebaseConnection` / `KmpFirebaseConnection.KmpFirebaseListener` (`kmpuiviews/.../utils/FireListenerClosable.kt`), `AuthManager`/`AuthState` (`favoritesdatabase/supabase-integration/.../auth/AuthManager.kt`), `ServerRepository` (`kmpuiviews/.../domain/customserver/ServerRepository.kt`), `SystemAlerter` (`kmpuiviews/src/jvmMain/.../Platform.jvm.kt`), `ItemDao`/`ItemDatabase`/`DbModel`/`ChapterWatched` (`favoritesdatabase`).
- Produces: `FakeKmpFirebaseConnection`, `FakeKmpFirebaseListener`, `FakeAuthManager`, and `createTestItemDatabase(): ItemDatabase` in the new `testing` package — Task 7 imports all four.

- [ ] **Step 1: Add a jvmTest source set to kmpuiviews**

In `kmpuiviews/build.gradle.kts`, add a new block right after the existing `jvmMain { dependencies { ... } }` block (before `val deviceMain by creating { ... }`):

```kotlin
        jvmTest {
            dependencies {
                implementation(commonLibs.turbine)
                implementation(commonLibs.koin.test)
            }
        }
```

(`kotlin.test` and `coroutinesTest` are already inherited from `commonTest`, and Room/`BundledSQLiteDriver` are already inherited from `jvmMain`'s `favoritesdatabase` dependency.)

- [ ] **Step 2: Write the shared fakes**

```kotlin
package com.programmersbox.kmpuiviews.testing

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import io.github.jan.supabase.auth.providers.OAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File

fun createTestItemDatabase(): ItemDatabase {
    val dbFile = File.createTempFile("kmpuiviews-test", ".db").also { it.deleteOnExit() }
    return Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
}

class FakeKmpFirebaseConnection(
    private val shows: List<DbModel> = emptyList(),
) : KmpFirebaseConnection {
    override fun getAllShows(): List<DbModel> = shows
    override fun insertShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun removeShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun updateShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> = flowOf(Unit)
    override fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> = flowOf(Unit)
}

class FakeKmpFirebaseListener(
    private val showsFlow: MutableStateFlow<List<DbModel>> = MutableStateFlow(emptyList()),
) : KmpFirebaseConnection.KmpFirebaseListener {
    override fun getAllShowsFlow(): Flow<List<DbModel>> = showsFlow
    override fun getShowFlow(url: String?): Flow<DbModel?> = flowOf(showsFlow.value.find { it.url == url })
    override fun findItemByUrlFlow(url: String?): Flow<Boolean> = flowOf(showsFlow.value.any { it.url == url })
    override fun getAllEpisodesByShowFlow(showUrl: String): Flow<List<ChapterWatched>> = flowOf(emptyList())
    override fun unregister() {}
}

class FakeAuthManager(
    private val loggedIn: Boolean = false,
) : AuthManager {
    override val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override fun isLoggedIn(): Boolean = loggedIn
    override suspend fun signInWithEmail(email: String, password: String) {}
    override suspend fun signUpWithEmail(email: String, password: String) {}
    override suspend fun signInWithOAuth(provider: OAuthProvider) {}
    override suspend fun signInWithMagicLink(email: String) {}
    override suspend fun signInWithPhone(phone: String, otp: String) {}
    override suspend fun signInAnonymously() {}
    override suspend fun signOut() {}
    override suspend fun deleteAccount() {}
    override suspend fun refreshSession() {}
}
```

- [ ] **Step 3: Write the FavoritesRepository test**

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.customserver.ServerRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FakeKmpFirebaseConnection
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoritesRepositoryTest {

    private lateinit var database: ItemDatabase

    private fun repository(loggedIn: Boolean = false) = FavoritesRepository(
        dao = database.itemDao(),
        firebaseDb = FakeKmpFirebaseConnection(),
        serverRepository = ServerRepository(),
        systemAlerter = SystemAlerter(),
        authManager = FakeAuthManager(loggedIn = loggedIn),
    )

    private fun favorite(url: String) = DbModel(
        title = "Title",
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
    )

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test fun `addFavorite persists the item`() = runTest {
        repository().addFavorite(favorite("https://example.com/1"))

        val all = repository().getAllFavorites()

        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].url)
    }

    @Test fun `removeFavorite hard-deletes when logged out`() = runTest {
        val repo = repository(loggedIn = false)
        val item = favorite("https://example.com/1")
        repo.addFavorite(item)

        repo.removeFavorite(item)

        assertEquals(0, repo.getAllFavorites().size)
    }

    @Test fun `removeFavorite soft-deletes when logged in`() = runTest {
        val repo = repository(loggedIn = true)
        val item = favorite("https://example.com/1")
        repo.addFavorite(item)

        repo.removeFavorite(item)

        assertNull(database.itemDao().getDbModelSync("https://example.com/1"))
        assertEquals(1, database.itemDao().getAllFavoritesSync().size) // soft-deleted row still exists
    }

    @Test fun `isIncognito is false for a source with no incognito entry`() = runTest {
        assertEquals(false, repository().isIncognito("ExampleService"))
    }

    @Test fun `addWatched persists a chapter`() = runTest {
        val repo = repository()
        repo.addFavorite(favorite("https://example.com/1"))

        repo.addWatched(
            ChapterWatched(
                url = "https://example.com/1/ch1",
                name = "Chapter 1",
                favoriteUrl = "https://example.com/1",
            )
        )

        assertEquals(1, database.itemDao().getAllChaptersSync().size)
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.FavoritesRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/build.gradle.kts kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesRepositoryTest.kt
git commit -m "test: add jvmTest fakes and cover FavoritesRepository against a real Room database"
```

---

## Task 7: kmpuiviews — cover FavoriteViewModel end-to-end

**Files:**
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteViewModelTest.kt`

**Interfaces:**
- Consumes: `FavoriteViewModel` (`kmpuiviews/.../presentation/favorite/FavoriteViewModel.kt`), `FavoritesRepository` + fakes from Task 6, `com.programmersbox.kmpmodels.SourceRepository`.

- [ ] **Step 1: Write the test**

```kotlin
package com.programmersbox.kmpuiviews.presentation.favorite

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.customserver.ServerRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FakeKmpFirebaseConnection
import com.programmersbox.kmpuiviews.testing.FakeKmpFirebaseListener
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteViewModelTest {

    private lateinit var database: ItemDatabase
    private val testDispatcher = StandardTestDispatcher()

    private fun favorite(url: String) = DbModel(
        title = "Title $url",
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(seedFavorites: List<DbModel> = emptyList()) = FavoriteViewModel(
        sourceRepository = SourceRepository(),
        favoritesRepository = FavoritesRepository(
            dao = database.itemDao(),
            firebaseDb = FakeKmpFirebaseConnection(),
            serverRepository = ServerRepository(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
        firebaseFavoriteListener = FakeKmpFirebaseListener(),
    ).also {
        seedFavorites.forEach { db -> database.itemDao().insertFavorite(runBlockingInsert(db)) }
    }

    // Room's suspend insert needs a coroutine; the test dispatcher is not yet advanced during
    // construction, so seed synchronously via the DAO's suspend fun through runTest's scope instead.
    private fun runBlockingInsert(db: DbModel): DbModel = db

    @Test fun `starts with no favorites selected`() = runTest(testDispatcher) {
        val vm = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.listSources.isEmpty())
    }

    @Test fun `inserted favorite shows up in listSources after collection`() = runTest(testDispatcher) {
        val dao = database.itemDao()
        dao.insertFavorite(favorite("https://example.com/1"))

        val vm = FavoriteViewModel(
            sourceRepository = SourceRepository(),
            favoritesRepository = FavoritesRepository(
                dao = dao,
                firebaseDb = FakeKmpFirebaseConnection(),
                serverRepository = ServerRepository(),
                systemAlerter = SystemAlerter(),
                authManager = FakeAuthManager(),
            ),
            firebaseFavoriteListener = FakeKmpFirebaseListener(),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.listSources.size)
        assertEquals("Title https://example.com/1", vm.listSources[0].title)
        assertTrue("ExampleService" in vm.selectedSources)
    }

    @Test fun `searchText filters listSources by title`() = runTest(testDispatcher) {
        val dao = database.itemDao()
        dao.insertFavorite(favorite("https://example.com/1"))
        dao.insertFavorite(favorite("https://example.com/2"))

        val vm = FavoriteViewModel(
            sourceRepository = SourceRepository(),
            favoritesRepository = FavoritesRepository(
                dao = dao,
                firebaseDb = FakeKmpFirebaseConnection(),
                serverRepository = ServerRepository(),
                systemAlerter = SystemAlerter(),
                authManager = FakeAuthManager(),
            ),
            firebaseFavoriteListener = FakeKmpFirebaseListener(),
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.listSources.size)

        vm.searchText = androidx.compose.foundation.text.input.TextFieldState("//1")
        assertEquals(1, vm.listSources.size)
    }

    @Test fun `newSource toggles membership in selectedSources`() = runTest(testDispatcher) {
        val vm = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.newSource("SomeSource")
        assertTrue("SomeSource" in vm.selectedSources)

        vm.newSource("SomeSource")
        assertTrue("SomeSource" !in vm.selectedSources)
    }
}
```

- [ ] **Step 2: Run the tests, fix the seeding helper if it doesn't compile**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.favorite.FavoriteViewModelTest"`

The `viewModel(seedFavorites = ...)` helper above calls a suspend DAO function (`insertFavorite`) from a non-suspend context, which will not compile. Since only the no-seed case actually needs that helper (`starts with no favorites selected`), delete `runBlockingInsert` and the `seedFavorites`/`.also { }` block from `viewModel(...)` entirely — it should just be:

```kotlin
    private fun viewModel() = FavoriteViewModel(
        sourceRepository = SourceRepository(),
        favoritesRepository = FavoritesRepository(
            dao = database.itemDao(),
            firebaseDb = FakeKmpFirebaseConnection(),
            serverRepository = ServerRepository(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
        firebaseFavoriteListener = FakeKmpFirebaseListener(),
    )
```

and update its one caller (`starts with no favorites selected`) to `viewModel()` with no arguments. Re-run the same command.
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteViewModelTest.kt
git commit -m "test: cover FavoriteViewModel state updates against a real repository and DAO"
```

---

## Task 8: kmpuiviews — verify the `databases` Koin module resolves

**Files:**
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModuleTest.kt`

**Interfaces:**
- Consumes: `databases` module (`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModule.kt`), `databaseBuilder` jvm module (`Platform.jvm.kt:112`), `AppDirs` (`ca.gosyer.appdirs`, already a `jvmMain` dependency via `favoritesdatabase`).

This is intentionally narrow: it verifies the one module whose full dependency chain is already known and constructible without Firebase/Android/Supabase wiring. Extending `checkModules()` to `viewModels`/`AppModule`/`RepositoryModule` is real follow-up work (see "Out of scope" below) — those graphs need Firebase and Supabase fakes that don't exist yet.

- [ ] **Step 1: Write the test**

```kotlin
package com.programmersbox.kmpuiviews.di

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.kmpuiviews.databaseBuilder
import org.koin.test.check.checkModules
import org.koin.test.KoinTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class DatabaseModuleTest : KoinTest {

    @Test fun `databases module resolves every declared DAO and database`() {
        val tempDir = createTempDirectory("koin-database-module-test").toFile()

        checkModules {
            modules(
                databases,
                org.koin.dsl.module {
                    single<AppDirs> {
                        AppDirs("com.programmersbox.test", tempDir)
                    }
                }.apply { includes(databaseBuilder) } // no-op include kept explicit for readability
            )

            withInstance<ItemDao>()
            withInstance<BlurHashDao>()
            withInstance<HistoryDao>()
            withInstance<ListDao>()
            withInstance<RecommendationDao>()
            withInstance<HeatMapDao>()
            withInstance<ExceptionDao>()
            withInstance<BookmarkDao>()
            withInstance<NotesDao>()
        }
    }
}
```

- [ ] **Step 2: Run the test, fix the `AppDirs`/`databaseBuilder` wiring if the constructor doesn't match**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.di.DatabaseModuleTest"`

`databaseBuilder` (jvm) already provides its own `AppDirs` single (check `Platform.jvm.kt` around line 112) — if it does, delete the inline `AppDirs` module above and just pass `modules(databases, databaseBuilder)` directly, since `databases` already `includes(databaseBuilder)` internally (see `DatabaseModule.kt:26`), so passing `databases` alone may be sufficient:

```kotlin
        checkModules {
            modules(databases)

            withInstance<ItemDao>()
            withInstance<BlurHashDao>()
            withInstance<HistoryDao>()
            withInstance<ListDao>()
            withInstance<RecommendationDao>()
            withInstance<HeatMapDao>()
            withInstance<ExceptionDao>()
            withInstance<BookmarkDao>()
            withInstance<NotesDao>()
        }
```

Try the simplified version first; only fall back to manually supplying `AppDirs` if `checkModules` reports it as unresolved. Re-run the same test command after any change.
Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/di/DatabaseModuleTest.kt
git commit -m "test: verify the databases Koin module resolves with checkModules"
```

---

## Task 9: Update the README with testing documentation

**Files:**
- Modify: `README.md` (repo root)

- [ ] **Step 1: Add a "Testing" section**

Read the current `README.md` first to find the right insertion point (after any existing "Building"/"Development" section, before "License"/footer if present). Insert a section with this content:

```markdown
## Testing

### Running tests

```bash
# Run every test in the project
./gradlew test

# Run tests for one module
./gradlew :kmpuiviews:test
./gradlew :favoritesdatabase:test
./gradlew :kmpmodels:test

# Run only the JVM-target tests of a KMP module (fastest — no Android emulator)
./gradlew :kmpuiviews:jvmTest

# Run a single test class
./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.FavoritesRepositoryTest"
```

### Frameworks and libraries

- **`kotlin.test`** — assertions, multiplatform-compatible. This project does **not** use JUnit-style
  assertions or a mocking framework (no MockK/Mockito) — write small hand-rolled fakes for interfaces
  instead of mocking them. See `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt`
  for examples.
- **`kotlinx-coroutines-test`** (`runTest`, `StandardTestDispatcher`, `Dispatchers.setMain`/`resetMain`) —
  for suspend functions and Flow-driven ViewModels.
- **Turbine** (`app.cash.turbine`) — for asserting on `Flow`/`StateFlow` emissions (`repository.asFlow().test { ... }`).
- **Ktor `ktor-client-mock`** (`MockEngine`) — for testing code that makes HTTP calls via Ktor's `HttpClient`,
  without a live network call. See `mangaworld/shared/src/jvmTest/.../DownloadCoreTest.kt`.
- **Room (KMP, `androidx.room3`) with `BundledSQLiteDriver`** — DAO and repository tests that need a real
  database build one against a temp file on the JVM target (no Android emulator, no Robolectric):
  ```kotlin
  Room.databaseBuilder<ItemDatabase>(name = tempFile.absolutePath)
      .setDriver(BundledSQLiteDriver())
      .build()
  ```
- **Koin `koin-test`** (`checkModules`) — verifies a Koin module's dependency graph resolves without
  starting the full app.

### Source set conventions

| Source set | Use for |
|---|---|
| `commonTest` | Pure logic with no platform dependency (models, repositories with no Room/JVM-only APIs) |
| `jvmTest` | Anything needing Room's JVM driver, a real `viewModelScope`/`Dispatchers.Main`, or `koin-test` |
| `androidUnitTest` | Android-only classes that don't fit the two above (rare — prefer `jvmTest` when the code is KMP) |

### Adding a new test

1. Find the closest existing test for the layer you're testing (`ExampleServiceTest` for a source
   plugin, `ItemDaoTest` for a DAO, `FavoritesRepositoryTest`/`FavoriteViewModelTest` for a
   repository/ViewModel pair, `DatabaseModuleTest` for a Koin module) and copy its structure.
2. Use backtick-named test functions describing the behavior, e.g.
   `` `removeFavorite soft-deletes when logged in` ``.
3. If the class under test has an interface dependency with side effects (Firebase, Supabase, network),
   write a small fake implementing that interface in a `testing` package next to the test — do not
   reach for a mocking library.
4. If it depends on a concrete class with real side effects that can't be faked (e.g. a class doing
   Android/WebView work), that's a sign the test belongs in `jvmTest`/`androidUnitTest` with a narrower
   scope, or that the testable logic should be exercised through its pure-function seams only.

### Current coverage (as of this plan)

Foundational coverage exists for one representative class per layer: the source-plugin/service layer
(`ExampleService`), model serialization (`KmpStorage`), a Room DAO (`ItemDao`), a repository
(`CurrentSourceRepository`, `FavoritesRepository`), a ViewModel (`FavoriteViewModel`), and one Koin
module (`databases`). Ktor networking already had coverage before this pass
(`DownloadCoreTest`, `WebScraperTest`). The remaining ~25 ViewModels, ~10 repositories, and ~8 other
Koin modules follow the same patterns and are tracked as follow-up work, not yet covered.
```

- [ ] **Step 2: Sanity-check the commands in the new section actually match Gradle task names**

Run: `./gradlew :kmpuiviews:jvmTest --dry-run` and `./gradlew :favoritesdatabase:jvmTest --dry-run` and `./gradlew :kmpmodels:jvmTest --dry-run`
Expected: each prints a list of tasks (`:kmpmodels:jvmTest SKIPPED` etc.) with no "task not found" error — confirms the task names documented in the README are correct.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document the unit testing setup, frameworks, and conventions"
```

---

## Out of scope for this wave (follow-up plans)

This wave establishes one grounded, working example per layer plus the shared infra (catalog entries,
`jvmTest` source sets, fakes, README). It deliberately does not attempt exhaustive coverage in a single
pass — each of these is real follow-up work, sized like its own plan:

- The other ~25 ViewModels registered in `ViewModelModule.kt` (same pattern as `FavoriteViewModelTest`).
- The other repositories in `kmpuiviews/.../repository/` (`SourceInfoRepository`, `ListRepository`,
  `NotificationRepository`, `PrereleaseRepository`, `ChangingSettingsRepository`, `SetupRepository`,
  `BookmarkRepository`, `IncognitoRepository`, `PlatformRepository`).
- The other Room DAOs (`BlurHashDao`, `HistoryDao`, `ListDao`, `RecommendationDao`, `HeatMapDao`,
  `ExceptionDao`, `BookmarkDao`, `NotesDao`) — same temp-file-Room pattern as `ItemDaoTest`.
- `checkModules()` for `viewModels`, `AppModule`, `RepositoryModule` — blocked on writing Firebase and
  Supabase fakes those graphs need; `databases` was chosen for this wave specifically because it needed
  none.
- App-specific Koin graphs (`GenericManga`/`GenericAnime`/`GenericNovel`, `MangaApp`/`AnimeApp`/`NovelApp`).
- `source_utilities/NetworkHelper.kt` — it's OkHttp/Jsoup-based (not Ktor) and its `CloudflareInterceptor`
  is WebView-based (Android-only, effectively untestable without Robolectric); only its pure helper
  functions, if any are extracted later, would be testable as-is.
- Gson-based parsing code still present in ~14 files (`CLAUDE.md`'s in-progress Gson→kotlinx.serialization
  migration) — hold off testing those until the migration lands, to avoid testing code about to be deleted.
