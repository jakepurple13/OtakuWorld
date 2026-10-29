package com.programmersbox.kmpuiviews.utils.composables.sharedelements

import com.programmersbox.kmpmodels.KmpItemModel

/**
 * Identifies one item across screens for shared element transitions.
 * Keyed by the item url + source so the same title from two sources stays distinct.
 */
data class SharedItemKey(
    val url: String,
    val source: String,
)

fun KmpItemModel.sharedItemKey() = SharedItemKey(url = url, source = source.serviceName)

data class OtakuSharedElement(
    val item: SharedItemKey,
    val type: OtakuSharedElementType,
)

fun OtakuImageElement(item: SharedItemKey) = OtakuSharedElement(item, OtakuSharedElementType.Image)

fun OtakuTitleElement(item: SharedItemKey) = OtakuSharedElement(item, OtakuSharedElementType.Title)

enum class OtakuSharedElementType {
    Image,
    Title
}
