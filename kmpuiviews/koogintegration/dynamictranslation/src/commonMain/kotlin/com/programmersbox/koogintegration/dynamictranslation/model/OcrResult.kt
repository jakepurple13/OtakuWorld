package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class OcrBlock(
    val text: String,
    val bounds: BoundingBox,
)

@Serializable
data class OcrResult(
    val blocks: List<OcrBlock>,
)
