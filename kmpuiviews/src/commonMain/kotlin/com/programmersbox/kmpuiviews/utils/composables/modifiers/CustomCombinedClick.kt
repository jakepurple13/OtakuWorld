package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.programmersbox.kmpuiviews.utils.ComponentState

fun Modifier.combineClickableWithIndication(
    onLongPress: (ComponentState) -> Unit = {},
    onClick: () -> Unit = {},
    onDoubleTap: (() -> Unit)? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onClick,
        onLongClick = { onLongPress(ComponentState.Pressed) },
        onDoubleClick = onDoubleTap
    )
    //TODO: If I go with making this an option, this will be in it's own function with an if statement
    /*.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { onLongPress(ComponentState.Pressed) },
            onPress = {
                val press = PressInteraction.Press(it)
                interactionSource.tryEmit(press)
                tryAwaitRelease()
                onLongPress(ComponentState.Released)
                interactionSource.tryEmit(PressInteraction.Release(press))
            },
            onTap = onClick?.let { c -> { c() } },
            onDoubleTap = onDoubleTap?.let { d -> { d() } }
        )
    }*/
}