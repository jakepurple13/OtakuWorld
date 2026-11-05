package com.programmersbox.anime.shared.subtitlegen

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class WhisperLanguage(val code: String, val name: String)

val WhisperAuto = WhisperLanguage("auto", "Detect automatically")

/** Languages offered for subtitle generation, excluding [WhisperAuto]. */
val whisperLanguages: List<WhisperLanguage> = listOf(
    WhisperLanguage("ja", "Japanese"),
    WhisperLanguage("en", "English"),
    WhisperLanguage("ko", "Korean"),
    WhisperLanguage("zh", "Chinese"),
    WhisperLanguage("es", "Spanish"),
)

data class TargetOption(val language: WhisperLanguage, val enabled: Boolean, val note: String)

private val English = whisperLanguages.first { it.code == "en" }

/**
 * Whisper only translates into English, so "To" offers English and the "From" language
 * (transcribed as spoken). Every other language is listed but disabled.
 */
fun targetOptions(from: WhisperLanguage): List<TargetOption> {
    val same = if (from.code == "auto") WhisperLanguage("auto", "Same as spoken") else from
    val enabled = buildList {
        if (from.code != "en") add(TargetOption(English, true, "Translated"))
        add(TargetOption(same, true, "Transcribed as spoken"))
    }
    val disabled = whisperLanguages
        .filter { it.code != "en" && it.code != from.code }
        .map { TargetOption(it, false, "Needs a translation add-on") }
    return enabled + disabled
}

fun fromOptions(englishOnlyModel: Boolean): List<WhisperLanguage> =
    if (englishOnlyModel) listOf(English) else listOf(WhisperAuto) + whisperLanguages

data class GenerationRequest(val from: WhisperLanguage, val to: WhisperLanguage)

val GenerationRequest.translate: Boolean get() = to.code == "en" && from.code != "en"

sealed interface GenerationStatus {
    data class Queued(val afterTitle: String) : GenerationStatus
    data object ExtractingAudio : GenerationStatus
    data class DownloadingModel(val modelName: String, val fraction: Float) : GenerationStatus
    data class Transcribing(
        val from: WhisperLanguage,
        val to: WhisperLanguage,
        val fraction: Float,
        val remainingMs: Long?,
    ) : GenerationStatus

    data class Failed(val message: String, val request: GenerationRequest) : GenerationStatus
}

data class ModelSummary(
    val name: String,
    val downloaded: Boolean,
    val sizeLabel: String,
    val translatesWell: Boolean,
    val englishOnly: Boolean,
)

/** Map key and [finished] value are the video's absolute path. */
interface SubtitleGenerator {
    val statuses: StateFlow<Map<String, GenerationStatus>>
    val finished: SharedFlow<String>
    val model: StateFlow<ModelSummary>
    val lastRequest: StateFlow<GenerationRequest>
    fun enqueue(videoPath: String, request: GenerationRequest, remember: Boolean)
    fun cancel(videoPath: String)
    fun dismiss(videoPath: String)
}
