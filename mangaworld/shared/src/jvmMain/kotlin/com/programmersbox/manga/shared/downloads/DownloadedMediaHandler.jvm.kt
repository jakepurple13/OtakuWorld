package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
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
            ).observeExtensionsDir()
        ) { path, _ -> path }
            .mapNotNull { rootDir ->
                File(rootDir)
                    .listFiles { it.isDirectory && !it.isHidden }
                    ?.flatMap { file ->
                        println(file)
                        val title = file.name
                        file
                            .listFiles()
                            ?.flatMap { c ->
                                println(c)
                                c
                                    .listFiles()
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


    actual fun delete(downloadedChapters: DownloadedChapters) {
    }

    actual fun clear() {
    }
}