package com.programmersbox.kmpuiviews.domain.aitext

object AiTextPrompts {
    const val RECAP_TAIL_CHARS = 3_000
    const val TLDR_MIN_CHARS = 200
    const val NANO_TEXT_LIMIT = 4_000
    const val MAX_LIST_TITLES = 30

    private const val MAX_RESULTS = 3
    private val markerRegex = Regex("""^\s*(?:[-•*]|\d+[.)])\s*""")

    /** The last [RECAP_TAIL_CHARS] characters of [chapter], starting at a word boundary. */
    fun recapTail(chapter: String): String {
        if (chapter.length <= RECAP_TAIL_CHARS) return chapter
        val start = chapter.length - RECAP_TAIL_CHARS
        val tail = chapter.substring(start)
        if (chapter[start - 1].isWhitespace() || tail.first().isWhitespace()) return tail.trimStart()
        val firstGap = tail.indexOfFirst { it.isWhitespace() }
        return if (firstGap < 0) tail else tail.substring(firstGap).trimStart()
    }

    /** The first [NANO_TEXT_LIMIT] characters of [description], ending at a word boundary. */
    fun tldrHead(description: String): String {
        if (description.length <= NANO_TEXT_LIMIT) return description
        val head = description.substring(0, NANO_TEXT_LIMIT)
        if (description[NANO_TEXT_LIMIT].isWhitespace() || head.last().isWhitespace()) return head.trimEnd()
        val lastGap = head.indexOfLast { it.isWhitespace() }
        return if (lastGap < 0) head else head.substring(0, lastGap).trimEnd()
    }

    /**
     * Strips "-", "•", "*", "1." markers; at most 3. A heading line such as "Here's your summary:"
     * (small models often add one) is dropped so it doesn't take a bullet's place.
     */
    fun parseBullets(raw: String): List<String> = lines(raw)
        .filterNot { it.trimEnd('*', ' ').endsWith(':') }
        .take(MAX_RESULTS)

    /** Strips markers and quotes, trims, drops blanks and case-insensitive duplicates; at most 3. */
    fun parseNames(raw: String): List<String> = lines(raw)
        .map { it.trim('"', '\'', '“', '”', '`').trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .take(MAX_RESULTS)

    private fun lines(raw: String): List<String> = raw.lines()
        .map { markerRegex.replace(it, "").trim() }
        .filter { it.isNotBlank() }
}
