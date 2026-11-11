package com.programmersbox.kmpuiviews.presentation.dailyoverview

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewSettings
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import com.programmersbox.kmpuiviews.presentation.dailyoverview.settings.DailyOverviewSettingsViewModel
import com.programmersbox.kmpuiviews.presentation.dailyoverview.settings.SampleFacts
import com.programmersbox.kmpuiviews.testing.FakeKmpGenericInfo
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class DailyOverviewSettingsViewModelTest {

    /** [label] mirrors the real engines' card label; [shortName] is what settings rows show. */
    private class FakeEngine(
        override val id: SummaryEngineId,
        private val shortName: String,
        @Volatile var status: EngineStatus = EngineStatus.Ready,
        private val throws: Boolean = false,
    ) : SummaryEngine {
        @Volatile
        var received: DailyFacts? = null
        override suspend fun label() = "$shortName · on device"
        override suspend fun shortName() = shortName
        override val onDevice = true
        override suspend fun status(): EngineStatus = if (throws) error("model dir missing") else status
        override fun summarize(facts: DailyFacts): Flow<String> {
            received = facts
            return flowOf("Sample ", "text")
        }
    }

    private val viewModelStore = ViewModelStore()
    private lateinit var settings: DailyOverviewSettings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        settings = DailyOverviewSettings(createTestDataStoreHandling())
    }

    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    private suspend fun await(condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) { while (!condition()) delay(10) }
        }
    }

    private fun viewModel(vararg engines: SummaryEngine): DailyOverviewSettingsViewModel {
        val resolver = EngineResolver(
            listOf(TemplateEngine()) + engines,
            listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma),
        )
        return DailyOverviewSettingsViewModel(settings, resolver, FakeKmpGenericInfo())
            .also { viewModelStore.put("vm", it) }
    }

    @Test
    fun `resolvedLabel reads Automatic · Gemma 4 E2B when Gemma ready`() = runTest {
        val vm = viewModel(
            FakeEngine(SummaryEngineId.Nano, "Gemini Nano", EngineStatus.Unavailable),
            FakeEngine(SummaryEngineId.Gemma, "Gemma 4 E2B"),
        )
        await { vm.ui.value?.resolvedLabel == "Automatic · Gemma 4 E2B" }
        assertEquals(
            listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma, SummaryEngineId.Template),
            vm.ui.value?.engines?.map { it.id },
        )
    }

    @Test
    fun `resolvedLabel appends (not ready) when explicit choice is not Ready`() = runTest {
        settings.setEngine(SummaryEngineChoice.Gemma)
        val vm = viewModel(
            FakeEngine(SummaryEngineId.Gemma, "Gemma", EngineStatus.NeedsSetup("Download a model")),
        )
        await { vm.ui.value?.resolvedLabel == "Gemma on this phone (not ready)" }
    }

    @Test
    fun `runTest streams from the chosen engine using sample facts`() = runTest {
        val gemma = FakeEngine(SummaryEngineId.Gemma, "Gemma 4 E2B")
        val vm = viewModel(gemma)
        vm.choose(SummaryEngineChoice.Gemma)
        await { vm.ui.value?.prefs?.engine == SummaryEngineChoice.Gemma }
        vm.runTest()
        await { vm.test.value is SummaryState.Done }
        val done = vm.test.value as SummaryState.Done
        assertEquals("Sample text", done.text)
        assertEquals(SummaryEngineId.Gemma, done.engine)
        assertEquals(SampleFacts.create(FakeKmpGenericInfo().updateUnit), gemma.received)
    }

    @Test
    fun `refresh updates resolvedLabel after an engine becomes ready`() = runTest {
        val gemma = FakeEngine(SummaryEngineId.Gemma, "Gemma 4 E2B", EngineStatus.NeedsSetup("Download a model"))
        val vm = viewModel(gemma)
        await { vm.ui.value?.resolvedLabel == "Automatic · Numbers only (not ready)" }
        gemma.status = EngineStatus.Ready
        vm.refresh()
        await { vm.ui.value?.resolvedLabel == "Automatic · Gemma 4 E2B" }
    }

    @Test
    fun `an engine whose status throws reads as needing setup and does not break the page`() = runTest {
        val vm = viewModel(FakeEngine(SummaryEngineId.Gemma, "Gemma 4 E2B", throws = true))
        await { vm.ui.value?.engines?.isNotEmpty() == true }
        val gemma = vm.ui.value!!.engines.first { it.id == SummaryEngineId.Gemma }
        assertTrue(gemma.status is EngineStatus.NeedsSetup)
        assertEquals("Automatic · Numbers only (not ready)", vm.ui.value?.resolvedLabel)

        vm.choose(SummaryEngineChoice.Gemma)
        await { vm.ui.value?.prefs?.engine == SummaryEngineChoice.Gemma }
        vm.runTest()
        await { vm.test.value is SummaryState.Done }
        assertEquals(SummaryEngineId.Template, (vm.test.value as SummaryState.Done).engine)
    }

    @Test
    fun `setTime stores minutes of day`() = runTest {
        val vm = viewModel()
        vm.setTime(615)
        await { vm.ui.value?.prefs?.minuteOfDay == 615 }
        assertEquals(615, settings.prefs.first().minuteOfDay)
    }
}
