package com.programmersbox.kmpuiviews.aitext

import android.content.Context
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.proofreading.Proofreader
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading
import com.google.mlkit.genai.proofreading.ProofreadingRequest
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.rewriting.RewriterOptions
import com.google.mlkit.genai.rewriting.Rewriting
import com.google.mlkit.genai.rewriting.RewritingRequest
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.NanoTask
import com.programmersbox.kmpuiviews.domain.aitext.NanoTools
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

/**
 * Task-specific Gemini Nano APIs (ML Kit GenAI Summarization / Proofreading / Rewriting).
 *
 * Features without an [com.programmersbox.kmpuiviews.domain.aitext.AiFeature.nanoTask] (Recap,
 * list suggestions) never come here: Summarization only offers 1-3 bullets, not the 2-3 sentence
 * prose Recap needs, so those go through the Prompt API.
 *
 * Summarize (TL;DR) is only ready when the translation target language is one Summarization supports
 * (English, Japanese, Korean): the Details screen translates the description to that target
 * before summarizing. For any other target, Tldr goes through the Prompt API, then Koog.
 */
class MlKitNanoTools(
    private val context: Context,
    private val settings: NewSettingsHandling,
) : NanoTools {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Keyed by task kind: one download covers every rewrite style
    private val downloadStarted = mutableMapOf<KClass<out NanoTask>, AtomicBoolean>()

    private val summarizers = mutableMapOf<Int, Summarizer>()

    private fun summarizer(language: Int): Summarizer = synchronized(summarizers) {
        summarizers.getOrPut(language) {
            Summarization.getClient(
                SummarizerOptions.builder(context)
                    .setInputType(SummarizerOptions.InputType.ARTICLE)
                    .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
                    .setLanguage(language)
                    .build()
            )
        }
    }

    /** The summarizer for the translation target language, or null when Summarization doesn't support it. */
    private suspend fun targetSummarizer(): Summarizer? {
        val language = when (settings.translationTargetLanguage.get().substringBefore('-')) {
            "en" -> SummarizerOptions.Language.ENGLISH
            "ja" -> SummarizerOptions.Language.JAPANESE
            "ko" -> SummarizerOptions.Language.KOREAN
            else -> return null
        }
        return summarizer(language)
    }

    private val proofreader: Proofreader by lazy {
        Proofreading.getClient(
            ProofreaderOptions.builder(context)
                .setInputType(ProofreaderOptions.InputType.KEYBOARD)
                .setLanguage(proofreadingLanguage())
                .build()
        )
    }

    private val rewriters = mutableMapOf<RewriteStyle, Rewriter>()

    private fun rewriter(style: RewriteStyle): Rewriter = synchronized(rewriters) {
        rewriters.getOrPut(style) {
            Rewriting.getClient(
                RewriterOptions.builder(context)
                    .setOutputType(style.toOutputType())
                    .setLanguage(rewritingLanguage())
                    .build()
            )
        }
    }

    override suspend fun isReady(task: NanoTask, inputLength: Int): Boolean = withContext(Dispatchers.IO) {
        if (inputLength > AiTextPrompts.NANO_TEXT_LIMIT) return@withContext false
        val check: Pair<ListenableFuture<Int>, (DownloadCallback) -> ListenableFuture<Void>> = when (task) {
            NanoTask.Summarize -> targetSummarizer()
                ?.let { it.checkFeatureStatus() to it::downloadFeature }
                ?: return@withContext false
            NanoTask.Proofread -> proofreader.checkFeatureStatus() to proofreader::downloadFeature
            // The status is per model, so the requested style stands in for "rewriting is ready".
            is NanoTask.Rewrite -> rewriter(task.style).let { it.checkFeatureStatus() to it::downloadFeature }
        }
        val status = try {
            check.first.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FeatureStatus.UNAVAILABLE
        }
        if (status == FeatureStatus.DOWNLOADABLE) {
            val started = synchronized(downloadStarted) { downloadStarted.getOrPut(task::class) { AtomicBoolean(false) } }
            if (started.compareAndSet(false, true)) {
                scope.launch {
                    try {
                        check.second(object : DownloadCallback {}).await()
                    } catch (e: CancellationException) {
                        started.set(false)
                        throw e
                    } catch (e: Exception) {
                        started.set(false)
                    }
                }
            }
        }
        status == FeatureStatus.AVAILABLE
    }

    override fun run(task: NanoTask, input: String): Flow<String> = flow {
        val text = when (task) {
            NanoTask.Summarize -> (targetSummarizer() ?: error("Summarization doesn't support the target language"))
                .runInference(SummarizationRequest.builder(input).build()).await()
                .summary

            NanoTask.Proofread -> proofreader
                .runInference(ProofreadingRequest.builder(input).build()).await()
                .results.firstOrNull()?.text

            is NanoTask.Rewrite -> rewriter(task.style)
                .runInference(RewritingRequest.builder(input).build()).await()
                .results.firstOrNull()?.text
        }
        // A single chunk is a valid delta stream.
        emit(text?.takeIf { it.isNotBlank() } ?: error("ML Kit returned no result"))
    }.flowOn(Dispatchers.IO)

    private fun RewriteStyle.toOutputType() = when (this) {
        RewriteStyle.Rephrase -> RewriterOptions.OutputType.REPHRASE
        RewriteStyle.Shorten -> RewriterOptions.OutputType.SHORTEN
        RewriteStyle.Elaborate -> RewriterOptions.OutputType.ELABORATE
        RewriteStyle.Friendly -> RewriterOptions.OutputType.FRIENDLY
        RewriteStyle.Professional -> RewriterOptions.OutputType.PROFESSIONAL
        RewriteStyle.Emojify -> RewriterOptions.OutputType.EMOJIFY
    }

    private fun proofreadingLanguage() = when (Locale.getDefault().language) {
        "ja" -> ProofreaderOptions.Language.JAPANESE
        "ko" -> ProofreaderOptions.Language.KOREAN
        "de" -> ProofreaderOptions.Language.GERMAN
        "fr" -> ProofreaderOptions.Language.FRENCH
        "it" -> ProofreaderOptions.Language.ITALIAN
        "es" -> ProofreaderOptions.Language.SPANISH
        else -> ProofreaderOptions.Language.ENGLISH
    }

    private fun rewritingLanguage() = when (Locale.getDefault().language) {
        "ja" -> RewriterOptions.Language.JAPANESE
        "ko" -> RewriterOptions.Language.KOREAN
        "de" -> RewriterOptions.Language.GERMAN
        "fr" -> RewriterOptions.Language.FRENCH
        "it" -> RewriterOptions.Language.ITALIAN
        "es" -> RewriterOptions.Language.SPANISH
        else -> RewriterOptions.Language.ENGLISH
    }
}

private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    addListener({
        try {
            cont.resume(get())
        } catch (e: Throwable) {
            cont.resumeWithException(e.cause ?: e)
        }
    }, Runnable::run)
    cont.invokeOnCancellation { cancel(false) }
}
