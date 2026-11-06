package com.programmersbox.uiviews.di

import android.content.Context
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.domain.TranslationModel
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.sharedutils.TranslateItems
import com.programmersbox.sharedutils.TranslatorUtils
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.dsl.module

val kmpInterop = module {
    single {
        AppConfig(
            appName = get<Context>().getString(R.string.app_name),
            isDebug = BuildConfig.DEBUG,
            buildType = when (BuildConfig.FLAVOR) {
                "noFirebase" -> BuildType.NoFirebase
                else -> BuildType.Full
            }
        )
    }

    factory<TranslationHandler> { TranslationItemHandler(get()) }
    factory<TranslationModelHandler> {
        MlKitTranslationModelHandler(
            context = get(),
            settings = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
}

private const val ESTIMATED_MODEL_BYTES = 30_000_000L

/** One row per catalog language. `en` is required; unknown sizes fall back to a 30 MB estimate. */
fun mlKitRows(
    downloaded: Set<String>,
    downloading: Map<String, ModelState>,
    sizes: (String) -> Long?,
    lastUsed: Map<String, Long>,
): List<TranslationModel> = TranslationLanguages.all.map { language ->
    val code = language.code
    val state = downloading[code]
        ?: if (code in downloaded) ModelState.Downloaded else ModelState.NotDownloaded
    val size = if (code in downloaded) sizes(code) else null
    TranslationModel(
        language = language,
        state = state,
        sizeBytes = size ?: ESTIMATED_MODEL_BYTES,
        sizeIsEstimate = size == null,
        required = code == "en",
        lastUsedMillis = lastUsed[code],
    )
}

class MlKitTranslationModelHandler(
    private val context: Context,
    private val settings: NewSettingsHandling,
    private val scope: CoroutineScope,
) : TranslationModelHandler {
    override val isAvailable: Boolean = TranslatorUtils.isAvailable

    private val downloading = MutableStateFlow<Map<String, ModelState>>(emptyMap())
    private val jobs = mutableMapOf<String, Job>()
    private val _models = MutableStateFlow(
        mlKitRows(emptySet(), emptyMap(), { null }, emptyMap())
    )
    override val models: StateFlow<List<TranslationModel>> = _models

    private var downloaded: Set<String> = emptySet()

    override suspend fun refresh() {
        if (!isAvailable) return
        downloaded = runCatching { TranslatorUtils.downloadedCodes() }.getOrDefault(downloaded)
        publish(settings.translationLastUsed.get())
    }

    private suspend fun publish(lastUsed: Map<String, Long>) {
        _models.value = mlKitRows(
            downloaded = downloaded,
            downloading = downloading.value,
            sizes = { TranslatorUtils.modelSizeBytes(context, it) },
            lastUsed = lastUsed,
        )
    }

    override fun download(code: String) {
        if (!isAvailable || jobs[code]?.isActive == true) return
        jobs[code] = scope.launch {
            setState(code, ModelState.Downloading(fraction = null))
            try {
                TranslatorUtils.download(code)
                setState(code, null)
                refresh()
            } catch (e: CancellationException) {
                setState(code, null)
                throw e
            } catch (e: Exception) {
                setState(code, ModelState.Failed(e.message ?: "Download failed"))
            }
        }
    }

    override fun cancelDownload(code: String) {
        jobs.remove(code)?.cancel()
        // ML Kit keeps downloading in the background; the next refresh reflects reality.
    }

    override suspend fun delete(codes: Set<String>) {
        codes.filter { it != "en" }.forEach { runCatching { TranslatorUtils.delete(it) } }
        refresh()
    }

    private suspend fun setState(code: String, state: ModelState?) {
        downloading.value = if (state == null) downloading.value - code else downloading.value + (code to state)
        publish(settings.translationLastUsed.get())
    }
}

class TranslationItemHandler(
    private val settings: NewSettingsHandling,
    private val translateItems: TranslateItems = TranslateItems(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : TranslationHandler {
    override fun translateDescription(
        textToTranslate: String,
        progress: (Boolean) -> Unit,
        translatedText: (String) -> Unit,
    ) {
        progress(true)
        scope.launch {
            try {
                val translated = translate(textToTranslate)
                if (translated != textToTranslate) translatedText(translated)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                progress(false)
            }
        }
    }

    override suspend fun translate(textToTranslate: String): String {
        val target = settings.translationTargetLanguage.get()
        return translateItems.translate(textToTranslate, target) { source ->
            settings.translationLastUsed.set(
                settings.translationLastUsed.get() + (source to System.currentTimeMillis())
            )
        }
    }

    override fun clear() = translateItems.clear()
}
