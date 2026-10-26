package com.programmersbox.anime.shared.videos

import com.programmersbox.anime.shared.downloads.sidecarIndexName
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class VideoLibrarySourceTest {
    @Test
    fun libraryListsVideosButNotDashTempTracks() {
        val dir = Files.createTempDirectory("library").toFile()
        try {
            dir.resolve("Show/Ep 1.mp4").apply { parentFile.mkdirs(); writeText("v") }
            dir.resolve("Show/Ep 2.mp4.part.tracks/video.mp4").apply { parentFile.mkdirs(); writeText("v") }
            dir.resolve("Show/Ep 2.mp4.part.tracks/audio.mp4").writeText("a")
            dir.resolve("Show/Ep 2.mp4.part").writeText("p")

            assertEquals(listOf("Ep 1.mp4"), libraryVideoFiles(dir).map { it.name }.toList())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun deleteRemovesSubtitleSidecarsAndIndex() {
        val dir = Files.createTempDirectory("library").toFile()
        try {
            val video = dir.resolve("Ep 1.mp4").apply { writeText("v") }
            val other = dir.resolve("Ep 2.mp4").apply { writeText("v") }
            val otherSub = dir.resolve("Ep 2.0.English.vtt").apply { writeText("w") }
            dir.resolve("Ep 1.0.English.vtt").writeText("w")
            dir.resolve("Ep 1.1.Spanish.vtt").writeText("w")
            dir.resolve(sidecarIndexName("Ep 1")).writeText("[]")

            deleteWithSidecars(video)

            assertEquals(listOf(other.name, otherSub.name).sorted(), dir.list().orEmpty().sorted())
            assertFalse(video.exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
