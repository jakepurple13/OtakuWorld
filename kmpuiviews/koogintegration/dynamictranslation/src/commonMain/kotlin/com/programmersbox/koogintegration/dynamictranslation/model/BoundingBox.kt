package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
