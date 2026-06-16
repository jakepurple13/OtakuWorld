package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedBlock(
    val original: String,
    val translated: String,
    val bounds: BoundingBox,
)

@Serializable
data class TranslationResult(
    val blocks: List<TranslatedBlock>,
)
