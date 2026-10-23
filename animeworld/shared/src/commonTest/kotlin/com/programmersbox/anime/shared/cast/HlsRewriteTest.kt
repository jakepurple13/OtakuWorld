package com.programmersbox.anime.shared.cast

import kotlin.test.Test
import kotlin.test.assertEquals

class HlsRewriteTest {
    private val base = "https://cdn.example/a/b/index.m3u8?token=1"
    private val proxy: (String) -> String = { "http://p/$it" }

    private fun rewrite(text: String) = rewriteHlsPlaylist(text, base, proxy)

    @Test
    fun rewritesRelativeSegment() {
        assertEquals(
            "#EXTINF:4.0,\nhttp://p/https://cdn.example/a/b/seg1.ts",
            rewrite("#EXTINF:4.0,\nseg1.ts"),
        )
    }

    @Test
    fun rewritesAbsoluteVariant() {
        assertEquals(
            "#EXT-X-STREAM-INF:BANDWIDTH=1000\nhttp://p/https://other/v/720.m3u8",
            rewrite("#EXT-X-STREAM-INF:BANDWIDTH=1000\nhttps://other/v/720.m3u8"),
        )
    }

    @Test
    fun rewritesKeyAndMapUriAttributes() {
        assertEquals(
            "#EXT-X-KEY:METHOD=AES-128,URI=\"http://p/https://cdn.example/a/b/key.bin\",IV=0x01\n" +
                "#EXT-X-MAP:URI=\"http://p/https://cdn.example/a/b/init.mp4\",BYTERANGE=\"720@0\"",
            rewrite(
                "#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\",IV=0x01\n" +
                    "#EXT-X-MAP:URI=\"init.mp4\",BYTERANGE=\"720@0\""
            ),
        )
    }

    @Test
    fun rewritesMediaSubtitleRendition() {
        assertEquals(
            "#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"s\",NAME=\"En\",URI=\"http://p/https://cdn.example/a/b/subs/en.m3u8\"",
            rewrite("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"s\",NAME=\"En\",URI=\"subs/en.m3u8\""),
        )
    }

    @Test
    fun leavesTagsAndCommentsUntouched() {
        val text = "#EXTM3U\n#EXT-X-VERSION:3\n# a comment\n\n#EXT-X-ENDLIST\n"
        assertEquals(text, rewrite(text))
    }

    @Test
    fun keepsQueryStringOnResolvedUrl() {
        assertEquals(
            "http://p/https://cdn.example/a/b/seg1.ts?sig=abc",
            rewrite("seg1.ts?sig=abc"),
        )
    }
}
