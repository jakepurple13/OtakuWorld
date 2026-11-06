package com.programmersbox.kmpuiviews.translation

fun translationSystemPrompt(sourceName: String, targetName: String): String =
    "You are a translation engine. Translate the user's text from $sourceName to $targetName. " +
            "Reply with only the translation, no notes, no quotes."

/** Trims and strips one pair of surrounding quotes. */
fun cleanLlmReply(reply: String): String {
    val t = reply.trim()
    return if (t.length >= 2 && t.first() == '"' && t.last() == '"') t.substring(1, t.length - 1).trim() else t
}
