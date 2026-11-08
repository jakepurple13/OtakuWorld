package com.programmersbox.kmpuiviews.translation

import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.BuiltInLanguages
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories
import com.programmersbox.kmpuiviews.domain.TranslationLanguages

interface LanguageDetector {
    /** ISO 639-1 code, or null when unsure. */
    fun detect(text: String): String?
}

class LibraryLanguageDetector : LanguageDetector {
    private val detector by lazy {
        val reader = LanguageProfileReader()
        // Every target-picker language Optimaize ships a profile for (all but eo and ka).
        // zh-CN and zh-TW both report language "zh".
        val codes = TranslationLanguages.all.map { it.code }.toSet()
        val profiles = BuiltInLanguages.getLanguages()
            .filter { it.language in codes }
            .map { reader.readBuiltIn(it) }
        LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(profiles)
            .build()
    }
    private val textFactory by lazy { CommonTextObjectFactories.forDetectingShortCleanText() }

    override fun detect(text: String): String? {
        if (text.count { it.isLetter() } < MIN_LETTERS) return null
        return pickLanguage(
            detector.getProbabilities(textFactory.forText(text))
                .map { it.locale.toString() to it.probability }
        )
    }

    private companion object {
        /** Shorter text is too ambiguous to guess; leave it untranslated. */
        const val MIN_LETTERS = 12
    }
}

private const val MIN_PROBABILITY = 0.75

/**
 * Sums probabilities per language (zh-CN + zh-TW both count as "zh") and returns the best
 * language, or null when even the summed best is under [floor].
 */
fun pickLanguage(probabilities: List<Pair<String, Double>>, floor: Double = MIN_PROBABILITY): String? {
    val best = probabilities
        .groupBy({ it.first.substringBefore('-').substringBefore('_') }, { it.second })
        .mapValues { it.value.sum() }
        .maxByOrNull { it.value }
        ?: return null
    return best.key.takeIf { best.value >= floor && it.isNotBlank() }
}
