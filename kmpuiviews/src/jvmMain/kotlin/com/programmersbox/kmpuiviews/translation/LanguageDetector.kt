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
        val result = detector.getProbabilities(textFactory.forText(text))
            .maxByOrNull { it.probability }
            ?: return null
        if (result.probability < MIN_PROBABILITY) return null
        return result.locale.language.takeIf { it.isNotBlank() }
    }

    private companion object {
        /** Shorter text is too ambiguous to guess; leave it untranslated. */
        const val MIN_LETTERS = 12
        const val MIN_PROBABILITY = 0.75
    }
}
