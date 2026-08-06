package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent
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

@ShowcaseComponent(
    name = "Bounce Click",
    description = "A modifier that adds a bounce effect to a clickable element.",
    group = "Modifiers"
)
@Composable
fun BounceClickSample() {
    Box(
        modifier = Modifier
            .size(50.dp)
            .bounceClick(.9f)
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
    )
}