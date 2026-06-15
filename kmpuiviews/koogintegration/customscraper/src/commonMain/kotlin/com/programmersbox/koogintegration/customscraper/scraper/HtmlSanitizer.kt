package com.programmersbox.koogintegration.customscraper.scraper

object HtmlSanitizer {

    // Script blocks are kept only if they contain one of these media URL patterns.
    private val mediaUrlPatterns = listOf(".mp4", ".m3u8", ".jpg", ".jpeg", ".png", ".webp", ".gif")

    // Matches self-closing or open tags for the four media-relevant elements.
    private val mediaTagRegex = Regex(
        """<(?:img|video|source|a)\b[^>]*>""",
        RegexOption.IGNORE_CASE
    )

    fun sanitize(html: String, maxLength: Int = 8_000): String {
        var result = html

        // Strip HTML comments first so comment-wrapped tags don't confuse later patterns.
        result = result.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        // Strip <style> blocks entirely.
        result = result.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")

        // Strip <script> blocks unless the content contains a media URL pattern.
        result = result.replace(Regex("<script[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)) { match ->
            val content = match.groupValues[1]
            if (mediaUrlPatterns.any { content.contains(it, ignoreCase = true) }) match.value else ""
        }

        // Strip structural navigation/layout blocks — these never contain content media.
        result = result.replace(Regex("<nav[^>]*>.*?</nav>", RegexOption.DOT_MATCHES_ALL), "")
        result = result.replace(Regex("<footer[^>]*>.*?</footer>", RegexOption.DOT_MATCHES_ALL), "")
        result = result.replace(Regex("<header[^>]*>.*?</header>", RegexOption.DOT_MATCHES_ALL), "")

        if (result.length <= maxLength) return result

        // Overflow: concatenate only the preserved media tags so the LLM gets
        // the highest-value content within the character budget.
        val mediaTags = mediaTagRegex.findAll(result)
            .map { it.value }
            .joinToString("\n")

        return mediaTags.take(maxLength)
    }
}
