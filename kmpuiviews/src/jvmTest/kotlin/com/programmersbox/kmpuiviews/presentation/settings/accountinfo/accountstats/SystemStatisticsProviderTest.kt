package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstats

import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BlurHashItem
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.domain.TranslationModel
import com.programmersbox.kmpuiviews.testing.FakeExceptionDao
import com.programmersbox.kmpuiviews.testing.FakeTranslationModelHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemStatisticsProviderTest {

    private class FakeBlurHashDao : BlurHashDao {
        override fun getAllHashes(): Flow<List<BlurHashItem>> = flowOf(emptyList())
        override fun getAllHashesCount(): Flow<Int> = flowOf(0)
        override fun getHash(url: String?): Flow<BlurHashItem?> = flowOf(null)
        override suspend fun insertHash(model: BlurHashItem) {}
        override suspend fun deleteHash(model: BlurHashItem) {}
    }

    private fun row(code: String, state: ModelState) = TranslationModel(
        language = TranslationLanguages.byCode(code)!!,
        state = state,
        sizeBytes = null,
        sizeIsEstimate = true,
        required = code == "en",
        lastUsedMillis = null,
    )

    private fun provider(handler: FakeTranslationModelHandler) = SystemStatisticsProvider(
        exceptionDao = FakeExceptionDao(),
        blurHashDao = FakeBlurHashDao(),
        translationModelHandler = handler,
        appConfig = AppConfig(appName = "Test", buildType = BuildType.Full, isDebug = true),
    )

    @Test
    fun translationStatRefreshesHandlerBeforeCounting() = runBlocking {
        // A fresh handler knows nothing until refresh() reads what is on disk.
        val handler = FakeTranslationModelHandler(
            initial = listOf(row("en", ModelState.NotDownloaded), row("ja", ModelState.NotDownloaded)),
        )
        handler.onRefresh = listOf(row("en", ModelState.Downloaded), row("ja", ModelState.Downloaded))

        val stats = withTimeout(5_000) {
            provider(handler).observeStats()
                .first { list -> list.single { it.id == "translationModels" }.value == "2" }
        }

        assertEquals(1, handler.refreshCount)
        assertEquals("2", stats.single { it.id == "translationModels" }.value)
    }

    @Test
    fun translationStatSurvivesRefreshFailure() = runBlocking {
        val handler = FakeTranslationModelHandler(
            initial = listOf(row("en", ModelState.Downloaded)),
            refreshError = IllegalStateException("boom"),
        )

        val stats = withTimeout(5_000) {
            provider(handler).observeStats()
                .first { list -> list.single { it.id == "translationModels" }.value == "1" }
        }

        assertEquals("1", stats.single { it.id == "translationModels" }.value)
    }
}
