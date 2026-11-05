package com.programmersbox.anime.shared.subtitlegen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Extracts mono 16 kHz PCM, the only input whisper.cpp accepts. */
fun extractAudioArgs(ffmpeg: String, video: File, wav: File): List<String> = listOf(
    ffmpeg, "-hide_banner", "-nostdin", "-y", "-i", video.path,
    "-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", wav.path,
)

fun whisperArgs(whisper: String, model: File, wav: File, outBase: File, request: GenerationRequest): List<String> =
    listOf(whisper, "-m", model.path, "-f", wav.path, "-l", request.from.code, "-ovtt", "-of", outBase.path, "-pp") +
        if (request.translate) listOf("-tr") else emptyList()

private val progressRegex = Regex("""progress\s*=\s*(\d{1,3})%""")

fun parseWhisperProgress(line: String): Int? =
    progressRegex.find(line)?.groupValues?.get(1)?.toInt()?.coerceIn(0, 100)

/** The whisper-cli to run: the configured path when executable, else the first one on PATH or a usual install dir. */
suspend fun findWhisperCli(configured: String): File? = withContext(Dispatchers.IO) {
    val file = configured.takeIf { it.isNotBlank() }?.let(::File)
    if (file != null && file.canExecute()) return@withContext file
    val exe = if (System.getProperty("os.name").startsWith("Windows")) "whisper-cli.exe" else "whisper-cli"
    val dirs = System.getenv("PATH").orEmpty().split(File.pathSeparator) +
        listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")
    dirs.filter { it.isNotBlank() }.map { File(it, exe) }.firstOrNull { it.canExecute() }
}

fun remainingMs(elapsedMs: Long, fraction: Float): Long? =
    if (fraction < 0.02f) null else (elapsedMs / fraction * (1 - fraction)).toLong()
