package com.programmersbox.anime.shared.videoplayer

import com.programmersbox.anime.shared.cast.VttCue
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.downloads.SidecarEntry
import com.programmersbox.anime.shared.downloads.sidecarIndexName
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopSubtitlesTest {
    private fun sub(lang: String) = KmpSubtitle(url = "https://subs.example/${lang.hashCode()}.vtt", lang = lang)

    @Test
    fun prefersConfiguredLanguage() {
        val (tracks, default) = buildSubtitleTracks(
            external = listOf(sub("English"), sub("Spanish")),
            embedded = emptyList(),
            style = SubtitleStyle(preferredLanguages = listOf("es")),
            deviceLanguage = "en",
        )
        assertEquals("Spanish", tracks[default!!].label)
    }

    @Test
    fun skipsSignsAndSongs() {
        val (tracks, default) = buildSubtitleTracks(
            external = listOf(sub("English - Signs\\/Songs"), sub("English")),
            embedded = emptyList(),
            style = SubtitleStyle(),
            deviceLanguage = "en",
        )
        // Labels go through cleanSubtitleLabel.
        assertEquals("English - Signs/Songs", tracks[0].label)
        assertEquals(1, default)
    }

    @Test
    fun autoEnableOffMeansNoDefault() {
        val (tracks, default) = buildSubtitleTracks(
            external = listOf(sub("English")),
            embedded = listOf(SpuTrack(1, "English", "eng")),
            style = SubtitleStyle(autoEnable = false),
            deviceLanguage = "en",
        )
        assertEquals(2, tracks.size)
        assertNull(default)
    }

    @Test
    fun embeddedUsedWhenNoExternalMatches() {
        val (tracks, default) = buildSubtitleTracks(
            external = listOf(sub("French")),
            embedded = listOf(SpuTrack(3, "Track 1", "jpn"), SpuTrack(4, "Track 2", "spa")),
            style = SubtitleStyle(preferredLanguages = listOf("es")),
            deviceLanguage = "de",
        )
        assertEquals(
            listOf(
                SubtitleTrack.External(sub("French"), "French"),
                SubtitleTrack.Embedded(3, "Track 1"),
                SubtitleTrack.Embedded(4, "Track 2"),
            ),
            tracks,
        )
        assertEquals(SubtitleTrack.Embedded(4, "Track 2"), tracks[default!!])
    }

    @Test
    fun cueLoaderSendsHeadersAndConvertsSrt() = runTest {
        MockWebServer().use { server ->
            server.dispatcher = object : mockwebserver3.Dispatcher() {
                override fun dispatch(request: mockwebserver3.RecordedRequest): MockResponse =
                    if (request.headers["X-Key"] == "k" && request.headers["Referer"] == "https://ref.example/") {
                        MockResponse.Builder().code(200).body(SRT).build()
                    } else {
                        MockResponse.Builder().code(403).build()
                    }
            }
            server.start()
            val subtitle = KmpSubtitle(server.url("/subs/ep1.srt").toString(), "English", mapOf("X-Key" to "k"))

            val cues = SubtitleCueLoader(OkHttpClient()).load(subtitle, referer = "https://ref.example/")

            assertEquals(listOf(VttCue(1_000, 2_500, "Hello"), VttCue(3_000, 4_000, "World")), cues)
        }
    }

    @Test
    fun cueLoaderReadsLocalSidecar() = runTest {
        val dir = Files.createTempDirectory("sidecar").toFile()
        try {
            val video = dir.resolve("Show - Episode 1.mp4").apply { writeText("") }
            dir.resolve("Show - Episode 1.0.English.srt").writeText(SRT)
            dir.resolve(sidecarIndexName("Show - Episode 1")).writeText(
                Json.encodeToString(listOf(SidecarEntry(file = "Show - Episode 1.0.English.srt", lang = "en", label = "English")))
            )

            val subtitles = OfflineSubtitles.find(video)
            assertEquals(listOf("English"), subtitles.map { it.lang })

            val cues = SubtitleCueLoader(OkHttpClient()).load(subtitles.single(), referer = null)
            assertEquals(listOf(VttCue(1_000, 2_500, "Hello"), VttCue(3_000, 4_000, "World")), cues)
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        val SRT = """
            1
            00:00:01,000 --> 00:00:02,500
            Hello

            2
            00:00:03,000 --> 00:00:04,000
            World
        """.trimIndent() + "\n"
    }
}
