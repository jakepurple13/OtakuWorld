package com.programmersbox.anime.shared.downloads

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.serialization.json.Json
import java.io.File

/** Subtitle sidecars written next to a downloaded episode by [EpisodeDownloader]. */
object OfflineSubtitles {
    private val extensions = setOf("vtt", "srt", "ass", "ssa")

    /** Sidecars from the `<base>.subtitles.json` index (missing files skipped), else a glob by index. */
    fun find(video: File): List<KmpSubtitle> {
        val folder = video.absoluteFile.parentFile ?: return emptyList()
        val base = video.nameWithoutExtension
        return fromIndex(folder, base) ?: fromGlob(folder, base)
    }

    /** Library entries are `content://` MediaStore URIs; sidecars sit next to the real file. */
    fun resolveVideoFile(context: Context, path: String): File? =
        resolveVideoFile(path) { uri ->
            runCatching {
                context.contentResolver.query(
                    Uri.parse(uri), arrayOf(MediaStore.MediaColumns.DATA), null, null, null,
                )?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull()
        }

    internal fun resolveVideoFile(path: String, contentData: (String) -> String?): File? = when {
        path.startsWith("content://") -> contentData(path)?.let(::File)
        path.startsWith("file:") -> runCatching { File(java.net.URI(path)) }.getOrNull()
        else -> File(path)
    }

    /** Removes the sidecars named by the index plus the glob matches (sibling-safe), then the index. */
    fun deleteFor(video: File) {
        val folder = video.absoluteFile.parentFile ?: return
        val base = video.nameWithoutExtension
        val index = File(folder, sidecarIndexName(base))
        val entries = readIndex(index)
        entries?.forEach { File(folder, File(it.file).name).delete() }
        sidecars(folder, base).forEach { it.second.delete() }
        index.delete()
    }

    private fun readIndex(index: File): List<SidecarEntry>? =
        index.takeIf { it.isFile }
            ?.let { runCatching { Json.decodeFromString<List<SidecarEntry>>(it.readText()) }.getOrNull() }

    private fun fromIndex(folder: File, base: String): List<KmpSubtitle>? {
        val entries = readIndex(File(folder, sidecarIndexName(base))) ?: return null
        return entries.mapNotNull { entry ->
            // The index stores a file name; never follow a path out of the folder.
            val file = File(folder, File(entry.file).name).takeIf { it.isFile } ?: return@mapNotNull null
            KmpSubtitle(url = file.toURI().toString(), lang = entry.label)
        }
    }

    private fun fromGlob(folder: File, base: String): List<KmpSubtitle> =
        sidecars(folder, base)
            .sortedBy { it.first }
            .map { (_, file) ->
                val label = file.name.removePrefix("$base.").substringAfter('.').substringBeforeLast('.')
                KmpSubtitle(url = file.toURI().toString(), lang = label)
            }

    /**
     * `<base>.<index>.<label>.<ext>` files paired with their index. Files that belong to a longer-named
     * video next to this one (`Episode 12.5.mp4` vs base `Episode 12`) are excluded.
     */
    private fun sidecars(folder: File, base: String): List<Pair<Int, File>> {
        val files = folder.listFiles().orEmpty().filter { it.isFile }
        val otherBases = files
            .filter { it.extension.lowercase() !in extensions && it.extension != "json" && it.extension != "part" }
            .map { it.nameWithoutExtension }
            .filter { it.length > base.length && it.startsWith("$base.") }
        return files.mapNotNull { file ->
            if (file.extension.lowercase() !in extensions) return@mapNotNull null
            if (otherBases.any { file.name.startsWith("$it.") }) return@mapNotNull null
            val rest = file.name.removePrefix("$base.")
            if (rest.length == file.name.length) return@mapNotNull null
            val m = sidecarRest.matchEntire(rest) ?: return@mapNotNull null
            m.groupValues[1].toInt() to file
        }
    }

    private val sidecarRest = Regex("""(\d{1,9})\..+\.[^.]+""")
}
