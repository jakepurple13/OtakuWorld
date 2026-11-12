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
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.NanoTools
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Task-specific Gemini Nano APIs (ML Kit GenAI Summarization / Proofreading / Rewriting).
 *
 * Recap and ListSuggest are never ready here: Summarization only offers 1-3 bullets, not the
 * 2-3 sentence prose Recap needs, so those go through the Prompt API.
 */
class MlKitNanoTools(private val context: Context) : NanoTools {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadStarted = mutableMapOf<AiFeature, AtomicBoolean>()

    private val summarizer: Summarizer by lazy {
        Summarization.getClient(
            SummarizerOptions.builder(context)
                .setInputType(SummarizerOptions.InputType.ARTICLE)
                .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
                .setLanguage(SummarizerOptions.Language.ENGLISH)
                .build()
        )
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

    override suspend fun isReady(feature: AiFeature, inputLength: Int): Boolean {
        if (inputLength > AiTextPrompts.NANO_TEXT_LIMIT) return false
        val check: Pair<ListenableFuture<Int>, (DownloadCallback) -> ListenableFuture<Void>> = when (feature) {
            AiFeature.Tldr -> summarizer.checkFeatureStatus() to summarizer::downloadFeature
            AiFeature.Proofread -> proofreader.checkFeatureStatus() to proofreader::downloadFeature
            // The status is per model, so any style stands in for "rewriting is ready".
            AiFeature.Rewrite -> rewriter(RewriteStyle.Rephrase).let { it.checkFeatureStatus() to it::downloadFeature }
            AiFeature.Recap, AiFeature.ListSuggest -> return false
        }
        val status = runCatching { check.first.await() }.getOrDefault(FeatureStatus.UNAVAILABLE)
        if (status == FeatureStatus.DOWNLOADABLE) {
            val started = synchronized(downloadStarted) { downloadStarted.getOrPut(feature) { AtomicBoolean(false) } }
            if (started.compareAndSet(false, true)) {
                scope.launch {
                    runCatching { check.second(object : DownloadCallback {}).await() }
                        .onFailure { started.set(false) }
                }
            }
        }
        return status == FeatureStatus.AVAILABLE
    }

    override fun run(feature: AiFeature, input: String, style: RewriteStyle?): Flow<String> = flow {
        val text = when (feature) {
            AiFeature.Tldr -> summarizer
                .runInference(SummarizationRequest.builder(input).build()).await()
                .summary

            AiFeature.Proofread -> proofreader
                .runInference(ProofreadingRequest.builder(input).build()).await()
                .results.firstOrNull()?.text

            AiFeature.Rewrite -> rewriter(style ?: RewriteStyle.Rephrase)
                .runInference(RewritingRequest.builder(input).build()).await()
                .results.firstOrNull()?.text

            AiFeature.Recap, AiFeature.ListSuggest ->
                throw UnsupportedOperationException("$feature is not supported by ML Kit Nano tools")
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
