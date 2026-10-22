package com.programmersbox.kmpuiviews.presentation.tips

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

enum class TipGesture(val icon: ImageVector) {
    Hold(Icons.Outlined.BackHand),
    Tap(Icons.Default.TouchApp),
    Swipe(Icons.Default.SwipeRight),
    Drag(Icons.Default.SwipeVertical),
}

/**
 * A hidden gesture worth teaching. A [demo] adds a "Try" button that opens a practice view;
 * the demo calls its `onDone` once the user has done the gesture.
 */
class Tip(
    val title: String,
    val description: String,
    val gesture: TipGesture,
    val demo: (@Composable (onDone: () -> Unit) -> Unit)? = null,
)

class TipsScope {
    internal val tips = mutableListOf<Tip>()

    fun tip(
        title: String,
        description: String,
        gesture: TipGesture,
        demo: (@Composable (onDone: () -> Unit) -> Unit)? = null,
    ) {
        tips += Tip(title, description, gesture, demo)
    }
}

fun buildTips(block: TipsScope.() -> Unit): List<Tip> = TipsScope().apply(block).tips.toList()

/** Tips that apply to every app. */
val sharedTips: List<Tip> = buildTips {
    tip(
        title = "Hold a cover",
        description = "Opens quick options: favorite, add to lists or share. Works on Recent, All, Search, Favorites and Lists.",
        gesture = TipGesture.Hold,
        demo = { HoldCoverDemo(it) },
    )
    tip(
        title = "Swipe a chapter",
        description = "Runs your chapter swipe actions on the details screen.",
        gesture = TipGesture.Swipe,
        demo = { SwipeChapterDemo(it) },
    )
    tip(
        title = "Hold a description",
        description = "Translates it.",
        gesture = TipGesture.Hold,
    )
    tip(
        title = "Hold a title",
        description = "Copies it.",
        gesture = TipGesture.Hold,
    )
    tip(
        title = "Hold a source chip",
        description = "Shows favorites from only that source.",
        gesture = TipGesture.Hold,
    )
    tip(
        title = "Hold an item in a list",
        description = "Selects it so you can act on several items at once.",
        gesture = TipGesture.Hold,
    )
    tip(
        title = "Swipe history or notifications",
        description = "Swipe a row to delete it. Swipe a saved notification right to set a reminder.",
        gesture = TipGesture.Swipe,
    )
}
