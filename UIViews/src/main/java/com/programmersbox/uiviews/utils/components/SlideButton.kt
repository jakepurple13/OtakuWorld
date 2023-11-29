package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SwipeState { Swiped, Unswiped }

/**
 * Taken and modified from [Barros9](https://github.com/Barros9/ComposeFunctions/blob/main/app/src/main/java/com/barros/composefunctions/ui/composable/SwipeButton.kt)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeButton(
    swipeableState: AnchoredDraggableState<SwipeState>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    borderStroke: BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground),
    elevation: Dp = 8.dp,
    iconGraphicsLayer: GraphicsLayerScope.() -> Unit = {},
    icon: @Composable () -> Unit = {
        @Suppress("DEPRECATION")
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            modifier = Modifier.size(56.dp),
            contentDescription = null
        )
    },
    text: @Composable BoxScope.() -> Unit = {},
    onSwipe: () -> Unit,
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
            SideEffect {
                swipeableState.updateAnchors(
                    DraggableAnchors {
                        SwipeState.Unswiped at 0f
                        SwipeState.Swiped at maxWidth
                    }
                )
            }
            Box(
                modifier = Modifier
                    .onGloballyPositioned { iconSize = it.size }
                    .anchoredDraggable(
                        state = swipeableState,
                        orientation = Orientation.Horizontal
                    )
                    .offset { IntOffset(swipeableState.offset.roundToInt(), 0) }
            ) {
                Box(modifier = Modifier.graphicsLayer(iconGraphicsLayer)) { icon() }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@LightAndDarkPreviews
@Composable
fun SwipeButtonPreview() {
    PreviewTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val scope = rememberCoroutineScope()
                val one = remember {
                    AnchoredDraggableState(
                        initialValue = SwipeState.Unswiped,
                        positionalThreshold = { it },
                        velocityThreshold = { .9f },
                        animationSpec = spring(),
                    )
                }
                SwipeButton(
                    swipeableState = one,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    icon = {
                        @Suppress("DEPRECATION")
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

                val two = remember {
                    AnchoredDraggableState(
                        initialValue = SwipeState.Swiped,
                        positionalThreshold = { it },
                        velocityThreshold = { .9f },
                        animationSpec = spring(),
                    )
                }
                SwipeButton(
                    swipeableState = two,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    icon = {
                        @Suppress("DEPRECATION")
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
                Button(
                    onClick = {
                        scope.launch {
                            one.animateTo(SwipeState.Unswiped)
                            two.animateTo(SwipeState.Unswiped)
                        }
                    }
                ) { Text("Reset") }
            }
        }
    }
}