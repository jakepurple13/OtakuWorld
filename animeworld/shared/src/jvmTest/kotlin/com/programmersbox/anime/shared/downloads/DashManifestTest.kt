package com.programmersbox.anime.shared.downloads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashManifestTest {
    private val base = "https://cdn.example/show/ep1/manifest.mpd"

    private fun mpd(body: String, attrs: String = """type="static" mediaPresentationDuration="PT10S"""") =
        """<?xml version="1.0" encoding="utf-8"?>
        <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" $attrs>$body</MPD>"""

    @Test
    fun numberTemplateCountsSegmentsFromDuration() {
        val manifest = parseDashManifest(
            mpd(
                """
                <Period>
                  <AdaptationSet contentType="video" mimeType="video/mp4">
                    <SegmentTemplate timescale="1000" duration="4000" startNumber="5"
                        initialization="init-${'$'}RepresentationID${'$'}.mp4" media="seg-${'$'}RepresentationID${'$'}-${'$'}Number${'$'}.m4s"/>
                    <Representation id="v1" bandwidth="1000" height="720"/>
                  </AdaptationSet>
                </Period>
                """,
            ),
            base,
        )
        val rep = manifest.video.single()
        assertEquals("https://cdn.example/show/ep1/init-v1.mp4", rep.initUrl)
        // 10 s / 4 s = 2.5, so 3 segments numbered from startNumber.
        assertEquals(
            listOf(5, 6, 7).map { "https://cdn.example/show/ep1/seg-v1-$it.m4s" },
            rep.segmentUrls,
        )
        assertEquals(720, rep.height)
        assertEquals("video/mp4", rep.mimeType)
    }

    @Test
    fun timelineExpandsRepeats() {
        val manifest = parseDashManifest(
            mpd(
                """
                <Period>
                  <AdaptationSet mimeType="audio/mp4">
                    <Representation id="a" bandwidth="128000">
                      <SegmentTemplate timescale="10" initialization="a/init.mp4" media="a/${'$'}Time${'$'}.m4s">
                        <SegmentTimeline>
                          <S t="100" d="20" r="2"/>
                          <S d="15"/>
                          <S t="200" d="25" r="-1"/>
                        </SegmentTimeline>
                      </SegmentTemplate>
                    </Representation>
                  </AdaptationSet>
                </Period>
                """,
                attrs = """type="static" mediaPresentationDuration="PT30S"""",
            ),
            base,
        )
        val times = manifest.audio.single().segmentUrls.map { it.substringAfterLast('/').substringBefore('.').toLong() }
        // 100,120,140 (r=2), then 160 (t continues), then t=200 repeated by 25 until the 300 (30 s × 10) end.
        assertEquals(listOf(100L, 120, 140, 160, 200, 225, 250, 275), times)
    }

    @Test
    fun nestedBaseUrlsResolve() {
        val manifest = parseDashManifest(
            mpd(
                """
                <BaseURL>https://media.example/root/</BaseURL>
                <Period>
                  <BaseURL>period/</BaseURL>
                  <AdaptationSet mimeType="video/mp4">
                    <BaseURL>video/</BaseURL>
                    <Representation id="hd" bandwidth="5" height="1080">
                      <BaseURL>hd/</BaseURL>
                      <SegmentTemplate duration="5" initialization="init.mp4" media="${'$'}Number${'$'}.m4s"/>
                    </Representation>
                    <Representation id="abs" bandwidth="4" height="720">
                      <BaseURL>https://other.example/abs/</BaseURL>
                      <SegmentTemplate duration="5" initialization="/top/init.mp4" media="${'$'}Number${'$'}.m4s"/>
                    </Representation>
                  </AdaptationSet>
                </Period>
                """,
            ),
            base,
        )
        val (hd, abs) = manifest.video
        assertEquals("https://media.example/root/period/video/hd/init.mp4", hd.initUrl)
        assertEquals(
            listOf("https://media.example/root/period/video/hd/1.m4s", "https://media.example/root/period/video/hd/2.m4s"),
            hd.segmentUrls,
        )
        assertEquals("https://other.example/top/init.mp4", abs.initUrl)
        assertEquals("https://other.example/abs/1.m4s", abs.segmentUrls.first())
    }

    @Test
    fun formatWidthIdentifier() {
        val manifest = parseDashManifest(
            mpd(
                """
                <Period duration="PT2S">
                  <AdaptationSet mimeType="video/mp4">
                    <Representation id="r" bandwidth="250000" height="360">
                      <SegmentTemplate timescale="1" duration="1" startNumber="9"
                          initialization="${'$'}Bandwidth${'$'}/init.mp4"
                          media="${'$'}Bandwidth${'$'}/chunk-${'$'}Number%05d${'$'}-${'$'}${'$'}.m4s"/>
                    </Representation>
                  </AdaptationSet>
                </Period>
                """,
            ),
            base,
        )
        val rep = manifest.video.single()
        assertEquals("https://cdn.example/show/ep1/250000/init.mp4", rep.initUrl)
        // Period@duration (2 s) wins over mediaPresentationDuration; `$$` is a literal dollar.
        assertEquals(
            listOf("chunk-00009-$.m4s", "chunk-00010-$.m4s").map { "https://cdn.example/show/ep1/250000/$it" },
            rep.segmentUrls,
        )
    }

    @Test
    fun segmentListAndSegmentBase() {
        val manifest = parseDashManifest(
            mpd(
                """
                <Period>
                  <AdaptationSet mimeType="video/mp4">
                    <Representation id="list" bandwidth="1" height="480">
                      <SegmentList duration="5">
                        <Initialization sourceURL="list/init.mp4"/>
                        <SegmentURL media="list/a.m4s"/>
                        <SegmentURL media="list/b.m4s"/>
                      </SegmentList>
                    </Representation>
                  </AdaptationSet>
                  <AdaptationSet mimeType="audio/mp4">
                    <Representation id="single" bandwidth="2">
                      <BaseURL>audio/single.mp4</BaseURL>
                      <SegmentBase indexRange="800-900"><Initialization range="0-799"/></SegmentBase>
                    </Representation>
                  </AdaptationSet>
                </Period>
                """,
            ),
            base,
        )
        val list = manifest.video.single()
        assertEquals("https://cdn.example/show/ep1/list/init.mp4", list.initUrl)
        assertEquals(listOf("https://cdn.example/show/ep1/list/a.m4s", "https://cdn.example/show/ep1/list/b.m4s"), list.segmentUrls)

        val single = manifest.audio.single()
        assertNull(single.initUrl)
        assertEquals(listOf("https://cdn.example/show/ep1/audio/single.mp4"), single.segmentUrls)
    }

    @Test
    fun dynamicManifestRejected() {
        val error = assertFailsWith<UnsupportedOperationException> {
            parseDashManifest(mpd("<Period/>", attrs = """type="dynamic" availabilityStartTime="2026-01-01T00:00:00Z""""), base)
        }
        assertEquals("Live DASH", error.message)
    }

    @Test
    fun drmRejected() {
        val error = assertFailsWith<UnsupportedOperationException> {
            parseDashManifest(
                mpd(
                    """
                    <Period>
                      <AdaptationSet mimeType="video/mp4">
                        <ContentProtection schemeIdUri="urn:mpeg:dash:mp4protection:2011" value="cenc"/>
                        <Representation id="v" bandwidth="1"><BaseURL>v.mp4</BaseURL></Representation>
                      </AdaptationSet>
                    </Period>
                    """,
                ),
                base,
            )
        }
        assertTrue("DRM" in error.message.orEmpty(), error.message)
    }

    @Test
    fun choosesBest1080AndBestAudio() {
        fun rep(id: String, bandwidth: Long, height: Int?, mime: String = "video/mp4") =
            DashRepresentation(id, mime, bandwidth, height, null, listOf("$id.mp4"))

        val manifest = DashManifest(
            video = listOf(rep("480", 1_000, 480), rep("1080", 5_000, 1080), rep("1080lo", 3_000, 1080), rep("2160", 20_000, 2160)),
            audio = listOf(rep("a64", 64, null, "audio/mp4"), rep("a128", 128, null, "audio/mp4")),
            muxed = emptyList(),
        )
        assertEquals("1080" to "a128", manifest.choose().let { (v, a) -> v.id to a?.id })
        assertEquals("480", manifest.choose(maxHeight = 720).first.id)

        // Nothing fits under maxHeight: the lowest height wins.
        val tall = manifest.copy(video = listOf(rep("2160", 20_000, 2160), rep("1440", 9_000, 1440)))
        assertEquals("1440", tall.choose().first.id)

        // Muxed-only manifests have no separate audio.
        val muxed = DashManifest(video = emptyList(), audio = emptyList(), muxed = listOf(rep("m", 1, 720)))
        assertEquals("m" to null, muxed.choose().let { (v, a) -> v.id to a?.id })
    }
}
