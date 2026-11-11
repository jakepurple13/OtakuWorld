package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_notif_chapters
import otakuworld.kmpuiviews.generated.resources.do_notif_episodes
import otakuworld.kmpuiviews.generated.resources.do_notif_title
import otakuworld.kmpuiviews.generated.resources.do_notif_updates

/** "Yesterday: 23 chapters, 11 updates". Both numbers come straight from [facts]. */
suspend fun morningNotificationTitle(facts: DailyFacts): String {
    val read = getPluralString(
        when (facts.unit) {
            UpdateUnit.Chapters -> Res.plurals.do_notif_chapters
            UpdateUnit.Episodes -> Res.plurals.do_notif_episodes
        },
        facts.readCount,
        facts.readCount,
    )
    val updates = getPluralString(Res.plurals.do_notif_updates, facts.updates.size, facts.updates.size)
    return getString(Res.string.do_notif_title, read, updates)
}

private val sentenceEnd = Regex("""[.!?](?=\s|$)""")

/** The text up to and including the first sentence end, or all of it when there is none. */
fun firstSentence(text: String): String {
    val trimmed = text.trim()
    val end = sentenceEnd.find(trimmed) ?: return trimmed
    return trimmed.substring(0, end.range.last + 1)
}
