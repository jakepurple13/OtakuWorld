package com.programmersbox.kmpuiviews.presentation.details

import com.programmersbox.kmpuiviews.testing.clearAndJoin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.aitext.AiEngineInfo
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FakeKmpGenericInfo
import com.programmersbox.kmpuiviews.testing.FakeTranslationHandler
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedCoverBitmaps
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedItemKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetailsViewModelTest {

    private val viewModelStore = ViewModelStore()

    private lateinit var itemDbFile: File
    private lateinit var itemDatabase: ItemDatabase

    private lateinit var bookmarkDbFile: File
    private lateinit var bookmarkDatabase: BookmarkDatabase

    private lateinit var blurHashDbFile: File
    private lateinit var blurHashDatabase: BlurHashDatabase

    private lateinit var exceptionDbFile: File
    private lateinit var exceptionDatabase: ExceptionDatabase

    private val sourceRepository = SourceRepository()

    // The ViewModel observes several Room-generated Flows, which emit on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive those emissions, so wait for them with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    // snapshotFlow only sees a write after the apply notification, which Compose sends every frame in the app
    private suspend fun setDescription(vm: DetailsViewModel, text: String) {
        // the load assigns the description right after Success, so let it land before overriding it
        awaitCondition { vm.description.isNotEmpty() }
        vm.description = text
        Snapshot.sendApplyNotifications()
    }

    private fun details(source: String = "ExampleService") = Screen.DetailsScreen.Details(
        title = "Example",
        description = "Example",
        url = "https://example.com/",
        imageUrl = "https://picsum.photos/200/300",
        source = source,
    )

    private fun viewModel(
        details: Screen.DetailsScreen.Details? = details(),
        sharedCoverBitmaps: SharedCoverBitmaps = SharedCoverBitmaps(),
        aiText: AiText = FakeAiText(),
        translationHandler: TranslationHandler = FakeTranslationHandler(),
        tldrCache: TldrCache = TldrCache(),
    ) = DetailsViewModel(
        details = details,
        genericInfo = FakeKmpGenericInfo(),
        blurHashDao = blurHashDatabase.blurDao(),
        sourceRepository = sourceRepository,
        favoritesRepository = FavoritesRepository(
            dao = itemDatabase.itemDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
            sourceRepository = sourceRepository,
        ),
        translationHandler = translationHandler,
        exceptionDao = exceptionDatabase.exceptionDao(),
        imageModifier = ImageModifier(),
        bookmarkRepository = BookmarkRepository(
            dao = bookmarkDatabase.bookmarkDao(),
            authManager = FakeAuthManager(),
        ),
        sharedCoverBitmaps = sharedCoverBitmaps,
        aiText = aiText,
        tldrCache = tldrCache,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)

        sourceRepository.setSources(listOf(ExampleService.getSourceInformation()))

        itemDbFile = File.createTempFile("details-vm-item-test", ".db").also { it.deleteOnExit() }
        itemDatabase = Room.databaseBuilder<ItemDatabase>(name = itemDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        bookmarkDbFile = File.createTempFile("details-vm-bookmark-test", ".db").also { it.deleteOnExit() }
        bookmarkDatabase = Room.databaseBuilder<BookmarkDatabase>(name = bookmarkDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        blurHashDbFile = File.createTempFile("details-vm-blurhash-test", ".db").also { it.deleteOnExit() }
        blurHashDatabase = Room.databaseBuilder<BlurHashDatabase>(name = blurHashDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        exceptionDbFile = File.createTempFile("details-vm-exception-test", ".db").also { it.deleteOnExit() }
        exceptionDatabase = Room.databaseBuilder<ExceptionDatabase>(name = exceptionDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clearAndJoin()
        Dispatchers.resetMain()
        runBlocking { Cached.cache.cleanUp() }
        itemDatabase.close()
        itemDbFile.delete()
        bookmarkDatabase.close()
        bookmarkDbFile.delete()
        blurHashDatabase.close()
        blurHashDbFile.delete()
        exceptionDatabase.close()
        exceptionDbFile.delete()
    }

    @Test
    fun `starts with the cover handed off by the tapped card`() {
        val bitmaps = SharedCoverBitmaps()
        val cover = FakeImageBitmap(width = 2, height = 3)
        bitmaps.offer(SharedItemKey(url = "https://example.com/", source = "ExampleService"), cover)

        val vm = viewModel(sharedCoverBitmaps = bitmaps)

        assertNotNull(vm.imageBitmap)
        assertEquals(2, vm.imageBitmap?.width)
        // Taken once, so a later Details for the same item loads normally
        assertNull(bitmaps.take(SharedItemKey(url = "https://example.com/", source = "ExampleService")))
    }

    @Test
    fun `ignores a handed off cover for a different item`() {
        val bitmaps = SharedCoverBitmaps()
        bitmaps.offer(SharedItemKey(url = "https://other.com/", source = "ExampleService"), FakeImageBitmap(width = 2, height = 3))

        val vm = viewModel(sharedCoverBitmaps = bitmaps)

        assertNull(vm.imageBitmap)
    }

    @Test
    fun `uses cached info instead of refetching from source`() = runTest {
        val service = ExampleService()
        val cachedInfo = service.itemInfo(
            KmpItemModel(
                title = "Cached Title",
                description = "Cached Description",
                url = "https://example.com/",
                imageUrl = "https://picsum.photos/200/300",
                source = service,
            )
        ).copy(title = "Cached Title")
        Cached.cache["https://example.com/"] = cachedInfo

        val vm = viewModel()

        awaitCondition { vm.currentState is DetailState.Success }

        val state = vm.currentState as DetailState.Success
        assertEquals("Cached Title", state.info.title)
    }

    @Test
    fun `favoriteAction Add persists favorite and flips currentState to Remove`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val addAction = (vm.currentState as DetailState.Success).action as DetailFavoriteAction.Add
        vm.favoriteAction(addAction)

        awaitCondition { vm.favoriteListener }

        assertTrue(itemDatabase.itemDao().getDbModelSync("https://example.com/") != null)
        assertTrue((vm.currentState as DetailState.Success).action is DetailFavoriteAction.Remove)
    }

    @Test
    fun `favoriteAction Remove deletes favorite and flips currentState back to Add`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val addAction = (vm.currentState as DetailState.Success).action as DetailFavoriteAction.Add
        vm.favoriteAction(addAction)
        awaitCondition { vm.favoriteListener }

        val removeAction = (vm.currentState as DetailState.Success).action as DetailFavoriteAction.Remove
        vm.favoriteAction(removeAction)
        awaitCondition { !vm.favoriteListener }

        assertTrue(itemDatabase.itemDao().getDbModelSync("https://example.com/") == null)
        assertTrue((vm.currentState as DetailState.Success).action is DetailFavoriteAction.Add)
    }

    @Test
    fun `toggleBookmark adds then removes a bookmark for a chapter`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val chapter = (vm.currentState as DetailState.Success).info.chapters.first()

        vm.toggleBookmark(chapter)
        awaitCondition { vm.bookmarkedChapterUrls.contains(chapter.url) }
        assertTrue(chapter.url in vm.bookmarkedChapterUrls)

        vm.toggleBookmark(chapter)
        awaitCondition { !vm.bookmarkedChapterUrls.contains(chapter.url) }
        assertTrue(chapter.url !in vm.bookmarkedChapterUrls)
    }

    @Test
    fun `markAs true adds a watched chapter and reread clears it`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val chapter = (vm.currentState as DetailState.Success).info.chapters.first()

        vm.markAs(chapter, true)
        awaitCondition { vm.chapters.any { it.url == chapter.url } }
        assertTrue(vm.chapters.any { it.url == chapter.url })

        vm.reread()
        awaitCondition { vm.chapters.isEmpty() }
        assertTrue(vm.chapters.isEmpty())
    }

    @Test
    fun `markAs does not stamp a read time, so it never counts as reading that day`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val chapter = (vm.currentState as DetailState.Success).info.chapters.first()

        vm.markAs(chapter, true)
        awaitCondition { vm.chapters.any { it.url == chapter.url } }
        assertEquals(0L, vm.chapters.first { it.url == chapter.url }.createdAt)
    }

    @Test
    fun `selection toggle selectAll invert and clear`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        val chapters = (vm.currentState as DetailState.Success).info.chapters

        vm.toggleSelection(chapters[0].url)
        assertTrue(vm.isSelecting)
        assertEquals(setOf(chapters[0].url), vm.selectedChapterUrls.toSet())

        vm.toggleSelection(chapters[0].url)
        assertTrue(vm.selectedChapterUrls.isEmpty())
        assertFalse(vm.isSelecting)

        vm.toggleSelection(chapters[0].url)
        vm.selectAllChapters()
        assertEquals(10, vm.selectedChapterUrls.size)

        vm.clearSelection()
        assertTrue(vm.selectedChapterUrls.isEmpty())

        vm.toggleSelection(chapters[0].url)
        vm.toggleSelection(chapters[1].url)
        vm.invertSelection()
        assertEquals(8, vm.selectedChapterUrls.size)
        assertFalse(chapters[0].url in vm.selectedChapterUrls)
        assertFalse(chapters[1].url in vm.selectedChapterUrls)

        vm.selectAllChapters()
        vm.invertSelection()
        assertTrue(vm.selectedChapterUrls.isEmpty())
        assertFalse(vm.isSelecting)
    }

    @Test
    fun `setBookmarked true on mixed selection bookmarks all`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        val chapters = (vm.currentState as DetailState.Success).info.chapters
        val picked = chapters.take(3)

        vm.toggleBookmark(picked[0])
        awaitCondition { picked[0].url in vm.bookmarkedChapterUrls }

        vm.setBookmarked(picked, true)
        awaitCondition { picked.all { it.url in vm.bookmarkedChapterUrls } }
        assertTrue(picked.all { it.url in vm.bookmarkedChapterUrls })

        vm.setBookmarked(picked, false)
        awaitCondition { picked.none { it.url in vm.bookmarkedChapterUrls } }
        assertTrue(picked.none { it.url in vm.bookmarkedChapterUrls })
    }

    @Test
    fun `restoreReadState restores per-chapter prior state`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        val chapters = (vm.currentState as DetailState.Success).info.chapters
        val picked = chapters.take(2)

        vm.markAs(picked[0], true)
        awaitCondition { vm.chapters.any { it.url == picked[0].url } }

        vm.markChaptersAs(picked, true)
        awaitCondition { picked.all { c -> vm.chapters.any { it.url == c.url } } }

        vm.restoreReadState(picked, setOf(picked[0].url))
        awaitCondition {
            vm.chapters.any { it.url == picked[0].url } && vm.chapters.none { it.url == picked[1].url }
        }
        assertTrue(vm.chapters.any { it.url == picked[0].url })
        assertTrue(vm.chapters.none { it.url == picked[1].url })
    }

    @Test
    fun `markChaptersAs leaves already read chapters untouched`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        val chapters = (vm.currentState as DetailState.Success).info.chapters
        val picked = chapters.take(2)

        itemDatabase.itemDao().insertChapter(
            ChapterWatched(
                url = picked[0].url,
                name = picked[0].name,
                favoriteUrl = "https://example.com/",
                supabaseId = "synced-id",
                createdAt = 123L,
            )
        )
        awaitCondition { vm.chapters.any { it.url == picked[0].url } }

        vm.markChaptersAs(picked, true)
        awaitCondition { vm.chapters.any { it.url == picked[1].url } }

        val kept = vm.chapters.first { it.url == picked[0].url }
        assertEquals("synced-id", kept.supabaseId)
        assertEquals(123L, kept.createdAt)
    }

    @Test
    fun `restoreBookmarkState restores per-chapter prior state`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        val chapters = (vm.currentState as DetailState.Success).info.chapters
        val picked = chapters.take(2)

        vm.toggleBookmark(picked[0])
        awaitCondition { picked[0].url in vm.bookmarkedChapterUrls }

        vm.setBookmarked(picked, true)
        awaitCondition { picked.all { it.url in vm.bookmarkedChapterUrls } }

        vm.restoreBookmarkState(picked, setOf(picked[0].url))
        awaitCondition {
            picked[0].url in vm.bookmarkedChapterUrls && picked[1].url !in vm.bookmarkedChapterUrls
        }
        assertTrue(picked[0].url in vm.bookmarkedChapterUrls)
        assertTrue(picked[1].url !in vm.bookmarkedChapterUrls)
    }

    @Test
    fun `translateDescription updates description via TranslationHandler`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val progress = mutableStateOf(false)
        vm.translateDescription(progress)

        awaitCondition { !progress.value }
        assertEquals("Example", vm.description)
    }

    @Test
    fun `tldr chip hidden for short description`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(401))
        awaitCondition { vm.tldrAvailable.value }

        setDescription(vm, "a".repeat(400))
        awaitCondition { !vm.tldrAvailable.value }
        assertFalse(vm.tldrAvailable.value)
    }

    @Test
    fun `tldr chip hidden when no engine`() = runTest {
        val ai = FakeAiText()
        val vm = viewModel(aiText = ai)
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))
        awaitCondition { vm.tldrAvailable.value }

        ai.engine = null
        setDescription(vm, "a".repeat(501))
        awaitCondition { !vm.tldrAvailable.value }
        assertFalse(vm.tldrAvailable.value)
    }

    @Test
    fun `tldr chip hidden for blank description`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))
        awaitCondition { vm.tldrAvailable.value }

        setDescription(vm, " ".repeat(500))
        awaitCondition { !vm.tldrAvailable.value }
        assertFalse(vm.tldrAvailable.value)
    }

    @Test
    fun `showTldr streams bullets to Done`() = runTest {
        val ai = FakeAiText(tldrOutput = listOf("- one\n", "- two"))
        val vm = viewModel(aiText = ai)
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))

        vm.showTldr()
        awaitCondition { vm.tldr.value is AiTextState.Done }

        val done = vm.tldr.value as AiTextState.Done
        assertEquals("- one\n- two", done.text)
        assertEquals(FakeAiText.ENGINE, done.engine)
        assertFalse(vm.tldrTranslated.value)
        assertFalse(vm.tldrTranslating.value)
    }

    @Test
    fun `showTldr uses cache on second call`() = runTest {
        val ai = FakeAiText()
        val vm = viewModel(aiText = ai)
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))

        vm.showTldr()
        awaitCondition { vm.tldr.value is AiTextState.Done }
        vm.hideTldr()
        vm.showTldr()
        awaitCondition { vm.tldr.value is AiTextState.Done }

        assertEquals(1, ai.tldrCalls.size)
    }

    @Test
    fun `translated description marks footer`() = runTest {
        val ai = FakeAiText()
        val translator = object : TranslationHandler by FakeTranslationHandler() {
            override suspend fun translate(textToTranslate: String): String = "TRANSLATED"
        }
        val vm = viewModel(aiText = ai, translationHandler = translator)
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))

        vm.showTldr()
        awaitCondition { vm.tldr.value is AiTextState.Done }

        assertEquals(listOf("TRANSLATED"), ai.tldrCalls)
        assertTrue(vm.tldrTranslated.value)
    }

    @Test
    fun `translation failure summarizes the original`() = runTest {
        val ai = FakeAiText()
        val translator = object : TranslationHandler by FakeTranslationHandler() {
            override suspend fun translate(textToTranslate: String): String = error("no model")
        }
        val vm = viewModel(aiText = ai, translationHandler = translator)
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))

        vm.showTldr()
        awaitCondition { vm.tldr.value is AiTextState.Done }

        assertEquals(listOf("a".repeat(500)), ai.tldrCalls)
        assertFalse(vm.tldrTranslated.value)
        assertFalse(vm.tldrTranslating.value)
    }

    @Test
    fun `hideTldr returns to Idle and does not cancel cache`() = runTest {
        val cache = TldrCache()
        val vm = viewModel(tldrCache = cache)
        awaitCondition { vm.currentState is DetailState.Success }
        setDescription(vm, "a".repeat(500))

        vm.showTldr()
        awaitCondition { vm.tldr.value is AiTextState.Done }
        vm.hideTldr()

        awaitCondition { vm.tldr.value == AiTextState.Idle }
        assertEquals(AiTextState.Idle, vm.tldr.value)
        assertNotNull(cache["https://example.com/"])
    }

    @Test
    fun `null details produces null itemModel and Loading never resolves`() = runTest {
        val vm = viewModel(details = null)

        assertEquals(null, vm.itemModel)
        assertEquals(DetailState.Loading, vm.currentState)
    }
}

// The JVM test runtime has no Compose graphics backend, so a real ImageBitmap can't be created here
private class FakeImageBitmap(
    override val width: Int,
    override val height: Int,
) : ImageBitmap {
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha: Boolean = false
    override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888
    override fun prepareToDraw() = Unit
    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ) = Unit
}

private class FakeAiText(
    var engine: AiEngineInfo? = ENGINE,
    private val tldrOutput: List<String> = listOf("- a\n- b\n- c"),
) : AiText {
    val tldrCalls = mutableListOf<String>()

    override suspend fun engineFor(feature: AiFeature, inputLength: Int): AiEngineInfo? = engine
    override fun tldr(description: String): Flow<String> {
        tldrCalls += description
        return kotlinx.coroutines.flow.flow { tldrOutput.forEach { emit(it) } }
    }

    override fun recap(previousChapterText: String): Flow<String> = flowOf()
    override fun proofread(text: String): Flow<String> = flowOf()
    override fun rewrite(text: String, style: RewriteStyle): Flow<String> = flowOf()
    override fun listNames(titles: List<String>): Flow<String> = flowOf()
    override fun listDescription(titles: List<String>): Flow<String> = flowOf()

    companion object {
        val ENGINE = AiEngineInfo(label = "Fake", onDevice = true, destination = null)
    }
}
