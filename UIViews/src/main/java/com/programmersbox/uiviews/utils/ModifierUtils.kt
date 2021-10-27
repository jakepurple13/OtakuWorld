package com.programmersbox.uiviews.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

enum class ComponentState { Pressed, Released }

@Composable
fun Modifier.combineClickableWithIndication(
    onLongPress: (ComponentState) -> Unit = {},
    onClick: () -> Unit = {}
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }

    return indication(
        interactionSource = interactionSource,
        indication = rememberRipple()
    )
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { onLongPress(ComponentState.Pressed) },
                onPress = {
                    val press = PressInteraction.Press(it)
                    interactionSource.tryEmit(press)
                    tryAwaitRelease()
                    onLongPress(ComponentState.Released)
                    interactionSource.tryEmit(PressInteraction.Release(press))
                },
                onTap = { onClick() }
            )
        }
}

fun Modifier.fadeInAnimation(): Modifier = composed {
    val animatedProgress = remember { Animatable(initialValue = 0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(600)
        )
    }
    alpha(animatedProgress.value)
}

@Composable
fun rememberScaleRotateOffset(
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    initialOffset: Offset = Offset.Zero
) = remember { ScaleRotateOffset(initialScale, initialRotation, initialOffset) }

class ScaleRotateOffset(initialScale: Float = 1f, initialRotation: Float = 0f, initialOffset: Offset = Offset.Zero) {
    val scale: MutableState<Float> = mutableStateOf(initialScale)
    val rotation: MutableState<Float> = mutableStateOf(initialRotation)
    val offset: MutableState<Offset> = mutableStateOf(initialOffset)
}

@Composable
fun Modifier.scaleRotateOffset(
    scaleRotateOffset: ScaleRotateOffset,
    canScale: Boolean = true,
    canRotate: Boolean = true,
    canOffset: Boolean = true
): Modifier = scaleRotateOffset(
    scaleRotateOffset.scale,
    scaleRotateOffset.rotation,
    scaleRotateOffset.offset,
    canScale,
    canRotate,
    canOffset
)

@Composable
fun Modifier.scaleRotateOffset(
    scale: MutableState<Float> = remember { mutableStateOf(1f) },
    rotation: MutableState<Float> = remember { mutableStateOf(0f) },
    offset: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) },
    canScale: Boolean = true,
    canRotate: Boolean = true,
    canOffset: Boolean = true
): Modifier {
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        if (canScale) scale.value *= zoomChange
        if (canRotate) rotation.value += rotationChange
        if (canOffset) offset.value += offsetChange
    }
    val animScale = animateFloatAsState(scale.value).value
    val (x, y) = animateOffsetAsState(offset.value).value
    return graphicsLayer(
        scaleX = animScale,
        scaleY = animScale,
        rotationZ = animateFloatAsState(rotation.value).value,
        translationX = x,
        translationY = y
    )
        // add transformable to listen to multitouch transformation events after offset
        .transformable(state = state)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.scaleRotateOffsetReset(
    canScale: Boolean = true,
    canRotate: Boolean = true,
    canOffset: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
): Modifier {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        if (canScale) scale *= zoomChange
        if (canRotate) rotation += rotationChange
        if (canOffset) offset += offsetChange
    }
    val animScale = animateFloatAsState(scale).value
    val (x, y) = animateOffsetAsState(offset).value
    return graphicsLayer(
        scaleX = animScale,
        scaleY = animScale,
        rotationZ = animateFloatAsState(rotation).value,
        translationX = x,
        translationY = y
    )
        // add transformable to listen to multitouch transformation events
        // after offset
        .transformable(state = state)
        .combinedClickable(
            onClick = onClick,
            onDoubleClick = {
                if (canScale) scale = 1f
                if (canRotate) rotation = 0f
                if (canOffset) offset = Offset.Zero
            },
            onLongClick = onLongClick,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
}