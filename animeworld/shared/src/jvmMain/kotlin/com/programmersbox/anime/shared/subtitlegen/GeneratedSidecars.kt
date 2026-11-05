package com.programmersbox.anime.shared.subtitlegen

import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.downloads.SidecarEntry
import com.programmersbox.anime.shared.downloads.SubtitleFormat
import com.programmersbox.anime.shared.downloads.sidecarIndexName
import com.programmersbox.anime.shared.downloads.sidecarName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Saves a generated subtitle track next to a video as a sidecar listed in its `.subtitles.json` index. */
object GeneratedSidecars {
    private val json = Json { prettyPrint = true }

    /**
     * Moves [vtt] next to [video] as a new generated track for [language] and returns it. A previously
     * generated track for the same language is replaced; downloaded tracks are never touched.
     */
    fun add(video: File, vtt: File, language: WhisperLanguage): File {
        val folder = video.absoluteFile.parentFile
        val base = video.nameWithoutExtension
        val indexFile = File(folder, sidecarIndexName(base))

        val existing = OfflineSubtitles.readIndex(indexFile) ?: OfflineSubtitles.find(video).map {
            SidecarEntry(file = File(URI(it.url)).name, lang = it.lang, label = it.lang)
        }
        val (replaced, kept) = existing.partition { it.generated && it.lang == language.code }

        val nextIndex = (existing.maxOfOrNull { indexOf(it.file, base) } ?: -1) + 1
        val label = "${language.name} (generated)"
        val name = sidecarName(base, nextIndex, label, SubtitleFormat.Vtt)
        val target = File(folder, name)
        Files.move(vtt.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val entries = kept + SidecarEntry(file = name, lang = language.code, label = label, generated = true)
        val tmp = File(folder, "${indexFile.name}.tmp")
        tmp.writeText(json.encodeToString(entries))
        Files.move(tmp.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        replaced.forEach { File(folder, File(it.file).name).delete() }
        return target
    }

    /** `<base>.<index>.<label>.<ext>` index, or -1 when the name does not follow that pattern. */
    private fun indexOf(file: String, base: String): Int =
        file.removePrefix("$base.").substringBefore('.').toIntOrNull() ?: -1
}
