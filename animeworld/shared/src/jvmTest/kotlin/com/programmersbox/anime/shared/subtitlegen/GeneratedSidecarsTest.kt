package com.programmersbox.anime.shared.subtitlegen

import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.downloads.SidecarEntry
import com.programmersbox.anime.shared.downloads.sidecarIndexName
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratedSidecarsTest {
    private val dir = Files.createTempDirectory("gensidecars").toFile()
    private val video = File(dir, "Frieren - 07.mkv").also { it.writeText("v") }
    private val english = WhisperLanguage("en", "English")
    private val japanese = WhisperLanguage("ja", "Japanese")
    private val indexFile get() = File(dir, sidecarIndexName("Frieren - 07"))

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    private fun vtt(name: String = "tmp.vtt") = File(dir, name).also { it.writeText("WEBVTT") }
    private fun labels() = OfflineSubtitles.find(video).map { it.lang }

    @Test
    fun addsTrackAndIndex() {
        val src = vtt()
        GeneratedSidecars.add(video, src, english)
        assertEquals(listOf("English (generated)"), labels())
        assertFalse(src.exists())
    }

    @Test
    fun keepsGlobSidecarsWhenNoIndex() {
        File(dir, "Frieren - 07.0.English.vtt").writeText("WEBVTT")
        File(dir, "Frieren - 07.1.Spanish.vtt").writeText("WEBVTT")
        GeneratedSidecars.add(video, vtt(), japanese)
        assertEquals(listOf("English", "Spanish", "Japanese (generated)"), labels())
    }

    @Test
    fun sameLanguageReplacesGeneratedOnly() {
        File(dir, "Frieren - 07.0.English.vtt").writeText("WEBVTT")
        val old = GeneratedSidecars.add(video, vtt("a.vtt"), english)
        assertEquals(2, Json.decodeFromString<List<SidecarEntry>>(indexFile.readText()).size)
        val again = GeneratedSidecars.add(video, vtt("b.vtt"), english)
        assertEquals(listOf("English", "English (generated)"), labels())
        assertFalse(old.exists())
        assertTrue(again.exists())
        assertTrue(File(dir, "Frieren - 07.0.English.vtt").exists())
    }

    @Test
    fun differentLanguageAdds() {
        GeneratedSidecars.add(video, vtt("a.vtt"), english)
        GeneratedSidecars.add(video, vtt("b.vtt"), japanese)
        assertEquals(listOf("English (generated)", "Japanese (generated)"), labels())
    }

    @Test
    fun oldIndexWithoutGeneratedFieldStillReads() {
        File(dir, "Frieren - 07.0.English.vtt").writeText("WEBVTT")
        indexFile.writeText("""[{"file":"Frieren - 07.0.English.vtt","lang":"en","label":"English"}]""")
        assertEquals(listOf("English"), labels())
        assertFalse(OfflineSubtitles.readIndex(indexFile)!!.single().generated)
    }

    @Test
    fun bracketedNameWithGlobSidecarKeepsItAndIndexesNext() {
        val v = File(dir, "[SubsPlease] Frieren - 07 (1080p).mkv").also { it.writeText("v") }
        File(dir, "[SubsPlease] Frieren - 07 (1080p).0.English.vtt").writeText("WEBVTT")
        val target = GeneratedSidecars.add(v, vtt(), japanese)
        assertEquals(listOf("English", "Japanese (generated)"), OfflineSubtitles.find(v).map { it.lang })
        assertTrue(target.name.startsWith("[SubsPlease] Frieren - 07 (1080p).1."), target.name)
    }

    @Test
    fun nonAsciiNameWorks() {
        val v = File(dir, "葬送のフリーレン 07.mkv").also { it.writeText("v") }
        val target = GeneratedSidecars.add(v, vtt(), english)
        assertTrue(target.isFile)
        assertEquals(listOf("English (generated)"), OfflineSubtitles.find(v).map { it.lang })
    }
}
