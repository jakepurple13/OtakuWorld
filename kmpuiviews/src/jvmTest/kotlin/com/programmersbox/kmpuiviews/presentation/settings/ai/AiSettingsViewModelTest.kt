package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.domain.aitext.AiEnginePreference
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.domain.aitext.DefaultAiText
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportState
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportedModel
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ModelImports
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import com.programmersbox.kmpuiviews.testing.InMemoryAiSettings
import com.programmersbox.kmpuiviews.testing.clearAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiSettingsViewModelTest {
    private val viewModelStore = ViewModelStore()

    private class Engine(
        override val id: SummaryEngineId,
        var status: EngineStatus = EngineStatus.Ready,
    ) : SummaryEngine {
        override val onDevice = true
        override suspend fun label() = "${id.name} · on device"
        override suspend fun shortName() = id.name
        override suspend fun status() = status
        override fun summarize(facts: DailyFacts): Flow<String> = flowOf()
        override fun generate(system: String, user: String): Flow<String> = flowOf("- sample")
    }

    private val order = listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma)

    private fun viewModel(
        settings: InMemoryAiSettings = InMemoryAiSettings(),
        engines: List<SummaryEngine> = listOf(Engine(SummaryEngineId.Nano), Engine(SummaryEngineId.Gemma)),
        order: List<SummaryEngineId> = this.order,
        imports: ModelImports? = null,
    ): AiSettingsViewModel {
        val all = listOf<SummaryEngine>(TemplateEngine()) + engines
        return AiSettingsViewModel(
            settings = settings,
            aiText = DefaultAiText(all, null, settings = settings, order = order),
            engines = all,
            order = order,
            imports = imports,
        ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }
    }

    private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T =
        withContext(Dispatchers.Default) { withTimeout(5_000) { first(predicate) } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        viewModelStore.clearAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `toggles write settings`() = runTest {
        val settings = InMemoryAiSettings()
        val vm = viewModel(settings)
        vm.setFeature(AiToggle.Tldr, false)
        vm.setEnabled(false)
        vm.setPreference(AiEnginePreference.Gemma)
        val ui = vm.ui.await { it != null && !it.enabled }!!
        assertEquals(setOf(AiToggle.Tldr), ui.disabled)
        assertEquals(AiEnginePreference.Gemma, settings.preference.value)
    }

    @Test
    fun `current follows preference`() = runTest {
        val vm = viewModel()
        assertEquals("Nano · on device", vm.ui.await { it?.current != null }!!.current)
        vm.setPreference(AiEnginePreference.Gemma)
        assertEquals("Gemma · on device", vm.ui.await { it?.current?.startsWith("Gemma") == true }!!.current)
    }

    @Test
    fun `current is null when nothing preferred is ready`() = runTest {
        val vm = viewModel(
            settings = InMemoryAiSettings(preference = AiEnginePreference.Nano),
            engines = listOf(Engine(SummaryEngineId.Nano, EngineStatus.Unavailable), Engine(SummaryEngineId.Gemma)),
        )
        val ui = vm.ui.await { it != null && it.engines.isNotEmpty() }!!
        assertNull(ui.current)
        assertEquals(listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma), ui.engines.map { it.id })
        assertEquals(EngineStatus.Unavailable, ui.engines.first().status)
    }

    @Test
    fun `show preference only with more than one engine`() = runTest {
        assertTrue(viewModel().ui.await { it != null }!!.showPreference)
        val desktop = viewModel(engines = listOf(Engine(SummaryEngineId.Ollama)), order = listOf(SummaryEngineId.Ollama))
        assertFalse(desktop.ui.await { it != null }!!.showPreference)
    }

    @Test
    fun `sample ignores feature toggles`() = runTest {
        val vm = viewModel(InMemoryAiSettings(disabled = AiToggle.entries.toSet()))
        vm.trySample()
        val done = vm.sample.await { it is AiTextState.Done || it is AiTextState.Error }
        assertEquals("- sample", (done as AiTextState.Done).text)
    }

    @Test
    fun `sample with ai off shows an error`() = runTest {
        val vm = viewModel(InMemoryAiSettings(enabled = false))
        vm.trySample()
        vm.sample.await { it is AiTextState.Error }
    }

    private class FakeImports(var model: ImportedModel? = null) : ModelImports {
        override val state = MutableStateFlow<ImportState>(ImportState.Idle)
        val imported = mutableListOf<Pair<String, String?>>()
        val removed = mutableListOf<String>()
        override fun import(file: PlatformFile, replacing: String?) {
            imported += file.file.name to replacing
        }
        override fun cancel() = Unit
        override fun dismissError() = Unit
        override fun remove(fileName: String) {
            removed += fileName
            model = null
        }
        override suspend fun current() = model
    }

    @Test
    fun `import state follows importer`() = runTest {
        val imports = FakeImports()
        val vm = viewModel(imports = imports)
        imports.state.value = ImportState.Checking("m.litertlm")
        assertEquals(ImportState.Checking("m.litertlm"), vm.importState.await { it is ImportState.Checking })
    }

    @Test
    fun `local model refreshes after an import finishes`() = runTest {
        val imports = FakeImports()
        val vm = viewModel(imports = imports)
        vm.ui.await { it != null }
        imports.state.value = ImportState.Copying("m.litertlm", 1, 2)
        imports.model = ImportedModel("m.litertlm", 2)
        imports.state.value = ImportState.Idle
        assertEquals(ImportedModel("m.litertlm", 2), vm.ui.await { it?.localModel != null }!!.localModel)
    }

    @Test
    fun `replace passes the current model and import does not`() = runTest {
        val imports = FakeImports(ImportedModel("old.litertlm", 1))
        val vm = viewModel(imports = imports)
        vm.ui.await { it?.localModel != null }
        vm.importModel(PlatformFile(java.io.File("new.litertlm")), replace = true)
        vm.importModel(PlatformFile(java.io.File("other.litertlm")), replace = false)
        assertEquals(listOf("new.litertlm" to "old.litertlm", "other.litertlm" to null), imports.imported)
    }

    @Test
    fun `remove deletes the current model and refreshes`() = runTest {
        val imports = FakeImports(ImportedModel("old.litertlm", 1))
        val vm = viewModel(imports = imports)
        vm.ui.await { it?.localModel != null }
        vm.removeModel()
        // Removal runs on IO; the refresh after it is what clears the row
        vm.ui.await { it != null && it.localModel == null }
        assertEquals(listOf("old.litertlm"), imports.removed)
    }

    @Test
    fun `preference hidden without Nano`() = runTest {
        val desktop = viewModel(
            engines = listOf(Engine(SummaryEngineId.Gemma), Engine(SummaryEngineId.Ollama)),
            order = listOf(SummaryEngineId.Gemma, SummaryEngineId.Ollama),
        )
        val ui = desktop.ui.await { it != null && it.engines.size == 2 }!!
        assertFalse(ui.showPreference)
    }

    @Test
    fun `summary counts on features`() = runTest {
        val ui = viewModel(InMemoryAiSettings(disabled = setOf(AiToggle.Tldr))).ui.await { it != null }!!
        assertEquals(AiToggle.entries.size - 1 to AiToggle.entries.size, ui.summary())
        assertNull(ui.copy(enabled = false).summary())
    }
}
