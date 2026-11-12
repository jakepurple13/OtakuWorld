package com.programmersbox.kmpuiviews.domain.aitext

object AiTextPrompts {
    const val RECAP_TAIL_CHARS = 3_000
    const val TLDR_MIN_CHARS = 400
    const val NANO_TEXT_LIMIT = 4_000
    const val MAX_LIST_TITLES = 30

    private const val MAX_RESULTS = 3
    private val markerRegex = Regex("""^\s*(?:[-•*]|\d+[.)])\s*""")

    /** System prompt for the list description, which shares [AiFeature.ListSuggest] with the names prompt. */
    const val LIST_DESCRIPTION_SYSTEM =
        "You write short descriptions for a user's custom list of titles. " +
                "Write 1–2 sentences describing what the list is about. Return only the description."

    /** The last [RECAP_TAIL_CHARS] characters of [chapter], starting at a word boundary. */
    fun recapTail(chapter: String): String {
        if (chapter.length <= RECAP_TAIL_CHARS) return chapter
        val start = chapter.length - RECAP_TAIL_CHARS
        val tail = chapter.substring(start)
        if (chapter[start - 1].isWhitespace() || tail.first().isWhitespace()) return tail.trimStart()
        val firstGap = tail.indexOfFirst { it.isWhitespace() }
        return if (firstGap < 0) tail else tail.substring(firstGap).trimStart()
    }

    fun system(feature: AiFeature, style: RewriteStyle? = null): String = when (feature) {
        AiFeature.Recap ->
            "You recap the end of a story chapter for a reader who is about to start the next one. " +
                    "Write 2–3 sentences, plain prose, no preamble."

        AiFeature.Tldr ->
            "You summarize a story description. Write exactly 3 bullets, one per line, " +
                    "each starting with \"- \". No preamble."

        AiFeature.Proofread ->
            "You fix spelling, grammar and punctuation without changing meaning or tone. " +
                    "Return only the corrected text."

        AiFeature.Rewrite -> {
            val instruction = when (style ?: RewriteStyle.Rephrase) {
                RewriteStyle.Rephrase -> "Rephrase the text using different wording but the same meaning."
                RewriteStyle.Shorten -> "Shorten the text, keeping only the key points."
                RewriteStyle.Elaborate -> "Elaborate on the text, adding detail while keeping the same meaning."
                RewriteStyle.Friendly -> "Rewrite the text in a warm, friendly tone."
                RewriteStyle.Professional -> "Rewrite the text in a polished, professional tone."
                RewriteStyle.Emojify -> "Rewrite the text and add fitting emojis."
            }
            "$instruction Return only the rewritten text."
        }

        AiFeature.ListSuggest ->
            "You name a user's custom list of titles. Suggest 3 names, one per line, max 4 words each. " +
                    "No numbering, no preamble."
    }

    fun user(feature: AiFeature, input: String): String = when (feature) {
        AiFeature.Recap -> "Chapter ending:\n$input"
        AiFeature.Tldr -> "Description:\n$input"
        AiFeature.Proofread, AiFeature.Rewrite -> input
        AiFeature.ListSuggest -> "Titles in the list:\n$input"
    }

    /** Strips "-", "•", "*", "1." markers; at most 3. */
    fun parseBullets(raw: String): List<String> = lines(raw).take(MAX_RESULTS)

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
