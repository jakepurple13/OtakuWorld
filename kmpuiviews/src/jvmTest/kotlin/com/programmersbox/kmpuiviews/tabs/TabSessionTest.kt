package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.koin.core.annotation.KoinExperimentalAPI
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Stands in for an app's reader key, which lives outside kmpuiviews. */
@Serializable
private data class FakeReader(val downloaded: Boolean, val url: String = "chapter") : NavKey

private val FakeReaderSerializers = NavKeySerializers(
    SerializersModule { polymorphic(NavKey::class) { subclass(FakeReader::class) } },
    restorable = { it !is FakeReader || it.downloaded },
)

class TabSessionTest {

    private val dir: File = Files.createTempDirectory("tab-session").toFile()
    private val file = File(dir, "tabs-session.json")
    private val store = TabSessionStore(file, navKeyJson(listOf(CoreNavKeySerializers)))

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun newController() = TabsController(
        homeNav = TabNavigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
    )

    private fun title(name: String) = Screen.DetailsScreen.Details(
        title = name,
        description = "d",
        url = "url-$name",
        imageUrl = "img-$name",
        source = "src",
    )

    private val TabsController.main get() = windows.first { it.isMain }
    private val BrowserWindowState.active get() = tabs.first { it.id == activeId.value }
    private fun BrowserWindowState.stacks(): List<List<NavKey>> = tabs.map { it.nav.backStack.toList() }

    private fun unknownKey(): JsonElement = buildJsonObject { put("type", JsonPrimitive("com.example.GoneRoute")) }

    private fun encoded(key: NavKey): JsonElement = assertNotNull(store.encode(key))

    /** A session with Home plus one title tab in the main window, and a torn-out window with one tab. */
    private fun sampleSession() = TabSession(
        windows = listOf(
            WindowSession(
                bounds = WindowBounds(10.dp, 20.dp, 900.dp, 700.dp),
                isMain = true,
                activeIndex = 1,
                tabs = listOf(
                    TabSessionEntry(pinned = true, backStack = listOf(encoded(Screen.RecentScreen))),
                    TabSessionEntry(pinned = false, backStack = listOf(encoded(title("A")))),
                ),
            ),
            WindowSession(
                bounds = WindowBounds(300.dp, 200.dp, 640.dp, 480.dp),
                isMain = false,
                activeIndex = 0,
                tabs = listOf(TabSessionEntry(pinned = false, backStack = listOf(encoded(title("B"))))),
            ),
        )
    )

    @Test
    fun roundTripRestoresWindowsTabsStacksAndActive() {
        val before = newController()
        val homeId = before.main.tabs.first().id
        // Home: a title opened in place on top of Recent.
        before.main.tabs.first().nav.navigate(title("H"))
        val a = before.open(title("A"), TabOpenMode.Foreground, homeId)
        before.main.tabs.first { it.id == a }.nav.navBackStack.add(Screen.WebViewScreen("https://a"))
        val b = before.open(title("B"), TabOpenMode.Background, homeId)
        val c = before.open(title("C"), TabOpenMode.NewWindow, homeId)
        val torn = before.windowOf(c)
        torn.tabs.first().nav.navBackStack.add(Screen.WebViewScreen("https://c"))
        torn.bounds.value = WindowBounds(300.dp, 200.dp, 640.dp, 480.dp)
        before.main.bounds.value = WindowBounds(10.dp, 20.dp, 900.dp, 700.dp)
        before.activate(b)

        assertTrue(store.save(before))
        val session = assertNotNull(store.load())

        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = session, decode = store::decode, onScreen = { true })

        assertEquals(2, after.windows.size)
        assertEquals(before.main.stacks(), after.main.stacks())
        assertEquals(listOf(true, false, false), after.main.tabs.map { it.pinned })
        assertEquals(title("B"), after.main.active.current)
        assertEquals(WindowBounds(10.dp, 20.dp, 900.dp, 700.dp), after.main.bounds.value)

        val restoredTorn = after.windows.first { !it.isMain }
        assertEquals(torn.stacks(), restoredTorn.stacks())
        assertEquals(WindowBounds(300.dp, 200.dp, 640.dp, 480.dp), restoredTorn.bounds.value)
        assertEquals(after.main.id, after.focusedWindow)
        // Restored tabs still route through the controller.
        assertEquals(after, after.main.active.nav.interceptor)
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun restoreDropsKeysWithNoPage() {
        // Only Recent, Details and Favorites have pages; Settings' top-level key has none in a tab.
        val routes = NavKeyRoutes(
            listOf(
                { entry<Screen.RecentScreen> {} },
                { entry<Screen.DetailsScreen.Details> {} },
                { entry<Screen.FavoriteScreen> {} },
            )
        )
        val routed = TabSessionStore(file, navKeyJson(listOf(CoreNavKeySerializers)), routes::routable)
        assertTrue(routes.routable(title("A")))
        assertFalse(routes.routable(Screen.SettingsScreen))

        val session = TabSession(
            windows = listOf(
                WindowSession(
                    bounds = null,
                    isMain = true,
                    activeIndex = 1,
                    tabs = listOf(
                        TabSessionEntry(pinned = true, backStack = listOf(encoded(Screen.RecentScreen))),
                        TabSessionEntry(pinned = false, backStack = listOf(encoded(title("A")), encoded(Screen.SettingsScreen))),
                        TabSessionEntry(pinned = false, backStack = listOf(encoded(Screen.SettingsScreen))),
                    ),
                ),
                WindowSession(
                    bounds = null,
                    isMain = false,
                    activeIndex = 0,
                    tabs = listOf(TabSessionEntry(pinned = false, backStack = listOf(encoded(Screen.SettingsScreen)))),
                ),
            )
        )
        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = session, decode = routed::decode)

        // The stack stops before the page-less key, a tab left empty is dropped, and so is a window left empty.
        assertEquals(1, after.windows.size)
        assertEquals(listOf(listOf<NavKey>(Screen.RecentScreen), listOf<NavKey>(title("A"))), after.main.stacks())
    }

    @Test
    fun restoredTornWindowOffScreenGetsDefaultBounds() {
        val offScreen = WindowBounds(300.dp, 200.dp, 640.dp, 480.dp)
        val session = sampleSession().let { s ->
            s.copy(windows = s.windows.map { if (it.isMain) it else it.copy(bounds = offScreen) })
        }
        val after = newController()
        after.startSession(
            OnLaunch.Restore,
            onboardingDone = true,
            session = session,
            decode = store::decode,
            onScreen = { it != offScreen },
        )

        assertEquals(TabsController.DefaultWindowBounds, after.windows.first { !it.isMain }.bounds.value)
        // The main window's bounds are checked when its window opens (rememberMainWindowState).
        assertEquals(WindowBounds(10.dp, 20.dp, 900.dp, 700.dp), after.main.bounds.value)
    }

    @Test
    fun homeRestoresOnlyItsCurrentTopLevelStack() {
        val before = newController()
        val home = before.main.tabs.first().nav
        home.navigate(title("H"))
        home.settings()
        home.navigate(Screen.GeneralSettings)

        store.save(before)
        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = store.load(), decode = store::decode)

        // Home's history has no top-level stacks any more: Settings is pushed, so the whole stack is saved.
        assertEquals(
            listOf<NavKey>(Screen.RecentScreen, title("H"), Screen.Settings, Screen.GeneralSettings),
            after.main.tabs.first().nav.backStack.toList(),
        )
    }

    @Test
    fun restoreSkipsUnknownKeys() {
        val session = TabSession(
            windows = listOf(
                WindowSession(
                    bounds = null,
                    isMain = true,
                    activeIndex = 1,
                    tabs = listOf(
                        TabSessionEntry(pinned = true, backStack = listOf(encoded(Screen.RecentScreen))),
                        // Its only page is gone: the tab is dropped, and it was the active one.
                        TabSessionEntry(pinned = false, backStack = listOf(unknownKey())),
                        // Keeps the decodable prefix.
                        TabSessionEntry(
                            pinned = false,
                            backStack = listOf(encoded(title("A")), unknownKey(), encoded(Screen.WebViewScreen("x"))),
                        ),
                        // Garbage that is not even an object.
                        TabSessionEntry(pinned = false, backStack = listOf(JsonPrimitive(42))),
                    ),
                ),
                WindowSession(
                    bounds = null,
                    isMain = false,
                    activeIndex = 0,
                    tabs = listOf(TabSessionEntry(pinned = false, backStack = listOf(unknownKey()))),
                ),
            )
        )

        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = session, decode = store::decode)

        assertEquals(1, after.windows.size)
        assertEquals(
            listOf(listOf<NavKey>(Screen.RecentScreen), listOf<NavKey>(title("A"))),
            after.main.stacks(),
        )
        assertTrue(after.main.active.pinned)
    }

    @Test
    fun restoredPagesAreTakenOnceAndOpenedPagesNever() {
        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = sampleSession(), decode = store::decode)
        val restoredA = after.main.tabs.first { it.current == title("A") }
        val torn = after.windows.first { !it.isMain }.tabs.first()
        val home = after.main.tabs.first { it.pinned }

        // A restored player opens paused, once; showing it again (a reopened player) plays as usual.
        assertTrue(after.takeRestored(restoredA.id, title("A")))
        assertFalse(after.takeRestored(restoredA.id, title("A")))
        assertTrue(after.takeRestored(torn.id, title("B")))
        assertTrue(after.takeRestored(home.id, Screen.RecentScreen))
        // Not the page it restored, or a tab opened after launch.
        assertFalse(after.takeRestored(restoredA.id, title("B")))
        val opened = after.open(title("C"), TabOpenMode.Foreground, home.id)
        assertFalse(after.takeRestored(opened, title("C")))
    }

    @Test
    fun sessionFileCarriesItsVersion() {
        assertTrue(store.write(sampleSession()))
        assertTrue("\"version\":1" in file.readText().replace(" ", ""))
        assertEquals(1, assertNotNull(store.load()).version)
    }

    @Test
    fun sessionWithoutVersionLoadsAsVersionOne() {
        assertTrue(store.write(sampleSession()))
        val old = file.readText().replace(",\"version\":1", "").replace("\"version\":1,", "")
        assertFalse("version" in old)
        file.writeText(old)

        val loaded = assertNotNull(store.load())
        assertEquals(1, loaded.version)
        assertEquals(2, loaded.windows.size)
    }

    @Test
    fun sessionFromANewerVersionStartsFresh() {
        assertTrue(store.write(sampleSession().copy(version = TabSession.CURRENT_VERSION + 1)))

        val session = store.load()
        assertNull(session)
        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = session, decode = store::decode)
        assertEquals(listOf(listOf<NavKey>(Screen.RecentScreen)), after.main.stacks())
        assertEquals(1, after.windows.size)
    }

    @Test
    fun restoreCorruptFileStartsFresh() {
        assertNull(store.load())

        file.writeText("")
        assertNull(store.load())

        file.writeText("{\"windows\": [ {\"isMain\": tr")
        assertNull(store.load())

        file.writeText("[1, 2, 3]")
        val session = store.load()
        assertNull(session)

        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = session, decode = store::decode)
        assertEquals(listOf(listOf<NavKey>(Screen.RecentScreen)), after.main.stacks())
        assertEquals(1, after.windows.size)
    }

    @Test
    fun onLaunchHomeStartsWithHomeOnly() {
        val after = newController()
        after.startSession(OnLaunch.Home, onboardingDone = true, session = sampleSession(), decode = store::decode)

        assertEquals(1, after.windows.size)
        assertEquals(listOf(listOf<NavKey>(Screen.RecentScreen)), after.main.stacks())
        assertTrue(after.main.active.pinned)
    }

    @Test
    fun onLaunchNewTabStartsWithNewTabPage() {
        val after = newController()
        after.startSession(OnLaunch.NewTab, onboardingDone = true, session = sampleSession(), decode = store::decode)

        assertEquals(1, after.windows.size)
        assertEquals(
            listOf(listOf<NavKey>(Screen.RecentScreen), listOf<NavKey>(NewTabRoute)),
            after.main.stacks(),
        )
        assertEquals(NewTabRoute, after.main.active.current)
        assertTrue(after.main.active.isUntouchedNewTab)
    }

    @Test
    fun onboardingPendingRestoresHomeOnly() {
        for (onLaunch in OnLaunch.entries) {
            val after = newController()
            after.startSession(onLaunch, onboardingDone = false, session = sampleSession(), decode = store::decode)

            assertEquals(1, after.windows.size, "$onLaunch")
            assertEquals(listOf(listOf<NavKey>(Screen.RecentScreen)), after.main.stacks(), "$onLaunch")
            assertTrue(after.main.active.pinned, "$onLaunch")
        }
    }

    @Test
    fun missingBoundsUsesDefaultWindowSize() {
        val session = sampleSession().let { s ->
            s.copy(windows = s.windows.map { it.copy(bounds = null) })
        }
        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = session, decode = store::decode)

        assertNull(after.main.bounds.value)
        assertEquals(DefaultMainWindowSize, initialWindowSize(after.main.bounds.value))
        assertEquals(DpSize(900.dp, 700.dp), initialWindowSize(WindowBounds(10.dp, 20.dp, 900.dp, 700.dp)))
        assertEquals(TabsController.DefaultWindowBounds, after.windows.first { !it.isMain }.bounds.value)
    }

    @Test
    fun queuedWritesLeaveTheNewestSessionLast() = runBlocking {
        val older = sampleSession()
        val newer = older.copy(windows = older.windows.take(1))
        val writes = PendingWrites(CoroutineScope(Dispatchers.IO))
        // An autosave in flight, then the quit save, on the same queue.
        writes.launch { store.write(older) }
        writes.launch { store.write(newer) }
        writes.awaitAll()
        assertEquals(newer, store.load())
    }

    @Test
    fun concurrentWritesNeverLeaveAHalfWrittenSession() {
        val sessions = List(8) { i ->
            sampleSession().let { s -> s.copy(windows = s.windows.map { it.copy(activeIndex = i) }) }
        }
        val failed = java.util.concurrent.atomic.AtomicInteger()
        val threads = sessions.map { session ->
            Thread { repeat(25) { if (!store.write(session)) failed.incrementAndGet() } }
        }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        // Sharing the temp file, one write's rename takes another's file (or a half-written one) away.
        assertEquals(0, failed.get())
        assertTrue(store.load() in sessions)
        assertFalse(File(dir, "tabs-session.json.tmp").exists())
    }

    @Test
    fun streamedReadersAreCutFromTheSavedStack() {
        val readerStore = TabSessionStore(
            file,
            navKeyJson(listOf(CoreNavKeySerializers, FakeReaderSerializers)),
            listOf(CoreNavKeySerializers, FakeReaderSerializers).restorable(),
        )
        val before = newController()
        val homeId = before.main.tabs.first().id
        // Details, then a streamed reader opened from it: comes back as Details.
        val fromDetails = before.open(title("A"), TabOpenMode.Foreground, homeId)
        before.main.tabs.first { it.id == fromDetails }.nav.navBackStack.add(FakeReader(downloaded = false))
        // Only a streamed reader: dropped.
        before.open(FakeReader(downloaded = false, url = "only"), TabOpenMode.Background, homeId)
        // A downloaded reader reads its file: kept.
        before.open(FakeReader(downloaded = true, url = "file"), TabOpenMode.Background, homeId)

        assertTrue(readerStore.save(before))
        val after = newController()
        after.startSession(OnLaunch.Restore, onboardingDone = true, session = readerStore.load(), decode = readerStore::decode)

        assertEquals(
            setOf(
                listOf<NavKey>(Screen.RecentScreen),
                listOf<NavKey>(title("A")),
                listOf<NavKey>(FakeReader(downloaded = true, url = "file")),
            ),
            after.main.stacks().toSet(),
        )
        assertEquals(3, after.main.tabs.size)

        // A file saved before the key was marked unrestorable still can't bring it back.
        val permissive = TabSessionStore(file, navKeyJson(listOf(CoreNavKeySerializers, FakeReaderSerializers)))
        val streamed = assertNotNull(permissive.encode(FakeReader(downloaded = false)))
        assertNull(readerStore.decode(streamed))
        assertEquals(FakeReader(downloaded = true), readerStore.decode(assertNotNull(permissive.encode(FakeReader(downloaded = true)))))
    }

    @Test
    fun saveIsAtomic() {
        val good = sampleSession()
        assertTrue(store.write(good))

        // A crash mid-write left a partial temp file behind: the good session still loads, and the next save works.
        val temp = File(dir, "tabs-session.json.tmp")
        temp.writeText("{\"windows\": [")
        assertEquals(good, store.load())

        // A write that fails part way (here the temp path cannot be written) never touches the good session.
        temp.delete()
        temp.mkdirs()
        File(temp, "child").writeText("x")
        assertFalse(store.write(TabSession(emptyList())))
        assertEquals(good, store.load())

        temp.deleteRecursively()
        val next = good.copy(windows = good.windows.take(1))
        assertTrue(store.write(next))
        assertEquals(next, store.load())
        assertFalse(temp.exists())
    }
}
