package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.FavoritesSearchRepository
import com.programmersbox.kmpuiviews.repository.MatchField
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.kmpuiviews.testing.createTestNotesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var database: ItemDatabase
    private lateinit var notes: NotesDatabase

    // The ViewModel observes Room flows on Room's own dispatcher and snapshotFlow on the
    // global snapshot, so wait in real time and push snapshot apply notifications ourselves.
    private suspend fun awaitCondition(condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) {
                while (true) {
                    Snapshot.sendApplyNotifications()
                    if (condition()) break
                    delay(10)
                }
            }
        }
    }

    private fun favorite(url: String, title: String = "Title $url", source: String = "ExampleService") = DbModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = source,
    )

    private fun viewModel() = FavoriteViewModel(
        sourceRepository = SourceRepository(),
        favoritesRepository = FavoritesRepository(
            dao = database.itemDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
        searchRepository = FavoritesSearchRepository(database.itemDao(), notes.notesDao()),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun FavoriteViewModel.search(text: String) {
        searchText.setTextAndPlaceCursorAtEnd(text)
        awaitCondition { searchQuery == text }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        database = createTestItemDatabase()
        notes = createTestNotesDatabase()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        notes.close()
    }

    @Test
    fun `inserted favorite shows up in sections after collection`() = runTest {
        database.itemDao().insertFavorite(favorite("https://example.com/1"))
        val vm = viewModel()

        awaitCondition { vm.sections.isNotEmpty() }

        assertEquals("Title https://example.com/1", vm.sections.single().groups.single().title)
    }

    @Test
    fun `seeAll recent sorts by added and sets the quick filter`() {
        val vm = viewModel()

        vm.seeAll(QuickFilter.RECENT)
        assertEquals(FavoriteSort.ADDED, vm.sort)
        assertEquals(setOf(QuickFilter.RECENT), vm.quickFilters)

        vm.seeAll(QuickFilter.MULTI_SOURCE)
        assertEquals(FavoriteSort.TITLE, vm.sort)
        assertEquals(setOf(QuickFilter.MULTI_SOURCE), vm.quickFilters)
    }

    @Test
    fun `onlySource leaves exactly that source`() {
        val vm = viewModel()
        vm.toggleSource("A")

        vm.onlySource("B")

        assertEquals(setOf("B"), vm.selectedSources)
    }

    @Test
    fun `selectNone removes only the visible sources`() {
        val vm = viewModel()
        vm.selectAll(listOf("A", "B", "C"))

        vm.selectNone(listOf("A", "B"))

        assertEquals(setOf("C"), vm.selectedSources)
    }

    @Test
    fun `activeFilterCount counts sources once`() {
        val vm = viewModel()
        vm.toggleSource("A")
        vm.toggleSource("B")
        vm.toggleQuickFilter(QuickFilter.RECENT)

        assertEquals(2, vm.activeFilterCount)

        vm.resetFilters()
        assertEquals(0, vm.activeFilterCount)
    }

    @Test
    fun `typing a title word gives a title match section`() = runTest {
        database.itemDao().insertFavorite(favorite("u1", "Blue Lock"))
        database.itemDao().insertFavorite(favorite("u2", "Dandadan"))
        val vm = viewModel()
        awaitCondition { vm.sections.isNotEmpty() }

        vm.search("lock")

        assertTrue(vm.isSearching)
        assertEquals(SectionHeader.Match(MatchField.TITLE), vm.sections.single().header)
        assertEquals("Blue Lock", vm.sections.single().groups.single().title)
    }

    @Test
    fun `note hit for a non favorite gives no results`() = runTest {
        database.itemDao().insertFavorite(favorite("u1", "Blue Lock"))
        notes.notesDao().upsertNote(NoteItem("orphan", "Gone", "tournament arc"))
        val vm = viewModel()
        awaitCondition { vm.sections.isNotEmpty() }

        vm.search("tournament")

        assertTrue(vm.sections.isEmpty())
    }

    @Test
    fun `filters apply to search results`() = runTest {
        database.itemDao().insertFavorite(favorite("u1", "Blue Lock", source = "A"))
        database.itemDao().insertFavorite(favorite("u2", "Lock Up", source = "B"))
        val vm = viewModel()
        awaitCondition { vm.sections.isNotEmpty() }
        vm.onlySource("B")

        vm.search("lock")

        assertEquals(listOf("Lock Up"), vm.sections.flatMap { s -> s.groups.map { it.title } })
    }
}
