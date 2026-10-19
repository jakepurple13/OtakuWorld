package com.programmersbox.anime.shared.subtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SubtitleSettingsViewModelTest {

    /** Stores whatever it is given, so coercion must come from the view model. */
    private class FakeRepository : SubtitleSettingsRepository {
        val stored = MutableStateFlow(SubtitleStyle())
        override val style = stored
        override suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle) {
            stored.value = transform(stored.value)
        }

        override suspend fun reset() {
            stored.value = SubtitleStyle()
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateCoercesAndPersists() {
        val repo = FakeRepository()
        val viewModel = SubtitleSettingsViewModel(repo)

        viewModel.update { it.copy(sizePercent = 37) }

        assertEquals(50, repo.stored.value.sizePercent)
        assertEquals(50, viewModel.style.value.sizePercent)
    }

    @Test
    fun updateChangesOnlyTheTransformedField() {
        val repo = FakeRepository()
        val viewModel = SubtitleSettingsViewModel(repo)
        // A change the caller has not seen yet, e.g. from another control or screen.
        repo.stored.value = repo.stored.value.copy(edge = SubtitleEdge.Shadow)

        viewModel.update { it.copy(sizePercent = 150) }

        assertEquals(150, repo.stored.value.sizePercent)
        assertEquals(SubtitleEdge.Shadow, repo.stored.value.edge)
    }

    @Test
    fun resetRestoresDefaults() {
        val repo = FakeRepository()
        repo.stored.value = SubtitleStyle(sizePercent = 150, edge = SubtitleEdge.Shadow, autoEnable = false)
        val viewModel = SubtitleSettingsViewModel(repo)

        viewModel.reset()

        assertEquals(SubtitleStyle(), repo.stored.value)
        assertEquals(SubtitleStyle(), viewModel.style.value)
    }
}
