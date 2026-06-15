package com.programmersbox.koogintegration.customscraper

import com.programmersbox.koogintegration.customscraper.scraper.HtmlSanitizer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlSanitizerTest {

    // --- Strip tests ---

    @Test
    fun stripsStyleBlocks() {
        val html = "<html><style>body { color: red; }</style><img src='a.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<style>"), "style block should be removed")
        assertFalse(result.contains("color: red"), "style content should be removed")
    }

    @Test
    fun stripsScriptBlocksWithoutMediaUrls() {
        val html = "<html><script>var x = 1; doSomething();</script><img src='a.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<script>"), "script block should be removed")
        assertFalse(result.contains("doSomething"), "script content should be removed")
    }

    @Test
    fun preservesScriptBlocksContainingMp4Urls() {
        val html = """<html><script>var video = "https://cdn.example.com/ep1.mp4";</script></html>"""
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, ".mp4", message = "script with .mp4 URL should be kept")
    }

    @Test
    fun preservesScriptBlocksContainingM3u8Urls() {
        val html = """<html><script>var src = "https://cdn.example.com/stream.m3u8";</script></html>"""
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, ".m3u8", message = "script with .m3u8 URL should be kept")
    }

    @Test
    fun stripsNavBlock() {
        val html = "<html><nav><a href='/home'>Home</a></nav><img src='page1.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<nav>"), "nav block should be removed")
        assertFalse(result.contains("Home</a>"), "nav content should be removed")
    }

    @Test
    fun stripsFooterBlock() {
        val html = "<html><footer>Copyright 2025</footer><img src='page1.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<footer>"), "footer should be removed")
        assertFalse(result.contains("Copyright"), "footer content should be removed")
    }

    @Test
    fun stripsHeaderBlock() {
        val html = "<html><header><h1>Site Title</h1></header><img src='page1.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<header>"), "header should be removed")
        assertFalse(result.contains("Site Title"), "header content should be removed")
    }

    @Test
    fun stripsHtmlComments() {
        val html = "<html><!-- this is a comment --><img src='page1.jpg'><!-- another --></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<!--"), "HTML comments should be removed")
        assertFalse(result.contains("this is a comment"), "comment content should be removed")
    }

    // --- Preserve tests ---

    @Test
    fun preservesImgTags() {
        val html = "<html><img src='https://example.com/page1.jpg' data-src='lazy.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, "<img")
        assertContains(result, "page1.jpg")
    }

    @Test
    fun preservesVideoTags() {
        val html = "<html><video src='https://example.com/ep1.mp4'></video></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, "<video")
        assertContains(result, "ep1.mp4")
    }

    @Test
    fun preservesSourceTags() {
        val html = "<html><source src='https://example.com/stream.m3u8'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, "<source")
        assertContains(result, "stream.m3u8")
    }

    // --- Length cap tests ---

    @Test
    fun capsOutputAtMaxLength() {
        val html = "<html>" + "x".repeat(10_000) + "</html>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 8_000)
        assertTrue(result.length <= 8_000, "Output must not exceed maxLength")
    }

    @Test
    fun overflowFallbackExtractsMediaTags() {
        // 5000 chars of noise + media tags at the end → overflow triggers media-only extraction
        val noise = "<p>" + "a".repeat(5_000) + "</p>"
        val mediaTag = """<img src="https://example.com/page1.jpg">"""
        val html = "<html>$noise$mediaTag</html>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 100)
        assertContains(result, "page1.jpg", message = "media tag should survive overflow fallback")
    }

    @Test
    fun shortHtmlPassesThroughUnchanged() {
        val html = "<img src='a.jpg'>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 8_000)
        assertContains(result, "a.jpg")
    }

    @Test
    fun overflowFallbackReturnsRawHtmlWhenNoMediaTagsFound() {
        // Overflow with no media tags → should return truncated raw HTML, not empty string
        val html = "<html><p>" + "x".repeat(5_000) + "</p></html>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 100)
        assertTrue(result.isNotEmpty(), "should return truncated raw HTML, not empty string")
        assertTrue(result.length <= 100, "should still respect maxLength")
    }
}
