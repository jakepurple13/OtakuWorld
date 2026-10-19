package com.programmersbox.anime.shared.downloads

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineSubtitlesTest {

    private lateinit var folder: File

    @BeforeTest
    fun setUp() {
        folder = createTempDirectory("offline-subs").toFile()
    }

    @AfterTest
    fun tearDown() {
        folder.deleteRecursively()
    }

    private fun file(name: String) = File(folder, name).also { it.writeText("x") }

    private fun writeIndex(base: String, vararg entries: SidecarEntry) {
        File(folder, sidecarIndexName(base)).writeText(Json.encodeToString(entries.toList()))
    }

    @Test
    fun findsFromJsonInOrder() {
        val video = file("Ep 1.mp4")
        file("Ep 1.1.French.srt")
        file("Ep 1.0.English.vtt")
        writeIndex(
            "Ep 1",
            SidecarEntry("Ep 1.1.French.srt", "fr", "French"),
            SidecarEntry("Ep 1.0.English.vtt", "en", "English"),
        )

        val subs = OfflineSubtitles.find(video)

        assertEquals(listOf("French", "English"), subs.map { it.lang })
        assertEquals(File(folder, "Ep 1.1.French.srt").absoluteFile.toURI().toString(), subs[0].url)
        assertTrue(subs[0].url.startsWith("file:"))
    }

    @Test
    fun jsonEntryWithMissingFileSkipped() {
        val video = file("Ep 1.mp4")
        file("Ep 1.0.English.vtt")
        writeIndex(
            "Ep 1",
            SidecarEntry("Ep 1.0.English.vtt", "en", "English"),
            SidecarEntry("Ep 1.1.Gone.srt", "xx", "Gone"),
        )

        assertEquals(listOf("English"), OfflineSubtitles.find(video).map { it.lang })
    }

    @Test
    fun fallsBackToGlob() {
        val video = file("Ep 1.mp4")
        file("Ep 1.10.Ten.ass")
        file("Ep 1.2.Two.srt")
        file("Ep 1.0.Zero.vtt")
        file("Ep 1.notes.txt")
        file("Ep 1 (2).0.Other.vtt")

        assertEquals(listOf("Zero", "Two", "Ten"), OfflineSubtitles.find(video).map { it.lang })
    }

    @Test
    fun noSidecarsIsEmpty() {
        assertTrue(OfflineSubtitles.find(file("Ep 1.mp4")).isEmpty())
    }

    @Test
    fun deleteForRemovesSidecarsAndJson() {
        val video = file("Ep 1.mp4")
        file("Ep 1.0.English.vtt")
        file("Ep 1.1.French.srt")
        writeIndex("Ep 1", SidecarEntry("Ep 1.0.English.vtt", "en", "English"))
        file("Ep 1 (2).mp4")
        file("Ep 1 (2).0.English.vtt")
        writeIndex("Ep 1 (2)", SidecarEntry("Ep 1 (2).0.English.vtt", "en", "English"))

        OfflineSubtitles.deleteFor(video)

        assertFalse(File(folder, "Ep 1.0.English.vtt").exists())
        assertFalse(File(folder, "Ep 1.1.French.srt").exists())
        assertFalse(File(folder, "Ep 1.subtitles.json").exists())
        assertTrue(File(folder, "Ep 1 (2).0.English.vtt").exists())
        assertTrue(File(folder, "Ep 1 (2).subtitles.json").exists())
        assertTrue(video.exists())
    }

    @Test
    fun deleteForDoesNotTouchPrefixSiblingEpisode() {
        val video = file("Episode 12.mp4")
        file("Episode 12.0.English.vtt")
        file("Episode 12.5.mp4")
        file("Episode 12.5.0.English.vtt")

        OfflineSubtitles.deleteFor(video)

        assertFalse(File(folder, "Episode 12.0.English.vtt").exists())
        assertTrue(File(folder, "Episode 12.5.0.English.vtt").exists())
        assertEquals(1, OfflineSubtitles.find(File(folder, "Episode 12.5.mp4")).size)
        assertTrue(OfflineSubtitles.find(video).isEmpty())
    }

    @Test
    fun deleteForWithIndexDoesNotTouchPrefixSiblingEpisode() {
        val video = file("Episode 12.mp4")
        file("Episode 12.0.English.vtt")
        writeIndex("Episode 12", SidecarEntry("Episode 12.0.English.vtt", "en", "English"))
        file("Episode 12.5.mp4")
        file("Episode 12.5.0.English.vtt")
        writeIndex("Episode 12.5", SidecarEntry("Episode 12.5.0.English.vtt", "en", "English"))

        OfflineSubtitles.deleteFor(video)

        assertFalse(File(folder, "Episode 12.0.English.vtt").exists())
        assertFalse(File(folder, "Episode 12.subtitles.json").exists())
        assertTrue(File(folder, "Episode 12.5.0.English.vtt").exists())
        assertTrue(File(folder, "Episode 12.5.subtitles.json").exists())
    }

    @Test
    fun corruptIndexFallsBackToGlob() {
        val video = file("Ep 1.mp4")
        file("Ep 1.0.English.vtt")
        File(folder, "Ep 1.subtitles.json").writeText("{not json")

        assertEquals(listOf("English"), OfflineSubtitles.find(video).map { it.lang })
    }

    @Test
    fun resolveVideoFileFileSchemeAndBarePath() {
        val f = File(folder, "a b.mp4")
        assertEquals(f.absoluteFile, OfflineSubtitles.resolveVideoFile(f.absoluteFile.toURI().toString()) { null }?.absoluteFile)
        assertEquals(f, OfflineSubtitles.resolveVideoFile(f.path) { null })
        assertEquals(File("/x/y.mp4"), OfflineSubtitles.resolveVideoFile("content://media/1") { "/x/y.mp4" })
        assertEquals(null, OfflineSubtitles.resolveVideoFile("content://media/1") { null })
    }
}
