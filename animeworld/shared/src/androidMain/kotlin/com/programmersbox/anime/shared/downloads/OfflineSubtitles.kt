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
    fun resolveVideoFile(context: Context, path: String): File? {
        val uri = runCatching { Uri.parse(path) }.getOrNull()
        return when (uri?.scheme) {
            null, "" -> File(path)
            "file" -> uri.path?.let(::File)
            "content" -> runCatching {
                context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull()?.let(::File)
            else -> null
        }
    }

    fun deleteFor(video: File) {
        val folder = video.absoluteFile.parentFile ?: return
        val base = video.nameWithoutExtension
        sidecars(folder, base).forEach { it.second.delete() }
        File(folder, sidecarIndexName(base)).delete()
    }

    private fun fromIndex(folder: File, base: String): List<KmpSubtitle>? {
        val index = File(folder, sidecarIndexName(base)).takeIf { it.isFile } ?: return null
        val entries = runCatching { Json.decodeFromString<List<SidecarEntry>>(index.readText()) }
            .getOrNull() ?: return null
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

    /** `<base>.<index>.<label>.<ext>` files paired with their index. */
    private fun sidecars(folder: File, base: String): List<Pair<Int, File>> =
        folder.listFiles().orEmpty().mapNotNull { file ->
            if (!file.isFile || file.extension.lowercase() !in extensions) return@mapNotNull null
            val rest = file.name.removePrefix("$base.")
            if (rest.length == file.name.length || !rest.contains('.')) return@mapNotNull null
            val index = rest.substringBefore('.').toIntOrNull() ?: return@mapNotNull null
            index to file
        }
}
