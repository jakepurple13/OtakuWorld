package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.launch

// 1. The Extension Function
fun Modifier.bounceClick(scaleAmount: Float = 0.7f) = this then BounceClickElement(scaleAmount)

// 2. The ModifierNodeElement
private data class BounceClickElement(
    val scaleAmount: Float,
) : ModifierNodeElement<BounceClickNode>() {

    override fun create(): BounceClickNode {
        return BounceClickNode(scaleAmount)
    }

    override fun update(node: BounceClickNode) {
        node.scaleAmount = scaleAmount
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bounceClick"
        properties["scaleAmount"] = scaleAmount
    }
}

// 3. The Modifier.Node implementation
private class BounceClickNode(
    var scaleAmount: Float,
) : DelegatingNode(), DrawModifierNode {

    // Animatable replaces animateFloatAsState since we are outside @Composable
    private val scale = Animatable(1f)

    // Delegate to the built-in SuspendingPointerInputModifierNode
    private val pointerInput = delegate(
        SuspendingPointerInputModifierNode {
            awaitEachGesture {
                // Wait for the finger to touch down
                awaitFirstDown(requireUnconsumed = false)

                // Launch the press animation using the Node's built-in coroutineScope
                coroutineScope.launch {
                    scale.animateTo(scaleAmount)
                }

                // Wait for the finger to lift or the gesture to be cancelled
                waitForUpOrCancellation()

                // Launch the release animation
                coroutineScope.launch {
                    scale.animateTo(1f)
                }
            }
        }
    )

    // DrawModifierNode allows us to alter the drawing phase
    override fun ContentDrawScope.draw() {
        // Read the Animatable value
        val currentScale = scale.value

        // Scale the canvas, then draw the content inside it
        scale(currentScale) {
            this@draw.drawContent()
        }
    }
}