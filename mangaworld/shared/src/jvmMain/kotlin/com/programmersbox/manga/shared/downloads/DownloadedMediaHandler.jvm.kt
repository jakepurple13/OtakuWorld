package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import java.io.File

actual class DownloadedMediaHandler(
    private val mangaDesktopSettings: MangaDesktopSettings,
) {
    actual fun init(folderLocation: String) {
    }

    actual fun listenToUpdates(): Flow<List<DownloadedChapters>> =
        combine(
            mangaDesktopSettings.downloadsDirectory.asFlow(),
            ExtensionWatcher(
                mangaDesktopSettings
                    .downloadsDirectory
                    .asFlow()
            ).observeExtensionsDir(),
            deletions,
        ) { path, _, _ -> path }
            .mapNotNull { rootDir ->
                File(rootDir)
                    .listFiles { it.isDirectory && !it.isHidden }
                    ?.flatMap { file ->
                        println(file)
                        val title = file.name
                        file
                            .listFiles()
                            ?.filter { isCompleteChapter(it.list()) }
                            ?.flatMap { c ->
                                println(c)
                                c
                                    .listFiles { f -> !f.name.startsWith(".") }
                                    ?.map {
                                        println(it)
                                        DownloadedChapters(
                                            name = title,
                                            data = it.absolutePath,
                                            assetFileStringUri = "",
                                            id = it.absolutePath,
                                            folder = file.absolutePath,
                                            folderName = file.name,
                                            chapterFolder = c.absolutePath,
                                            chapterName = c.name,
                                        )
                                    } ?: emptyList()
                            } ?: emptyList()
                    }
                    ?: emptyList()
            }


    // The directory watcher only sees the root folder, so deletes refresh the list themselves
    private val deletions = MutableStateFlow(0)

    actual fun delete(downloadedChapters: DownloadedChapters) {
        File(downloadedChapters.chapterFolder).deleteRecursively()
        deletions.update { it + 1 }
    }

    actual fun clear() {
    }
}