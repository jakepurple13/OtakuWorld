package com.programmersbox.koogintegration.dynamictranslation.model

data class DynamicTranslationOutput(
    val imageBytes: ByteArray,
    val translations: List<TranslatedBlock>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DynamicTranslationOutput) return false
        return imageBytes.contentEquals(other.imageBytes) && translations == other.translations
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + translations.hashCode()
        return result
    }
}
