package com.programmersbox.kmpuiviews.translation

import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.i18n.LdLocale
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories

interface LanguageDetector {
    /** ISO 639-1 code, or null when unsure. */
    fun detect(text: String): String?
}

class LibraryLanguageDetector : LanguageDetector {
    private val detector by lazy {
        val reader = LanguageProfileReader()
        val profiles = SUPPORTED.map { reader.readBuiltIn(LdLocale.fromString(it)) }
        LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(profiles)
            .build()
    }
    private val textFactory by lazy { CommonTextObjectFactories.forDetectingShortCleanText() }

    override fun detect(text: String): String? {
        if (text.isBlank()) return null
        val result = detector.getProbabilities(textFactory.forText(text))
            .maxByOrNull { it.probability }
            ?: return null
        return result.locale.language.takeIf { it.isNotBlank() }
    }

    private companion object {
        val SUPPORTED = listOf("ja", "ko", "zh-CN", "zh-TW", "es", "fr", "de", "en", "ru")
    }
}
