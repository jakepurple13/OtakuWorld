package com.programmersbox.kmpuiviews.utils.composables.sharedelements

data class OtakuSharedElement(
    val source: String,
    val origin: String,
    val type: OtakuSharedElementType,
)

fun OtakuImageElement(
    source: String,
    origin: String,
) = OtakuSharedElement(source, origin, OtakuSharedElementType.Image)

fun OtakuTitleElement(
    source: String,
    origin: String,
) = OtakuSharedElement(source, origin, OtakuSharedElementType.Title)

enum class OtakuSharedElementType {
    Image,
    Title
}