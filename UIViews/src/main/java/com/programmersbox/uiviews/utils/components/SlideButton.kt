package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class SwipeState { Swiped, Unswiped }

/**
 * Taken and modified from [Barros9](https://github.com/Barros9/ComposeFunctions/blob/main/app/src/main/java/com/barros/composefunctions/ui/composable/SwipeButton.kt)
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeButton(
    swipeableState: SwipeableState<SwipeState>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    borderStroke: BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground),
    elevation: Dp = 8.dp,
    iconGraphicsLayer: GraphicsLayerScope.() -> Unit = {},
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            modifier = Modifier.size(56.dp),
            contentDescription = null
        )
    },
    text: @Composable BoxScope.() -> Unit = {},
    onSwipe: () -> Unit
) {
    if (swipeableState.isAnimationRunning) {
        DisposableEffect(Unit) {
            onDispose {
                if (swipeableState.currentValue == SwipeState.Swiped) {
                    onSwipe()
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        border = borderStroke,
        tonalElevation = elevation
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            var iconSize by remember { mutableStateOf(IntSize.Zero) }
            val maxWidth = with(LocalDensity.current) {
                this@BoxWithConstraints.maxWidth.toPx() - iconSize.width
            }

            ProvideTextStyle(MaterialTheme.typography.bodyLarge) { text() }
            Box(
                modifier = Modifier
                    .onGloballyPositioned { iconSize = it.size }
                    .swipeable(
                        state = swipeableState,
                        anchors = mapOf(
                            0f to SwipeState.Unswiped,
                            maxWidth to SwipeState.Swiped
                        ),
                        thresholds = { _, _ -> FractionalThreshold(0.9f) },
                        orientation = Orientation.Horizontal
                    )
                    .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
            ) {
                Box(modifier = Modifier.graphicsLayer(iconGraphicsLayer)) { icon() }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun SwipeButtonPreview() {
    MaterialTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SwipeButton(
                    swipeableState = rememberSwipeableState(initialValue = SwipeState.Unswiped),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            modifier = Modifier.size(56.dp),
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = TextAlign.End,
                            text = "Swipe",
                        )
                    },
                    onSwipe = {}
                )

                SwipeButton(
                    swipeableState = rememberSwipeableState(initialValue = SwipeState.Swiped),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            modifier = Modifier.size(56.dp),
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = TextAlign.End,
                            text = "Swipe",
                        )
                    },
                    onSwipe = {}
                )
            }
        }
    }
}