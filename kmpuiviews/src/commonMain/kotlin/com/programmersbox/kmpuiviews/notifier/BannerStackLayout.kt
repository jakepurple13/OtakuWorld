package com.programmersbox.kmpuiviews.notifier

import com.programmersbox.sharedcomponents.notifier.BannerDefaults

data class CardSlot(
    val translationY: Float,
    val scale: Float,
    val alpha: Float,
    val contentAlpha: Float,
    val zIndex: Float,
    val interactive: Boolean,
)

/** Pure layout math for the banner deck. Index 0 is the newest (top) card. */
fun bannerSlots(
    heightsPx: List<Float>,
    expanded: Boolean,
    peekPx: Float,
    gapPx: Float,
): List<CardSlot> {
    var offset = 0f
    return heightsPx.mapIndexed { index, height ->
        val zIndex = (heightsPx.size - index).toFloat()
        val slot = if (expanded) {
            CardSlot(
                translationY = offset,
                scale = 1f,
                alpha = 1f,
                contentAlpha = 1f,
                zIndex = zIndex,
                interactive = true,
            )
        } else {
            val visible = index < BannerDefaults.MaxVisible
            CardSlot(
                translationY = index * peekPx,
                scale = 1f - 0.05f * index,
                alpha = if (visible) 1f else 0f,
                contentAlpha = if (index == 0) 1f else 0f,
                zIndex = zIndex,
                interactive = visible,
            )
        }
        offset += height + gapPx
        slot
    }
}
