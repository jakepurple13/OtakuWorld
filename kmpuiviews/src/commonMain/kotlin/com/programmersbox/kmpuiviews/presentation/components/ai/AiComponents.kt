package com.programmersbox.kmpuiviews.presentation.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.kmpuiviews.domain.aitext.AiEngineInfo
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.showcase.annotations.ShowcaseComponent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_error
import otakuworld.kmpuiviews.generated.resources.ai_retry
import otakuworld.kmpuiviews.generated.resources.do_privacy_on_device
import otakuworld.kmpuiviews.generated.resources.do_privacy_sent

/** The AI gradient used for the generating border, the spark and the shimmer tint. */
private val AiColors = listOf(Color(0xFF1F8EF1), Color(0xFF9B72F2), Color(0xFFE1617A))
private val AiCardShape = RoundedCornerShape(24.dp)

/** The gradient border that flows around the card while text is generating. Fades in and out over 400 ms. */
fun Modifier.generatingBorder(active: Boolean, shape: Shape): Modifier = composed {
    val alpha by animateFloatAsState(if (active) 1f else 0f, tween(400), label = "borderAlpha")
    // Not drawn, and no infinite transition running, once the fade-out has finished.
    if (!active && alpha == 0f) return@composed Modifier

    val transition = rememberInfiniteTransition(label = "border")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "borderPhase",
    )
    Modifier.drawBehind {
        val stroke = 1.5.dp.toPx()
        val shift = phase * size.width
        val inner = Size(size.width - stroke, size.height - stroke)
        translate(stroke / 2, stroke / 2) {
            drawOutline(
                outline = shape.createOutline(inner, layoutDirection, this),
                brush = Brush.linearGradient(
                    colors = AiColors + AiColors.first(),
                    start = Offset(-shift, 0f),
                    end = Offset(size.width - shift, size.width * 0.58f),
                    tileMode = TileMode.Repeated,
                ),
                alpha = alpha,
                style = Stroke(stroke),
            )
        }
    }
}

/** Four-point star filled with the AI gradient; spins while generating. */
@Composable
fun Spark(spinning: Boolean, modifier: Modifier = Modifier) {
    // Read in the draw layer so spinning doesn't recompose.
    val rotation: () -> Float = if (spinning) {
        val transition = rememberInfiniteTransition(label = "spark")
        val angle = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "sparkAngle",
        );
        { angle.value }
    } else {
        { 0f }
    }
    Canvas(
        modifier
            .size(14.dp)
            .graphicsLayer { rotationZ = rotation() }
    ) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.62f, h * 0.38f)
            lineTo(w, h * 0.5f)
            lineTo(w * 0.62f, h * 0.62f)
            lineTo(w * 0.5f, h)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(0f, h * 0.5f)
            lineTo(w * 0.38f, h * 0.38f)
            close()
        }
        drawPath(path, Brush.linearGradient(AiColors, start = Offset.Zero, end = Offset(w, h)))
    }
}

@Composable
fun AiShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerPhase",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = lerp(base, AiColors[1], 0.25f)
    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        listOf(1f, 0.86f, 0.6f).forEach { width ->
            Spacer(
                Modifier
                    .fillMaxWidth(width)
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .drawBehind {
                        // A highlight band twice the line width sweeps right to left.
                        val band = size.width * 2
                        val start = size.width - phase * band
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(base, highlight, base),
                                start = Offset(start, 0f),
                                end = Offset(start + band, 0f),
                                tileMode = TileMode.Repeated,
                            )
                        )
                    }
            )
        }
    }
}

private class Chunk(val text: String, val alpha: Animatable<Float, *>)

/** Text that fades in chunk by chunk as the stream grows. */
@Composable
fun StreamedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.75.sp),
    minHeight: Dp = 44.dp,
) {
    val chunks = remember { mutableStateListOf<Chunk>() }
    // Fades run in their own scope so a newer chunk doesn't cancel an older one mid-fade.
    val fadeScope = rememberCoroutineScope()
    LaunchedEffect(text) {
        val shown = chunks.joinToString("") { it.text }
        if (text == shown) return@LaunchedEffect
        // Anything that isn't a continuation (regenerate, template fallback) starts over.
        if (!text.startsWith(shown)) chunks.clear()
        val added = Chunk(text.removePrefix(chunks.joinToString("") { it.text }), Animatable(0f))
        chunks += added
        fadeScope.launch { added.alpha.animateTo(1f, tween(350)) }
    }

    val color = MaterialTheme.colorScheme.onSurface
    Text(
        text = buildAnnotatedString {
            chunks.forEach { chunk ->
                withStyle(SpanStyle(color = color.copy(alpha = chunk.alpha.value))) { append(chunk.text) }
            }
        },
        style = style,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .animateContentSize()
    )
}

/**
 * Card for AI-generated text: spark and [label] on top, [body] for the text, then the engine and
 * privacy lines. Loading shows a shimmer, Error shows the message with a retry button.
 */
@Composable
fun AiCard(
    label: String,
    state: AiTextState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    headerAction: (@Composable () -> Unit)? = null,
    footerPrefix: String? = null,
    body: @Composable (String) -> Unit = { StreamedText(it) },
) {
    val generating = state is AiTextState.Loading || state is AiTextState.Streaming
    Column(
        modifier = modifier
            .clip(AiCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .generatingBorder(generating, AiCardShape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Spark(spinning = generating)
            Crossfade(
                targetState = label,
                animationSpec = tween(200),
                label = "aiCardLabel",
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = it.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.55.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            headerAction?.invoke()
        }

        // Keyed on the kind so Streaming -> Done keeps the same text; the outgoing body keeps its
        // own state while it fades.
        AnimatedContent(
            targetState = state,
            contentKey = {
                when (it) {
                    is AiTextState.Streaming, is AiTextState.Done -> "text"
                    else -> it::class
                }
            },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "aiCardBody",
        ) { current ->
            when (current) {
                AiTextState.Idle, AiTextState.Loading -> AiShimmer()
                is AiTextState.Streaming -> body(current.text)
                is AiTextState.Done -> body(current.text)
                is AiTextState.Error -> Column {
                    Text(
                        text = current.message.ifBlank { stringResource(Res.string.ai_error) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.ai_retry)) }
                }
            }
        }

        val engine = when (state) {
            is AiTextState.Streaming -> state.engine
            is AiTextState.Done -> state.engine
            else -> null
        }
        if (engine != null) {
            Column(Modifier.padding(top = 10.dp)) {
                Text(
                    text = listOfNotNull(footerPrefix, engine.label).joinToString(" · "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (engine.onDevice) stringResource(Res.string.do_privacy_on_device)
                    else stringResource(Res.string.do_privacy_sent, engine.destination ?: engine.label),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ShowcaseComponent(
    name = "AI Card",
    description = "Shared AI text card states: loading, streaming, done, and error.",
    group = "AI"
)
@Composable
fun AiCardSample() {
    val onDevice = AiEngineInfo(label = "Gemini Nano", onDevice = true, destination = null)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AiCard(label = "TL;DR", state = AiTextState.Loading, onRetry = {})
        AiCard(
            label = "TL;DR",
            state = AiTextState.Streaming("A swordswoman outlives her party and sets out to understand", onDevice),
            onRetry = {},
        )
        AiCard(
            label = "TL;DR",
            state = AiTextState.Done("A swordswoman outlives her party and learns what they meant to her.", onDevice),
            footerPrefix = "Translated",
            onRetry = {},
        )
        AiCard(label = "TL;DR", state = AiTextState.Error("The model stopped responding"), onRetry = {})
    }
}
