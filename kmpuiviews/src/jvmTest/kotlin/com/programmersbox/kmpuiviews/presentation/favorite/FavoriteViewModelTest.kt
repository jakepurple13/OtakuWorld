package com.programmersbox.kmpuiviews.presentation.favorite

import kotlinx.coroutines.CompletableDeferred
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.clearAndJoin
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

    // Stands in for the UI thread: the ViewModel writes its state here (Main) and the test reads it
    // here, as in the app. Reading derived state on another thread while it is written can leave a
    // derivedStateOf (sections) cached with stale inputs.
    private val uiDispatcher = Dispatchers.Default.limitedParallelism(1)

    // The ViewModel observes Room flows on Room's own dispatcher and snapshotFlow on the
    // global snapshot, so wait in real time and push snapshot apply notifications ourselves.
    private suspend fun awaitCondition(condition: () -> Boolean) {
        withContext(uiDispatcher) {
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

    private fun viewModel(ai: FakeAiText = FakeAiText(engine = null)) = FavoriteViewModel(
        sourceRepository = SourceRepository(),
        favoritesRepository = FavoritesRepository(
            dao = database.itemDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
            sourceRepository = SourceRepository(),
        ),
        searchRepository = FavoritesSearchRepository(database.itemDao(), notes.notesDao()),
        aiText = ai,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun FavoriteViewModel.search(text: String) {
        searchText.setTextAndPlaceCursorAtEnd(text)
        awaitCondition { searchQuery == text }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(uiDispatcher)
        database = createTestItemDatabase()
        notes = createTestNotesDatabase()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clearAndJoin()
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

    private val describing = "that isekai where the guy is a slime"

    @Test
    fun `short queries cannot describe`() = runTest {
        val vm = viewModel(FakeAiText())
        vm.search("blue lock")
        assertFalse(vm.canDescribe)
        vm.search(describing)
        awaitCondition { vm.canDescribe }
    }

    @Test
    fun `describe searches any keyword`() = runTest {
        database.itemDao().insertFavorite(favorite("u1", "That Time I Got Reincarnated as a Slime"))
        database.itemDao().insertFavorite(favorite("u2", "Dandadan"))
        val vm = viewModel(FakeAiText(output = { _, _ -> flowOf("Slime\nreincarnated") }))
        vm.search(describing)
        awaitCondition { vm.canDescribe }
        assertTrue(vm.sections.isEmpty())
        vm.describe()
        awaitCondition { vm.keywords == listOf("slime", "reincarnated") && vm.sections.isNotEmpty() }
        assertEquals("That Time I Got Reincarnated as a Slime", vm.sections.single().groups.single().title)
        assertFalse(vm.describing)
    }

    @Test
    fun `typing again clears keywords`() = runTest {
        val vm = viewModel(FakeAiText(output = { _, _ -> flowOf("slime") }))
        vm.search(describing)
        awaitCondition { vm.canDescribe }
        vm.describe()
        awaitCondition { vm.keywords != null }
        vm.search("blue")
        awaitCondition { vm.keywords == null }
    }

    @Test
    fun `removing the last keyword returns to normal search`() = runTest {
        val vm = viewModel(FakeAiText(output = { _, _ -> flowOf("slime\nisekai") }))
        vm.search(describing)
        awaitCondition { vm.canDescribe }
        vm.describe()
        awaitCondition { vm.keywords != null }
        vm.removeKeyword("slime")
        assertEquals(listOf("isekai"), vm.keywords)
        vm.removeKeyword("isekai")
        assertNull(vm.keywords)
    }

    @Test
    fun `failed describe keeps normal search`() = runTest {
        val vm = viewModel(FakeAiText(output = { _, _ -> flow { error("boom") } }))
        vm.search(describing)
        awaitCondition { vm.canDescribe }
        vm.describe()
        awaitCondition { !vm.describing }
        assertNull(vm.keywords)
        assertEquals(describing, vm.searchQuery)
    }

    @Test
    fun `describe availability follows settings`() = runTest {
        val ai = FakeAiText()
        val vm = viewModel(ai)
        vm.search(describing)
        awaitCondition { vm.canDescribe }
        ai.engine = null
        awaitCondition { !vm.canDescribe }
    }

    @Test
    fun `typing during describe cancels it`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(FakeAiText(output = { _, _ -> flow { gate.await(); emit("slime") } }))
        vm.search(describing)
        awaitCondition { vm.canDescribe }
        vm.describe()
        awaitCondition { vm.describing }
        vm.search("the elf mage who outlives her party")
        awaitCondition { !vm.describing && vm.canDescribe }
        gate.complete(Unit)
        assertNull(vm.keywords)
    }

    @Test
    fun `cjk descriptions can describe`() = runTest {
        val vm = viewModel(FakeAiText())
        vm.search("スライムに転生した男の話")
        awaitCondition { vm.canDescribe }
    }

    @Test
    fun `tabs count as word breaks`() = runTest {
        val vm = viewModel(FakeAiText())
        vm.search("that\tisekai\twith\tslime")
        awaitCondition { vm.canDescribe }
    }

    @Test
    fun `punctuation does not count as words`() = runTest {
        val vm = viewModel(FakeAiText())
        vm.search("?? !! -- .. ok")
        assertFalse(vm.canDescribe)
    }
}
