package com.programmersbox.kmpuiviews.presentation.onboarding.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private const val SweepMillis = 700

/**
 * Sweeps a theme change in from a position in ROOT coordinates (e.g. `positionInRoot()` of a tapped
 * node): call `sweepFrom(offset) { applyChange() }`. The current frame is captured first and
 * `applyChange` runs only after the capture, so the frozen frame always shows the old theme.
 * A step reads this so it works wherever the [ColorSweepHost] sits; the provider converts to host
 * coordinates. `null` means no host is provided above the step: apply the change directly.
 */
val LocalColorSweep = staticCompositionLocalOf<((Offset, () -> Unit) -> Unit)?> { null }

/**
 * Sweep state. [start] resets [progress] to 0 in the same snapshot that sets [origin] and the frozen
 * [frame], so the first frame of every sweep draws the old frame in full. [id] counts sweeps.
 * Only the draw phase reads these, so a sweep never recomposes the host's content.
 */
internal class ColorSweepState {
    var origin by mutableStateOf<Offset?>(null)
        private set
    var frame by mutableStateOf<ImageBitmap?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
    var id by mutableIntStateOf(0)
        private set

    /** Set while waiting for the next draw to record the current frame into the layer. */
    internal var captureRequest by mutableStateOf<CompletableDeferred<Unit>?>(null)

    fun start(offset: Offset, frame: ImageBitmap) {
        progress = 0f
        this.frame = frame
        origin = offset
        id++
    }

    fun finish() {
        origin = null
        frame = null
    }
}

/**
 * Hosts [content] and lets it start a theme sweep: call `sweepFrom(offset) { applyChange() }`. The
 * previous frame is captured as a bitmap and stays on top, with a circular hole growing from the
 * offset that reveals the new theme underneath over 700 ms. Under reduced motion the change is
 * applied immediately, with no capture.
 */
@Composable
fun ColorSweepHost(content: @Composable (sweepFrom: (Offset, () -> Unit) -> Unit) -> Unit) {
    ColorSweepHost(remember { ColorSweepState() }, content)
}

@Composable
internal fun ColorSweepHost(
    sweep: ColorSweepState,
    content: @Composable (sweepFrom: (Offset, () -> Unit) -> Unit) -> Unit,
) {
    val reduced by rememberUpdatedState(onboardingReducedMotion())
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    // Stable for the host's lifetime, so providing it as a composition local never recomposes readers.
    val sweepFrom: (Offset, () -> Unit) -> Unit = remember(sweep, layer, scope) {
        var job: Job? = null
        { offset, apply ->
            job?.cancel()
            sweep.finish()
            if (reduced) {
                apply()
            } else {
                job = scope.launch {
                    var applied = false
                    try {
                        val recorded = CompletableDeferred<Unit>()
                        val frame = try {
                            sweep.captureRequest = recorded
                            recorded.await()
                            layer.toImageBitmap()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null // Capture unsupported here: just apply the change.
                        } finally {
                            if (sweep.captureRequest === recorded) sweep.captureRequest = null
                        }
                        if (frame != null) sweep.start(offset, frame)
                        applied = true
                        apply()
                        if (frame != null) {
                            animate(0f, 1f, animationSpec = tween(SweepMillis, easing = LinearEasing)) { value, _ ->
                                sweep.progress = value
                            }
                        }
                    } finally {
                        // A newer tap cancelled this one before it applied: still apply, in tap order.
                        if (!applied) apply()
                        sweep.finish()
                    }
                }
            }
        }
    }

    Box(
        Modifier.drawWithContent {
            val request = sweep.captureRequest
            val origin = sweep.origin
            val frame = sweep.frame
            when {
                request != null -> {
                    // Record once, only when a sweep asks for it; the layer is then rasterised to a bitmap.
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                    request.complete(Unit)
                }

                origin != null && frame != null -> {
                    drawContent()
                    val farthest = max(
                        max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
                        max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
                    )
                    val hole = Path().apply { addOval(Rect(origin, farthest * sweep.progress)) }
                    clipPath(hole, ClipOp.Difference) { drawImage(frame) }
                }

                else -> drawContent()
            }
        },
    ) {
        content(sweepFrom)
    }
}
