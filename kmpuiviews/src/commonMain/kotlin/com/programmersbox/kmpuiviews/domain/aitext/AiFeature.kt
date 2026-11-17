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

    data object Recap : AiFeature<String>(
        system = "You recap the end of a story chapter for a reader who is about to start the next one. " +
                "Write 2–3 sentences, plain prose, no preamble.",
    ) {
        override fun prepare(input: String) = AiTextPrompts.recapTail(input)
        override fun userPrompt(prepared: String) = "Chapter ending:\n$prepared"
    }

    data object Tldr : AiFeature<String>(
        system = "You summarize a story description. Write exactly 3 bullets, one per line, " +
                "each starting with \"- \". No preamble.",
        nanoTask = NanoTask.Summarize,
    ) {
        override fun prepare(input: String) = AiTextPrompts.tldrHead(input)
        override fun userPrompt(prepared: String) = "Description:\n$prepared"
    }

    data object Proofread : AiFeature<String>(
        system = "You fix spelling, grammar and punctuation without changing meaning or tone. " +
                "Return only the corrected text.",
        nanoTask = NanoTask.Proofread,
        nanoMaxChars = AiTextPrompts.NANO_TEXT_LIMIT,
    ) {
        override fun prepare(input: String) = input
    }

    data class Rewrite(val style: RewriteStyle) : AiFeature<String>(
        system = "${rewriteInstruction(style)} Return only the rewritten text.",
        nanoTask = NanoTask.Rewrite(style),
        nanoMaxChars = AiTextPrompts.NANO_TEXT_LIMIT,
    ) {
        override fun prepare(input: String) = input
    }

    data object ListNames : AiFeature<List<String>>(
        system = "You name a user's custom list of titles. Suggest 3 names, one per line, max 4 words each. " +
                "No numbering, no preamble.",
    ) {
        override fun prepare(input: List<String>) = listTitles(input)
        override fun userPrompt(prepared: String) = listPrompt(prepared)
        override fun finish(raw: String) = AiTextPrompts.parseNames(raw).joinToString("\n")
    }

    data object ListDescription : AiFeature<List<String>>(
        system = "You write short descriptions for a user's custom list of titles. " +
                "Write 1–2 sentences describing what the list is about. Return only the description.",
    ) {
        override fun prepare(input: List<String>) = listTitles(input)
        override fun userPrompt(prepared: String) = listPrompt(prepared)
    }
}

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
