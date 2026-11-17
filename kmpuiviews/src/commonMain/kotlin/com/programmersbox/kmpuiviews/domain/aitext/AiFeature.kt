package com.programmersbox.kmpuiviews.domain.aitext

/**
 * A task-specific on-device API (ML Kit GenAI) that can stand in for the Prompt API when Nano is
 * the engine. Only some features have one.
 */
sealed interface NanoTask {
    data object Summarize : NanoTask
    data object Proofread : NanoTask
    data class Rewrite(val style: RewriteStyle) : NanoTask
}

/**
 * Every on-device AI text feature, with everything needed to run it. To add a feature, add an
 * entry here and run it with [AiText.run] or [AiTask.start].
 *
 * @param system the system prompt
 * @param nanoTask the ML Kit API used instead of the Prompt API when Nano is picked and the API is ready
 * @param nanoMaxChars input longer than this (after [prepare]) skips Nano; null means no cap
 */
sealed class AiFeature<in I>(
    val system: String,
    val nanoTask: NanoTask? = null,
    val nanoMaxChars: Int? = null,
) {
    /** The text actually sent to the engine. Its length is the input length used to pick an engine. */
    abstract fun prepare(input: I): String

    /** The user prompt for [prepared] text. */
    open fun userPrompt(prepared: String): String = prepared

    /** Cleans up the finished text. Applied only once generation is done, never to streamed text. */
    open fun finish(raw: String): String = raw

    /** The AI settings switch for this feature. Null is only gated by the master switch. */
    abstract val toggle: AiToggle?

    data object Recap : AiFeature<String>(
        system = "You recap the end of a story chapter for a reader who is about to start the next one. " +
                "Write 2–3 sentences, plain prose, no preamble.",
    ) {
        // The reader shows the recap bar only when its "Chapter recap" switch (AiToggle.ChapterRecap) is on,
        // so the engine itself isn't gated by it; otherwise the reader could never offer the switch again.
        override val toggle: AiToggle? = null
        override fun prepare(input: String) = AiTextPrompts.recapTail(input)
        override fun userPrompt(prepared: String) = "Chapter ending:\n$prepared"
    }

    data object Tldr : AiFeature<String>(
        system = "You summarize a story description. Write exactly 3 bullets, one per line, " +
                "each starting with \"- \". No preamble.",
        nanoTask = NanoTask.Summarize,
    ) {
        override val toggle = AiToggle.Tldr
        override fun prepare(input: String) = AiTextPrompts.tldrHead(input)
        override fun userPrompt(prepared: String) = "Description:\n$prepared"
    }

    data object Proofread : AiFeature<String>(
        system = "You fix spelling, grammar and punctuation without changing meaning or tone. " +
                "Return only the corrected text.",
        nanoTask = NanoTask.Proofread,
        nanoMaxChars = AiTextPrompts.NANO_TEXT_LIMIT,
    ) {
        override val toggle = AiToggle.WritingTools
        override fun prepare(input: String) = input
    }

    data class Rewrite(val style: RewriteStyle) : AiFeature<String>(
        system = "${rewriteInstruction(style)} Return only the rewritten text.",
        nanoTask = NanoTask.Rewrite(style),
        nanoMaxChars = AiTextPrompts.NANO_TEXT_LIMIT,
    ) {
        override val toggle = AiToggle.WritingTools
        override fun prepare(input: String) = input
    }

    data object ListNames : AiFeature<List<String>>(
        system = "You name a user's custom list of titles. Suggest 3 names, one per line, max 4 words each. " +
                "No numbering, no preamble.",
    ) {
        override val toggle = AiToggle.ListSuggestions
        override fun prepare(input: List<String>) = listTitles(input)
        override fun userPrompt(prepared: String) = listPrompt(prepared)
        override fun finish(raw: String) = AiTextPrompts.parseNames(raw).joinToString("\n")
    }

    data object ListDescription : AiFeature<List<String>>(
        system = "You write short descriptions for a user's custom list of titles. " +
                "Write 1–2 sentences describing what the list is about. Return only the description.",
    ) {
        override val toggle = AiToggle.ListSuggestions
        override fun prepare(input: List<String>) = listTitles(input)
        override fun userPrompt(prepared: String) = listPrompt(prepared)
    }

    /** The AI settings page's "Try it": a TL;DR of a fixed sample, gated only by the master switch. */
    data object TrySample : AiFeature<String>(system = Tldr.system) {
        override val toggle: AiToggle? = null
        override fun prepare(input: String) = Tldr.prepare(input)
        override fun userPrompt(prepared: String) = Tldr.userPrompt(prepared)
    }

    /** A short summary of the notifications from the latest update check, shown atop Notifications. */
    data object UpdateSummary : AiFeature<List<String>>(
        system = "You summarize new chapter and episode updates for a reader. In 2 short sentences, say which " +
                "titles got the most new chapters or episodes, and anything notable in their names. Use only what's " +
                "given; don't invent plot. Plain prose, no lists, no preamble.",
    ) {
        override val toggle = AiToggle.UpdateSummary
        override fun prepare(input: List<String>) = input.take(MAX_UPDATE_LINES).joinToString("\n")
        override fun userPrompt(prepared: String) = "New updates:\n$prepared"
    }

    /** Turns a description like "that isekai where the guy is a slime" into favorites search keywords. */
    data object SearchKeywords : AiFeature<String>(
        system = "You turn a reader's description of a manga, anime or novel into search keywords. Return 3 to " +
                "6 short keywords or title words likely to appear in its title or description, one per line, " +
                "lowercase. No numbering, no explanations.",
    ) {
        override val toggle = AiToggle.DescribeSearch
        override fun prepare(input: String) = input.trim().take(MAX_DESCRIBE_CHARS)
        override fun userPrompt(prepared: String) = "Description: $prepared"
        // Small models often ignore "one per line": accept commas or semicolons, and drop preambles
        // ("Here are the keywords:") and anything without a searchable word.
        override fun finish(raw: String) = raw.lines()
            .filterNot { it.trim().endsWith(':') }
            .map { it.replace(keywordsLabel, "") }
            .flatMap { it.split(',', ';') }
            .map { it.replace(listMarker, "").trim('"', '\'', '“', '”', '`', ' ').lowercase() }
            .filter { searchableWord.containsMatchIn(it) }
            .distinct()
            .take(MAX_KEYWORDS)
            .joinToString("\n")
    }

    /** Suggests up to 3 moods for a favorite from the built-in moods plus the user's own. */
    data object MoodTags : AiFeature<MoodInput>(
        system = "You tag a manga, anime or novel with moods. Pick 1 to 3 moods from the given list that " +
                "best fit it, one per line, written exactly as in the list. No other words.",
    ) {
        override val toggle = AiToggle.MoodTags
        override fun prepare(input: MoodInput) =
            "Title: ${input.title}\nDescription: ${input.description.take(MAX_MOOD_DESCRIPTION_CHARS)}\n" +
                    "Moods to choose from: ${input.choices.joinToString(", ")}"

        // The caller keeps only names from the list; this just splits and tidies
        override fun finish(raw: String) = raw.lines()
            .flatMap { it.split(',', ';') }
            .map { it.replace(listMarker, "").trim('"', '\'', '“', '”', '`', ' ').lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
    }

    /** Explains an update-check failure that has no fixed explanation. */
    data object ExplainError : AiFeature<ErrorDetails>(
        system = "You explain why an app couldn't check a manga, anime or novel source for new chapters. " +
                "The reader is not technical. In 2 sentences, say what most likely went wrong and one thing " +
                "they can try. Don't mention code, exceptions or stack traces. Return only the explanation.",
    ) {
        override val toggle = AiToggle.ErrorExplanations
        override fun prepare(input: ErrorDetails) =
            "Source: ${input.sourceName}\nError:\n${input.detail.take(MAX_ERROR_DETAIL_CHARS)}"
    }
}

/** A favorite to tag: its title, description and the moods the AI may pick from. */
data class MoodInput(val title: String, val description: String, val choices: List<String>)

/** A failed update check: the source's display name and the error detail (message and top stack lines). */
data class ErrorDetails(val sourceName: String, val detail: String)

private const val MAX_ERROR_DETAIL_CHARS = 1_500
private const val MAX_UPDATE_LINES = 25
private const val MAX_DESCRIBE_CHARS = 300
private const val MAX_KEYWORDS = 6
private const val MAX_MOOD_DESCRIPTION_CHARS = 1_500
private val listMarker = Regex("""^\s*(?:[-•*]|\d+[.)])\s*""")
private val keywordsLabel = Regex("""^\s*keywords?\s*:\s*""", RegexOption.IGNORE_CASE)
// Letters, digits and combining marks, the same runs FTS5 keeps (see ftsMatch)
private val searchableWord = Regex("""[\p{L}\p{N}\p{M}]""")

private fun rewriteInstruction(style: RewriteStyle) = when (style) {
    RewriteStyle.Rephrase -> "Rephrase the text using different wording but the same meaning."
    RewriteStyle.Shorten -> "Shorten the text, keeping only the key points."
    RewriteStyle.Elaborate -> "Elaborate on the text, adding detail while keeping the same meaning."
    RewriteStyle.Friendly -> "Rewrite the text in a warm, friendly tone."
    RewriteStyle.Professional -> "Rewrite the text in a polished, professional tone."
    RewriteStyle.Emojify -> "Rewrite the text and add fitting emojis."
}

private fun listTitles(titles: List<String>) = titles.take(AiTextPrompts.MAX_LIST_TITLES).joinToString("\n")

private fun listPrompt(prepared: String) = "Titles in the list:\n$prepared"
