package com.programmersbox.kmpuiviews.presentation.debug.navmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class NavMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val json = """
        {"app":"mangaworld","nodes":[
          {"id":"@NavBar","label":"Nav bar","pseudo":true},
          {"id":"RecentScreen","label":"RecentScreen"},
          {"id":"DetailsScreen.Details","label":"DetailsScreen.Details"},
          {"id":"ReadViewModel.MangaReader","label":"ReadViewModel.MangaReader"},
          {"id":"Settings","label":"Settings"},
          {"id":"Settings.Appearance","label":"Settings.Appearance"},
          {"id":"ColorHelper","label":"ColorHelper","allowOrphan":true}],
         "edges":[
          {"from":"@NavBar","to":"RecentScreen","origin":"override"},
          {"from":"@NavBar","to":"Settings","origin":"override"},
          {"from":"RecentScreen","to":"DetailsScreen.Details","origin":"scan","sites":[{"file":"a/RecentView.kt","line":3}]},
          {"from":"DetailsScreen.Details","to":"ReadViewModel.MangaReader","origin":"override","note":"chapterOnClick"},
          {"from":"Settings","to":"Settings.Appearance","origin":"scan"}],
         "unresolved":[]}
    """.trimIndent()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsGraphAndLayout() = runTest(dispatcher) {
        val vm = NavMapViewModel { json }
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("mangaworld", state.graph?.app)
        assertNotNull(state.layout)
    }

    @Test
    fun missingGraphShowsEmptyState() = runTest(dispatcher) {
        val vm = NavMapViewModel { null }
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.graph)
    }

    @Test
    fun brokenJsonShowsEmptyState() = runTest(dispatcher) {
        val vm = NavMapViewModel { "{not json" }
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.graph)
    }

    @Test
    fun focusAndWalkKeepLastSix() = runTest(dispatcher) {
        val vm = NavMapViewModel { json }
        advanceUntilIdle()
        vm.focus("RecentScreen")
        assertEquals(NavMapMode.Focus, vm.state.value.mode)
        assertEquals(listOf("RecentScreen"), vm.state.value.trail)

        listOf("DetailsScreen.Details", "ReadViewModel.MangaReader", "Settings", "Settings.Appearance", "Settings", "ColorHelper", "RecentScreen")
            .forEach(vm::walk)
        assertEquals(6, vm.state.value.trail.size)
        assertEquals("RecentScreen", vm.state.value.trail.last())

        vm.jumpTrail(1)
        assertEquals(2, vm.state.value.trail.size)
    }

    @Test
    fun selectAndFilters() = runTest(dispatcher) {
        val vm = NavMapViewModel { json }
        advanceUntilIdle()
        vm.select("Settings")
        assertEquals("Settings", vm.state.value.selected)
        vm.select(null)
        assertNull(vm.state.value.selected)

        vm.toggleOrphansOnly()
        vm.toggleOverridesOnly()
        vm.toggleArgs()
        vm.toggleEntryPoints()
        with(vm.state.value) {
            assertEquals(true, orphansOnly)
            assertEquals(true, overridesOnly)
            assertEquals(false, showArgs)
            assertEquals(false, showEntryPoints)
        }
    }

    @Test
    fun searchFindsFirstMatch() = runTest(dispatcher) {
        val vm = NavMapViewModel { json }
        advanceUntilIdle()
        vm.setQuery("reader")
        assertEquals("ReadViewModel.MangaReader", vm.firstMatch())
        vm.setQuery("nav bar")
        assertEquals("@NavBar", vm.firstMatch())
        vm.setQuery("")
        assertNull(vm.firstMatch())
    }
}
