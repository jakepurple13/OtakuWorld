package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.domain.TranslationModel
import com.programmersbox.kmpuiviews.testing.FakeTranslationModelHandler
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.sharedcomponents.notifier.DefaultInAppNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationViewModelTest {

    private val viewModelStore = ViewModelStore()
    private val settings = createTestNewSettingsHandling()

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun row(code: String, downloaded: Boolean, size: Long = 1_000L) = TranslationModel(
        language = TranslationLanguages.byCode(code)!!,
        state = if (downloaded) ModelState.Downloaded else ModelState.NotDownloaded,
        sizeBytes = if (downloaded) size else null,
        sizeIsEstimate = !downloaded,
        required = code == "en",
        lastUsedMillis = null,
    )

    private fun seeded() = FakeTranslationModelHandler(
        initial = listOf(
            row("en", true, 100),
            row("ja", true, 200),
            row("ko", true, 300),
            row("zh", true, 400),
            row("es", true, 500),
            row("fr", false),
            row("de", false),
        ),
    )

    private val notifier = DefaultInAppNotifier(CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private fun viewModel(handler: FakeTranslationModelHandler) =
        TranslationViewModel(handler, settings, notifier)
            .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    @Test fun `delete of target resets target to en`() = runTest {
        val handler = seeded()
        val vm = viewModel(handler)
        settings.translationTargetLanguage.set("ja")
        awaitCondition { vm.state.value.target.code == "ja" }

        vm.askDelete("ja")
        awaitCondition { vm.state.value.confirmingCode == "ja" }
        vm.confirmDelete()
        awaitCondition { vm.state.value.target.code == "en" }

        assertEquals(listOf(setOf("ja")), handler.deleted)
    }

    @Test fun `bulk delete of target resets target to en`() = runTest {
        val handler = seeded()
        val vm = viewModel(handler)
        settings.translationTargetLanguage.set("ja")
        awaitCondition { vm.state.value.target.code == "ja" }

        vm.enterSelect("ja")
        vm.toggle("ko")
        awaitCondition { vm.state.value.selected == setOf("ja", "ko") }
        vm.askBulkDelete()
        awaitCondition { vm.state.value.confirmingBulk }
        vm.confirmBulkDelete()
        awaitCondition { vm.state.value.target.code == "en" }

        assertEquals(listOf(setOf("ja", "ko")), handler.deleted)
    }

    @Test fun `english is never selectable`() = runTest {
        val vm = viewModel(seeded())

        vm.enterSelect()
        vm.toggle("en")
        vm.selectAll()
        awaitCondition { vm.state.value.selected.isNotEmpty() }

        assertFalse("en" in vm.state.value.selected)
        assertEquals(setOf("ja", "ko", "zh", "es"), vm.state.value.selected)
    }

    @Test fun `selectAll twice clears`() = runTest {
        val vm = viewModel(seeded())

        vm.enterSelect()
        vm.selectAll()
        awaitCondition { vm.state.value.selected.size == 4 }
        vm.selectAll()
        awaitCondition { vm.state.value.selected.isEmpty() }

        assertTrue(vm.state.value.selected.isEmpty())
    }

    @Test fun `selectedBytes sums selected sizes`() = runTest {
        val vm = viewModel(seeded())

        vm.enterSelect("ja")
        vm.toggle("ko")
        awaitCondition { vm.state.value.selectedBytes == 500L }

        assertEquals(500L, vm.state.value.selectedBytes)
    }

    @Test fun `totalBytes sums downloaded sizes`() = runTest {
        val vm = viewModel(seeded())

        awaitCondition { vm.state.value.downloaded.size == 5 }

        assertEquals(1_500L, vm.state.value.totalBytes)
        assertEquals("en", vm.state.value.downloaded.first().language.code)
    }

    @Test fun `onBack exits select before closing`() = runTest {
        val vm = viewModel(seeded())

        vm.enterSelect("ja")
        awaitCondition { vm.state.value.selecting }

        assertTrue(vm.onBack())
        awaitCondition { !vm.state.value.selecting }
        assertFalse(vm.onBack())
    }

    @Test fun `onBack closes picker first`() = runTest {
        val vm = viewModel(seeded())

        vm.openPicker()
        awaitCondition { vm.state.value.pickerOpen }

        assertTrue(vm.onBack())
        awaitCondition { !vm.state.value.pickerOpen }
        assertFalse(vm.onBack())
    }

    @Test fun `setTarget not downloaded triggers download`() = runTest {
        val handler = seeded()
        val vm = viewModel(handler)

        vm.openPicker()
        awaitCondition { vm.state.value.pickerOpen }
        vm.setTarget("fr")
        awaitCondition { vm.state.value.target.code == "fr" }

        assertEquals(listOf("fr"), handler.downloads)
        assertFalse(vm.state.value.pickerOpen)
    }

    @Test fun `query filters notDownloaded and picker by english or native name`() = runTest {
        val vm = viewModel(seeded())

        vm.setQuery("deut")
        awaitCondition { vm.state.value.query == "deut" }

        assertEquals(listOf("de"), vm.state.value.notDownloaded.map { it.language.code })
        assertEquals(listOf("de"), vm.state.value.pickerLanguages.map { it.language.code })

        vm.setQuery("french")
        awaitCondition { vm.state.value.query == "french" }
        assertEquals(listOf("fr"), vm.state.value.notDownloaded.map { it.language.code })
    }

    @Test fun `unavailable handler gives available false`() = runTest {
        val vm = viewModel(FakeTranslationModelHandler(isAvailable = false))

        assertFalse(vm.state.value.available)
    }

    @Test fun `english cannot be deleted`() = runTest {
        val handler = seeded()
        val vm = viewModel(handler)

        vm.askDelete("en")
        vm.confirmDelete()
        delay(200)

        assertTrue(handler.deleted.isEmpty())
        assertEquals(null, vm.state.value.confirmingCode)
    }

    @Test fun `delete failure shows error and keeps target`() = runTest {
        val handler = FakeTranslationModelHandler(
            initial = seeded().models.value,
            deleteError = IllegalStateException("boom"),
        )
        val vm = viewModel(handler)
        settings.translationTargetLanguage.set("ja")
        awaitCondition { vm.state.value.target.code == "ja" }

        vm.askDelete("ja")
        awaitCondition { vm.state.value.confirmingCode == "ja" }
        vm.confirmDelete()
        awaitCondition { notifier.banners.value.any { it.title == "Couldn't delete" && it.subtitle == "boom" } }

        assertEquals("ja", vm.state.value.target.code)
    }

    @Test fun `refresh failure shows error`() = runTest {
        viewModel(FakeTranslationModelHandler(refreshError = IllegalStateException("nope")))

        awaitCondition {
            notifier.banners.value.any { it.title == "Couldn't load translation models" && it.subtitle == "nope" }
        }
    }

    @Test fun `refresh reloads models`() = runTest {
        val handler = seeded()
        val vm = viewModel(handler)
        awaitCondition { handler.refreshCount == 1 && vm.state.value.downloaded.size == 5 }

        // A model downloaded elsewhere (e.g. while translating) shows up on the next refresh.
        handler.onRefresh = handler.models.value.map {
            if (it.language.code == "fr") it.copy(state = ModelState.Downloaded, sizeBytes = 600) else it
        }
        vm.refresh()
        awaitCondition { handler.refreshCount == 2 }
        awaitCondition { vm.state.value.downloaded.any { it.language.code == "fr" } }
        assertTrue(vm.state.value.notDownloaded.none { it.language.code == "fr" })
    }

    @Test fun `openPicker exits select and clears confirms`() = runTest {
        val vm = viewModel(seeded())
        awaitCondition { vm.state.value.downloaded.size == 5 }

        vm.askDelete("ja")
        vm.enterSelect("ko")
        vm.toggle("zh")
        vm.askBulkDelete()
        awaitCondition { vm.state.value.confirmingBulk && vm.state.value.confirmingCode == "ja" }

        vm.openPicker()
        awaitCondition { vm.state.value.pickerOpen }
        val s = vm.state.value
        assertFalse(s.selecting)
        assertTrue(s.selected.isEmpty())
        assertFalse(s.confirmingBulk)
        assertEquals(null, s.confirmingCode)
    }

    @Test fun `onBack closes picker in one press after select mode`() = runTest {
        val vm = viewModel(seeded())
        awaitCondition { vm.state.value.downloaded.size == 5 }

        vm.enterSelect("ja")
        vm.openPicker()
        awaitCondition { vm.state.value.pickerOpen }

        assertTrue(vm.onBack())
        awaitCondition { !vm.state.value.pickerOpen }
        assertFalse(vm.state.value.selecting)
        assertFalse(vm.onBack())
    }
}
