package com.programmersbox.kmpuiviews.domain.moodtags

import org.jetbrains.compose.resources.StringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.mood_action_packed
import otakuworld.kmpuiviews.generated.resources.mood_bittersweet
import otakuworld.kmpuiviews.generated.resources.mood_chaotic
import otakuworld.kmpuiviews.generated.resources.mood_cozy
import otakuworld.kmpuiviews.generated.resources.mood_dark
import otakuworld.kmpuiviews.generated.resources.mood_epic
import otakuworld.kmpuiviews.generated.resources.mood_found_family
import otakuworld.kmpuiviews.generated.resources.mood_funny
import otakuworld.kmpuiviews.generated.resources.mood_heartwarming
import otakuworld.kmpuiviews.generated.resources.mood_high_stakes
import otakuworld.kmpuiviews.generated.resources.mood_inspiring
import otakuworld.kmpuiviews.generated.resources.mood_lighthearted
import otakuworld.kmpuiviews.generated.resources.mood_melancholy
import otakuworld.kmpuiviews.generated.resources.mood_mysterious
import otakuworld.kmpuiviews.generated.resources.mood_romantic
import otakuworld.kmpuiviews.generated.resources.mood_slow_burn
import otakuworld.kmpuiviews.generated.resources.mood_spooky
import otakuworld.kmpuiviews.generated.resources.mood_tense
import otakuworld.kmpuiviews.generated.resources.mood_thoughtful
import otakuworld.kmpuiviews.generated.resources.mood_wholesome

/**
 * The 20 built-in moods. A mood is stored as its name: one of these ids, or a name the user made.
 * Ids never change; labels come from strings.
 */
object Moods {
    private val labels: Map<String, StringResource> = mapOf(
        "cozy" to Res.string.mood_cozy,
        "wholesome" to Res.string.mood_wholesome,
        "heartwarming" to Res.string.mood_heartwarming,
        "funny" to Res.string.mood_funny,
        "lighthearted" to Res.string.mood_lighthearted,
        "romantic" to Res.string.mood_romantic,
        "bittersweet" to Res.string.mood_bittersweet,
        "melancholy" to Res.string.mood_melancholy,
        "dark" to Res.string.mood_dark,
        "tense" to Res.string.mood_tense,
        "mysterious" to Res.string.mood_mysterious,
        "spooky" to Res.string.mood_spooky,
        "high stakes" to Res.string.mood_high_stakes,
        "action packed" to Res.string.mood_action_packed,
        "epic" to Res.string.mood_epic,
        "found family" to Res.string.mood_found_family,
        "slow burn" to Res.string.mood_slow_burn,
        "thoughtful" to Res.string.mood_thoughtful,
        "inspiring" to Res.string.mood_inspiring,
        "chaotic" to Res.string.mood_chaotic,
    )

    val builtIn: List<String> = labels.keys.toList()

    private const val MAX_CUSTOM_LENGTH = 30
    private val spaces = Regex("\\s+")

    fun isBuiltIn(name: String): Boolean = name.lowercase() in labels

    /** The label for a built-in mood, or null for the user's own (shown as typed). */
    fun label(name: String): StringResource? = labels[name.lowercase()]

    /** Trimmed, single-spaced and capped at 30 characters; null when nothing is left. */
    fun cleanCustom(raw: String): String? =
        raw.trim().replace(spaces, " ").take(MAX_CUSTOM_LENGTH).trim().takeIf { it.isNotEmpty() }
}
