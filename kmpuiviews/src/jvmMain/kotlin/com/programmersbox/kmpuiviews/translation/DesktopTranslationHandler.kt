package com.programmersbox.kmpuiviews.translation

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.IOException

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
        val target = appSettings.translationTargetLanguage.get()
        val source = detector.detect(textToTranslate)
        if (source == null || source == target) return textToTranslate
        return try {
            val kind = runCatching { EngineKind.valueOf(settings.engine.get()) }
                .getOrDefault(EngineKind.Bergamot)
            val engine = engines[kind]
                ?: throw TranslationUnavailable("Pick a translation engine in Settings → Translation")
            engine.translate(textToTranslate, source, target)
        } catch (e: TranslationUnavailable) {
            fail(e)
            textToTranslate
        } catch (e: IOException) {
            fail(e)
            textToTranslate
        }
    }

    private fun fail(e: Exception) {
        notifier.error(title = "Couldn't translate", subtitle = e.message)
    }

    override fun clear() {
        scope.coroutineContext.cancelChildren()
    }
}
