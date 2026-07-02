package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.domain.KmpCustomRemoteModel
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
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

class TranslationViewModelTest {

    private val viewModelStore = ViewModelStore()

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun model(language: String) = KmpCustomRemoteModel(hash = "hash-$language", language = language)

    private class FakeHandler(
        initialModels: List<KmpCustomRemoteModel> = emptyList(),
    ) : TranslationModelHandler {
        val models = initialModels.toMutableList()

        override fun getModels(onSuccess: (List<KmpCustomRemoteModel>) -> Unit) {
            onSuccess(models)
        }

        override suspend fun deleteModel(model: KmpCustomRemoteModel) {
            models.remove(model)
        }

        override suspend fun delete(model: KmpCustomRemoteModel) {
            models.remove(model)
        }

        override suspend fun modelList(): List<KmpCustomRemoteModel> = models
    }

    private fun viewModel(handler: TranslationModelHandler) = TranslationViewModel(
        translationModelHandler = handler,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

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

    @Test fun `starts with empty translation models`() = runTest {
        val vm = viewModel(FakeHandler())

        assertTrue(vm.translationModels.isEmpty())
    }

    @Test fun `loadModels populates translationModels from handler`() = runTest {
        val handler = FakeHandler(listOf(model("en"), model("jp")))
        val vm = viewModel(handler)

        vm.loadModels()
        awaitCondition { vm.translationModels.isNotEmpty() }

        assertEquals(2, vm.translationModels.size)
        assertEquals(listOf("en", "jp"), vm.translationModels.map { it.language })
    }

    @Test fun `deleteModel removes model and refreshes translationModels`() = runTest {
        val enModel = model("en")
        val handler = FakeHandler(listOf(enModel, model("jp")))
        val vm = viewModel(handler)

        vm.loadModels()
        awaitCondition { vm.translationModels.size == 2 }

        vm.deleteModel(enModel)
        awaitCondition { vm.translationModels.size == 1 }

        assertEquals("jp", vm.translationModels[0].language)
    }
}
