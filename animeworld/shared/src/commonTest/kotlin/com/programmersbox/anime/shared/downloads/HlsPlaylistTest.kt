package com.programmersbox.anime.shared.downloads

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HlsPlaylistTest {
    private val base = "https://cdn.example.com/a/b/master.m3u8?token=1#frag"

    @Test
    fun parsesMasterVariants() {
        val text = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,CODECS="avc1.64001f,mp4a.40.2"
            low/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720
            /hi/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=5000000
            https://other.example.com/x.m3u8
        """.trimIndent()
        val master = assertIs<HlsPlaylist.Master>(parseHlsPlaylist(text, base))
        assertEquals(3, master.variants.size)
        assertEquals(HlsVariant("https://cdn.example.com/a/b/low/index.m3u8", 800000, 640, 360), master.variants[0])
        assertEquals(HlsVariant("https://cdn.example.com/hi/index.m3u8", 2800000, 1280, 720), master.variants[1])
        assertEquals(HlsVariant("https://other.example.com/x.m3u8", 5000000, null, null), master.variants[2])
    }

    @Test
    fun parsesMediaWithRelativeUris() {
        val text = """
            #EXTM3U
            #EXT-X-MEDIA-SEQUENCE:7
            #EXTINF:4.0,
            seg0.ts
            #EXTINF:4.0,
            seg1.ts
            #EXT-X-ENDLIST
        """.trimIndent()
        val media = assertIs<HlsPlaylist.Media>(parseHlsPlaylist(text, base))
        assertEquals(7L, media.mediaSequence)
        assertEquals(
            listOf("https://cdn.example.com/a/b/seg0.ts", "https://cdn.example.com/a/b/seg1.ts"),
            media.segments.map { it.uri }
        )
        assertEquals(listOf(7L, 8L), media.segments.map { it.sequence })
        assertNull(media.map)
    }

    @Test
    fun keyCarriesUntilReplaced() {
        val text = """
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="k1.key",IV=0x00000000000000000000000000000010
            #EXTINF:4,
            a.ts
            #EXTINF:4,
            b.ts
            #EXT-X-KEY:METHOD=AES-128,URI="k2.key"
            #EXTINF:4,
            c.ts
        """.trimIndent()
        val media = assertIs<HlsPlaylist.Media>(parseHlsPlaylist(text, base))
        val k1 = media.segments[0].key!!
        assertEquals("AES-128", k1.method)
        assertEquals("https://cdn.example.com/a/b/k1.key", k1.uri)
        assertContentEquals(ByteArray(15) + byteArrayOf(0x10), k1.iv)
        assertEquals(k1, media.segments[1].key)
        val k2 = media.segments[2].key!!
        assertEquals("https://cdn.example.com/a/b/k2.key", k2.uri)
        assertNull(k2.iv)
    }

    @Test
    fun parsesFmp4Map() {
        val text = """
            #EXTM3U
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4,
            s.m4s
        """.trimIndent()
        val media = assertIs<HlsPlaylist.Media>(parseHlsPlaylist(text, base))
        assertEquals(HlsMap("https://cdn.example.com/a/b/init.mp4"), media.map)
    }

    @Test
    fun methodNoneClearsKey() {
        val text = """
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="k.key"
            #EXTINF:4,
            a.ts
            #EXT-X-KEY:METHOD=NONE
            #EXTINF:4,
            b.ts
        """.trimIndent()
        val media = assertIs<HlsPlaylist.Media>(parseHlsPlaylist(text, base))
        assertEquals("AES-128", media.segments[0].key?.method)
        assertNull(media.segments[1].key)
    }

    @Test
    fun chooseVariantByHeightThenBandwidth() {
        val master = HlsPlaylist.Master(
            listOf(
                HlsVariant("a", 1000, 480, 360),
                HlsVariant("b", 3000, 1280, 720),
                HlsVariant("c", 6000, 1920, 1080),
            )
        )
        assertEquals("b", master.chooseVariant("BestBob - 720p (1280x720)").uri)
        assertEquals("c", master.chooseVariant("1920x1080").uri)
        assertEquals("c", master.chooseVariant(null).uri)
        assertEquals("c", master.chooseVariant("unknown").uri)
    }

    @Test
    fun ivFromSequence() {
        val iv = aesIv(HlsSegment("u", HlsKey("AES-128", "k", null), 5))
        assertContentEquals(ByteArray(15) + byteArrayOf(5), iv)
        val explicit = ByteArray(16) { 1 }
        assertContentEquals(explicit, aesIv(HlsSegment("u", HlsKey("AES-128", "k", explicit), 5)))
    }
}
