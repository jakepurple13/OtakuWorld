package com.programmersbox.anime.shared.subtitlegen

/** whisper.cpp models the app can download. [speed] and [accuracy] are 1-5 meters. */
enum class BuiltInWhisperModel(
    val id: String,
    val label: String,
    val sizeLabel: String,
    val approxBytes: Long,
    val speed: Int,
    val accuracy: Int,
    val recommended: Boolean = false,
    val sameLanguageOnly: Boolean = false,
) {
    TINY("tiny", "Tiny", "75 MB", 75_000_000L, 5, 1),
    BASE("base", "Base", "142 MB", 142_000_000L, 4, 2),
    SMALL("small", "Small", "466 MB", 466_000_000L, 3, 3, recommended = true),
    MEDIUM("medium", "Medium", "1.5 GB", 1_530_000_000L, 2, 4),
    LARGE_V3("large-v3", "Large v3", "3.1 GB", 3_100_000_000L, 1, 5),
    LARGE_V3_TURBO("large-v3-turbo", "Large v3 Turbo", "1.6 GB", 1_620_000_000L, 3, 4, sameLanguageOnly = true);

    val fileName get() = "ggml-$id.bin"
}

sealed interface WhisperModelChoice {
    data class BuiltIn(val model: BuiltInWhisperModel) : WhisperModelChoice
    data class Custom(val path: String) : WhisperModelChoice
}

fun WhisperModelChoice.encode(): String = when (this) {
    is WhisperModelChoice.BuiltIn -> "builtin:${model.id}"
    is WhisperModelChoice.Custom -> "file:$path"
}

fun decodeModelChoice(value: String): WhisperModelChoice = when {
    value.startsWith("file:") -> WhisperModelChoice.Custom(value.removePrefix("file:"))
    else -> WhisperModelChoice.BuiltIn(
        BuiltInWhisperModel.entries.firstOrNull { "builtin:${it.id}" == value } ?: BuiltInWhisperModel.SMALL
    )
}

private val GGML_MAGIC = byteArrayOf(0x6c, 0x6d, 0x67, 0x67)
private val GGUF_MAGIC = "GGUF".encodeToByteArray()

/** True when the first four bytes are the whisper.cpp ggml (`lmgg`) or `GGUF` magic. */
fun isWhisperModelHeader(first4: ByteArray): Boolean =
    first4.size >= 4 && (first4.copyOf(4).contentEquals(GGML_MAGIC) || first4.copyOf(4).contentEquals(GGUF_MAGIC))

/** English-only models (`ggml-small.en.bin`) cannot transcribe other languages. */
fun isEnglishOnlyModel(fileName: String): Boolean = ".en." in fileName

fun customModelName(fileName: String): String = fileName.removePrefix("ggml-").removeSuffix(".bin")
