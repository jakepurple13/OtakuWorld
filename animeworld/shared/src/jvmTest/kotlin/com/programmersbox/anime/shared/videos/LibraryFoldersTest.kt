package com.programmersbox.anime.shared.videos

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryFoldersTest {
    private inline fun withTempDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("libraryFolders").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun File.touch(path: String) = resolve(path).apply { parentFile.mkdirs(); writeText("v") }

    @Test
    fun otherVideosSkipHiddenBundlesDownloadsAndDeepFolders() = withTempDir { root ->
        root.touch("Movie.mkv")
        root.touch("Clips/clip.mp4")
        root.touch("notes.txt")
        root.touch(".hidden/secret.mp4")
        root.touch("._Movie.mkv")
        root.touch("Photos Library.photoslibrary/originals/a.mov")
        root.touch("AnimeWorld/Show/Ep 1.mp4")
        root.touch("a/b/c/d/e/f/g/too-deep.mp4")

        val found = otherVideoFiles(root, exclude = root.resolve("AnimeWorld")).map { it.relativeTo(root).path }.sorted().toList()

        assertEquals(listOf("Clips/clip.mp4", "Movie.mkv"), found)
    }

    @Test
    fun folderInsideDownloadsIsNotScanned() = withTempDir { root ->
        root.touch("AnimeWorld/Show/Ep 1.mp4")

        assertEquals(emptyList(), otherVideoFiles(root.resolve("AnimeWorld/Show"), exclude = root.resolve("AnimeWorld")).toList())
    }

    @Test
    fun effectiveFoldersDropMissingDisabledAndNested() = withTempDir { root ->
        val movies = root.resolve("Movies").apply { mkdirs() }
        val nested = movies.resolve("Anime").apply { mkdirs() }
        val desktop = root.resolve("Desktop").apply { mkdirs() }
        val missing = root.resolve("Videos")

        val folders = effectiveLibraryFolders(
            defaults = listOf(movies.path, missing.path, desktop.path),
            extras = setOf(nested.path),
            disabled = setOf(desktop.path),
        )

        assertEquals(listOf(movies.absoluteFile), folders)
    }

    @Test
    fun cleanupGroupsSplitWatchedOtherAndRest() {
        fun video(isDownload: Boolean, duration: Long, position: Long) =
            SharedVideoContent("v", "p", duration, position, isDownload = isDownload)

        assertEquals(CleanupGroup.Watched, video(true, 1_000, 900).cleanupGroup())
        assertEquals(CleanupGroup.Rest, video(true, 1_000, 899).cleanupGroup())
        assertEquals(CleanupGroup.Rest, video(true, 0, 500).cleanupGroup())
        assertEquals(CleanupGroup.Other, video(false, 1_000, 1_000).cleanupGroup())
    }
}
