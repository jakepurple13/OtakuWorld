package com.programmersbox.kmpuiviews.translation

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class DesktopTranslationHandler(
    private val engines: Map<EngineKind, TranslationEngine>,
    private val detector: LanguageDetector,
    private val settings: DesktopTranslationSettings,
    private val appSettings: NewSettingsHandling,
    private val notifier: InAppNotifier,
) : TranslationHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun translateDescription(
        textToTranslate: String,
        progress: (Boolean) -> Unit,
        translatedText: (String) -> Unit,
    ) {
        scope.launch {
            progress(true)
            try {
                translatedText(translate(textToTranslate))
            } finally {
                progress(false)
            }
        }
    }

    override suspend fun translate(textToTranslate: String): String {
        return try {
            // Detection and settings reads can throw too (profile load, disk); keep them inside the catch.
            val target = appSettings.translationTargetLanguage.get()
            val source = withContext(Dispatchers.Default) { detector.detect(textToTranslate) }
            if (source == null || source == target) return textToTranslate
            val kind = runCatching { EngineKind.valueOf(settings.engine.get()) }
                .getOrDefault(EngineKind.Ollama)
            val engine = engines[kind]
                ?: throw TranslationUnavailable("Pick a translation engine in Settings → Translation")
            engine.translate(textToTranslate, source, target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(e)
            textToTranslate
        }
    }

    private fun fail(e: Exception) {
        notifier.error(title = "Couldn't translate", subtitle = e.message ?: "Something went wrong")
    }

    override fun clear() {
        scope.coroutineContext.cancelChildren()
    }
}
