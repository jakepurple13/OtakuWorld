package com.programmersbox.koogintegration.customscraper.scraper

object HtmlSanitizer {

    // Script blocks are kept only if they contain one of these media URL patterns.
    private val mediaUrlPatterns = listOf(".mp4", ".m3u8", ".jpg", ".jpeg", ".png", ".webp", ".gif")

    // Matches open tags for media-relevant elements. <a> is included because
    // some sites link directly to image/video files via href attributes.
    private val mediaTagRegex = Regex(
        """<(?:img|video|source|a)\b[^>]*>""",
        RegexOption.IGNORE_CASE
    )

    private val commentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val styleRegex = Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL)
    private val scriptRegex = Regex("<script[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
    private val navRegex = Regex("<nav[^>]*>.*?</nav>", RegexOption.DOT_MATCHES_ALL)
    private val footerRegex = Regex("<footer[^>]*>.*?</footer>", RegexOption.DOT_MATCHES_ALL)
    private val headerRegex = Regex("<header[^>]*>.*?</header>", RegexOption.DOT_MATCHES_ALL)

    fun sanitize(html: String, maxLength: Int = 8_000): String {
        // Hard cap input before any regex runs — prevents ReDoS on malformed/huge HTML
        // (e.g., unclosed <nav> in a 500KB page would cause catastrophic backtracking)
        val cappedHtml = if (html.length > 200_000) html.substring(0, 200_000) else html
        var result = cappedHtml

        // Strip HTML comments first so comment-wrapped tags don't confuse later patterns.
        result = result.replace(commentRegex, "")

        // Strip <style> blocks entirely.
        result = result.replace(styleRegex, "")

        // Strip <script> blocks unless the content contains a media URL pattern.
        result = result.replace(scriptRegex) { match ->
            val content = match.groupValues[1]
            if (mediaUrlPatterns.any { content.contains(it, ignoreCase = true) }) match.value else ""
        }

        // Strip structural navigation/layout blocks — these never contain content media.
        result = result.replace(navRegex, "")
        result = result.replace(footerRegex, "")
        result = result.replace(headerRegex, "")

        if (result.length <= maxLength) return result

        // Overflow: concatenate only the preserved media tags so the LLM gets
        // the highest-value content within the character budget.
        val mediaTags = mediaTagRegex.findAll(result)
            .map { it.value }
            .joinToString("\n")

        // If no media tags found, fall back to raw truncated HTML rather than returning empty.
        return mediaTags.ifEmpty { result.take(maxLength) }
    }
}
