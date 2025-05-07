package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun rememberScaleRotateOffset(
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    initialOffset: Offset = Offset.Zero,
) = remember { ScaleRotateOffset(initialScale, initialRotation, initialOffset) }

class ScaleRotateOffset(initialScale: Float = 1f, initialRotation: Float = 0f, initialOffset: Offset = Offset.Zero) {
    val scale: MutableState<Float> = mutableFloatStateOf(initialScale)
    val rotation: MutableState<Float> = mutableFloatStateOf(initialRotation)
    val offset: MutableState<Offset> = mutableStateOf(initialOffset)
}

fun Modifier.scaleRotateOffset(
    scaleRotateOffset: ScaleRotateOffset,
    canScale: Boolean = true,
    canRotate: Boolean = true,
    canOffset: Boolean = true,
): Modifier = composed {
    scaleRotateOffset(
        scaleRotateOffset.scale,
        scaleRotateOffset.rotation,
        scaleRotateOffset.offset,
        canScale,
        canRotate,
        canOffset
    )
}

@Composable
fun Modifier.scaleRotateOffset(
    scale: MutableState<Float> = remember { mutableFloatStateOf(1f) },
    rotation: MutableState<Float> = remember { mutableFloatStateOf(0f) },
    offset: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) },
    canScale: Boolean = true,
    canRotate: Boolean = true,
    canOffset: Boolean = true,
): Modifier {
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        if (canScale) scale.value *= zoomChange
        if (canRotate) rotation.value += rotationChange
        if (canOffset) offset.value += offsetChange
    }
    val animScale = animateFloatAsState(scale.value, label = "").value
    val (x, y) = animateOffsetAsState(offset.value, label = "").value
    return this
        .graphicsLayer(
            scaleX = animScale,
            scaleY = animScale,
            rotationZ = animateFloatAsState(rotation.value, label = "").value,
            translationX = x,
            translationY = y
        )
        // add transformable to listen to multitouch transformation events after offset
        .transformable(state = state)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.scaleRotateOffsetReset(
    canScale: Boolean = true,
    canRotate: Boolean = true,
    canOffset: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
): Modifier = this.composed {
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        if (canScale) scale *= zoomChange
        if (canRotate) rotation += rotationChange
        if (canOffset) offset += offsetChange
    }
    val animScale = animateFloatAsState(scale, label = "").value
    val (x, y) = animateOffsetAsState(offset, label = "").value
    graphicsLayer(
        scaleX = animScale,
        scaleY = animScale,
        rotationZ = animateFloatAsState(rotation, label = "").value,
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
            interactionSource = null
        )
}

