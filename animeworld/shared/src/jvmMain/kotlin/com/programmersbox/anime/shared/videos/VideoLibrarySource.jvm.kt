package com.programmersbox.anime.shared.videos

import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import java.awt.Desktop
import java.io.File
import java.util.prefs.Preferences

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "ts")

/** macOS app and media-library packages: folders on disk, but never worth walking into. */
private val BUNDLE_EXTENSIONS = setOf("app", "photoslibrary", "fcpbundle", "imovielibrary", "tvlibrary", "musiclibrary")

/** Library folders like ~/Downloads can be huge; videos deeper than this are skipped. */
private const val MAX_FOLDER_DEPTH = 6

/** How often library folders are rescanned while "Watch for new files" is on. */
private const val WATCH_INTERVAL_MS = 30_000L

/** Videos under [root], skipping the `<base>.mp4.part.tracks` folders a DASH download works in. */
internal fun libraryVideoFiles(root: File): Sequence<File> = root
    .walkTopDown()
    .onEnter { !it.name.endsWith(".tracks") }
    .filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS }

/**
 * Videos under a library folder [root]. Skips hidden folders, app/library bundles, DASH work folders,
 * the AnimeWorld downloads folder [exclude] (those are listed as downloads) and anything deeper than
 * [MAX_FOLDER_DEPTH].
 */
internal fun otherVideoFiles(root: File, exclude: File): Sequence<File> {
    val excluded = exclude.toPath().toAbsolutePath().normalize()
    if (root.toPath().toAbsolutePath().normalize().startsWith(excluded)) return emptySequence()
    return root
        .walkTopDown()
        .maxDepth(MAX_FOLDER_DEPTH)
        .onEnter { dir ->
            dir == root || (
                !dir.name.startsWith('.') &&
                    !dir.name.endsWith(".tracks") &&
                    dir.extension.lowercase() !in BUNDLE_EXTENSIONS &&
                    dir.toPath().toAbsolutePath().normalize() != excluded
                )
        }
        .filter { it.isFile && !it.name.startsWith('.') && it.extension.lowercase() in VIDEO_EXTENSIONS }
}

/**
 * The folders to scan: [defaults] that exist plus [extras], minus [disabled]. A folder inside another
 * listed folder is dropped so its videos aren't listed twice.
 */
internal fun effectiveLibraryFolders(
    defaults: List<String>,
    extras: Set<String>,
    disabled: Set<String>,
): List<File> {
    val folders = (defaults.filter { File(it).isDirectory } + extras)
        .distinct()
        .filter { it !in disabled }
        .map { File(it).absoluteFile }
    return folders.filter { folder ->
        folders.none { other -> other != folder && folder.toPath().normalize().startsWith(other.toPath().normalize()) }
    }
}

/** [path] with the home folder shortened to `~`, e.g. `~/Movies`. */
fun libraryFolderLabel(path: String): String {
    val home = System.getProperty("user.home")
    return if (path == home || path.startsWith(home + File.separator)) "~" + path.removePrefix(home) else path
}

/** Deletes [video] with its subtitle sidecars and index. */
internal fun deleteWithSidecars(video: File) {
    OfflineSubtitles.sidecarFiles(video).forEach { it.delete() }
    video.delete()
}

/** Whether the OS can move files to the Trash; when it can't, other videos are deleted permanently. */
internal val canMoveToTrash: Boolean by lazy {
    runCatching { Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH) }
        .getOrDefault(false)
}

/** Which library folder is being scanned, for the "Looking for videos…" card. */
data class LibraryScanProgress(val folder: String, val done: Int, val total: Int)

@OptIn(ExperimentalCoroutinesApi::class)
actual class VideoLibrarySource(private val animeDesktopSettings: AnimeDesktopSettings) {

    private val resumePositions = Preferences.userNodeForPackage(VideoLibrarySource::class.java)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val refresh = MutableStateFlow(0)

    private val _scanProgress = MutableStateFlow<LibraryScanProgress?>(null)

    /** Non-null while library folders are being scanned. */
    val scanProgress: StateFlow<LibraryScanProgress?> = _scanProgress.asStateFlow()

    /** Scans everything again now. */
    fun rescan() = refresh.update { it + 1 }

    private data class ScanConfig(
        val downloads: String,
        val includeOther: Boolean,
        val folders: List<File>,
        val watch: Boolean,
    )

    private val config = combine(
        animeDesktopSettings.downloadsDirectory.asFlow(),
        animeDesktopSettings.includeOtherFolders.asFlow(),
        animeDesktopSettings.extraLibraryFolders.asFlow(),
        animeDesktopSettings.disabledLibraryFolders.asFlow(),
        animeDesktopSettings.watchLibraryFolders.asFlow(),
    ) { downloads, include, extras, disabled, watch ->
        ScanConfig(
            downloads = downloads,
            includeOther = include,
            folders = effectiveLibraryFolders(AnimeDesktopSettings.DEFAULT_LIBRARY_FOLDERS, extras, disabled),
            watch = watch,
        )
    }

    private fun toContent(file: File, isDownload: Boolean, folder: String?) = SharedVideoContent(
        videoName = file.nameWithoutExtension,
        path = file.absolutePath,
        duration = 0L,
        lastPlayedPositionMs = resumePositions.getLong(file.absolutePath, 0L),
        sizeBytes = file.length(),
        dateAddedMs = file.lastModified(),
        isDownload = isDownload,
        folder = folder,
    )

    private fun scan(config: ScanConfig): List<SharedVideoContent> {
        val downloadsRoot = File(config.downloads)
        val downloads = if (downloadsRoot.exists()) {
            libraryVideoFiles(downloadsRoot).map { toContent(it, isDownload = true, folder = null) }.toList()
        } else {
            emptyList()
        }
        if (!config.includeOther) return downloads
        val others = try {
            config.folders.flatMapIndexed { index, folder ->
                val label = libraryFolderLabel(folder.path)
                _scanProgress.value = LibraryScanProgress(label, index, config.folders.size)
                otherVideoFiles(folder, downloadsRoot).map { toContent(it, isDownload = false, folder = label) }.toList()
            }
        } finally {
            _scanProgress.value = null
        }
        return downloads + others
    }

    private val videos: Flow<List<SharedVideoContent>> = config
        .flatMapLatest { config ->
            val ticks = if (config.watch && config.includeOther) {
                flow {
                    while (true) {
                        delay(WATCH_INTERVAL_MS)
                        emit(Unit)
                    }
                }
            } else {
                emptyFlow()
            }
            merge(refresh.map { }, ticks)
                .conflate()
                .map { scan(config) }
        }
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    actual fun observeVideos(): Flow<List<SharedVideoContent>> = videos

    actual fun getResumePosition(path: String): Long = resumePositions.getLong(path, 0L)

    actual fun setResumePosition(path: String, positionMs: Long) {
        resumePositions.putLong(path, positionMs)
    }

    /** Downloads are deleted with their subtitles; other videos go to the Trash when the OS supports it. */
    actual fun delete(content: SharedVideoContent) {
        val file = File(content.path)
        when {
            content.isDownload -> deleteWithSidecars(file)
            canMoveToTrash -> runCatching { Desktop.getDesktop().moveToTrash(file) }.onFailure { it.printStackTrace() }
            else -> file.delete()
        }
        rescan()
    }
}
