package com.programmersbox.kmpuiviews.presentation.dailyoverview.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.presentation.dailyoverview.SummaryState
import com.programmersbox.showcase.annotations.ShowcaseComponent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_privacy_on_device
import otakuworld.kmpuiviews.generated.resources.do_privacy_sent
import otakuworld.kmpuiviews.generated.resources.do_regenerate
import otakuworld.kmpuiviews.generated.resources.do_set_up_ai
import otakuworld.kmpuiviews.generated.resources.do_summary

/** The AI gradient used for the generating border, the spark and the shimmer tint. */
private val AiColors = listOf(Color(0xFF1F8EF1), Color(0xFF9B72F2), Color(0xFFE1617A))
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun SummaryCard(
    summary: SummaryState,
    needsSetup: Boolean,
    onRegenerate: () -> Unit,
    onSetUp: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = stringResource(Res.string.do_regenerate),
) {
    val generating = summary is SummaryState.Loading || summary is SummaryState.Streaming
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        AnimatedVisibility(
            visible = generating,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.matchParentSize()
        ) { GeneratingBorder() }

        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val template = summary is SummaryState.Done && summary.engine == SummaryEngineId.Template
                if (!template) Spark(spinning = generating)
                Text(
                    text = when (summary) {
                        SummaryState.Loading -> stringResource(Res.string.do_summary)
                        is SummaryState.Streaming -> summary.label
                        is SummaryState.Done -> summary.label
                    }.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.55.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Keyed on loading so Streaming -> Done keeps the same text; the outgoing body keeps
            // its own state while it fades.
            AnimatedContent(
                targetState = summary,
                contentKey = { it is SummaryState.Loading },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "summaryBody",
            ) { body ->
                if (body is SummaryState.Loading) {
                    Shimmer()
                } else {
                    Column {
                        StreamedText(
                            text = when (body) {
                                is SummaryState.Streaming -> body.text
                                is SummaryState.Done -> body.text
                                SummaryState.Loading -> ""
                            }
                        )
                        if (needsSetup && body is SummaryState.Done) {
                            AssistChip(
                                onClick = onSetUp,
                                label = { Text(stringResource(Res.string.do_set_up_ai)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                border = null,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = when (summary) {
                        SummaryState.Loading -> ""
                        is SummaryState.Streaming -> privacyLine(summary.onDevice, summary.destination ?: summary.label)
                        is SummaryState.Done -> privacyLine(summary.onDevice, summary.destination ?: summary.label)
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onRegenerate,
                    enabled = !generating,
                    label = { Text(actionLabel, fontSize = 11.sp) },
                )
            }
        }
    }
}

@Composable
private fun privacyLine(onDevice: Boolean, destination: String): String =
    if (onDevice) stringResource(Res.string.do_privacy_on_device)
    else stringResource(Res.string.do_privacy_sent, destination)

private class Chunk(val text: String, val alpha: Animatable<Float, *>)

/** Text that fades in chunk by chunk as the stream grows. */
@Composable
private fun StreamedText(text: String) {
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
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.75.sp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .animateContentSize()
    )
}

@Composable
private fun Shimmer() {
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
        modifier = Modifier.heightIn(min = 44.dp)
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

/** The gradient border that flows around the card while text is generating. */
@Composable
private fun GeneratingBorder() {
    val transition = rememberInfiniteTransition(label = "border")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "borderPhase",
    )
    Box(
        Modifier.drawBehind {
            val stroke = 1.5.dp.toPx()
            val shift = phase * size.width
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = AiColors + AiColors.first(),
                    start = Offset(-shift, 0f),
                    end = Offset(size.width - shift, size.width * 0.58f),
                    tileMode = TileMode.Repeated,
                ),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(24.dp.toPx() - stroke / 2),
                style = Stroke(stroke),
            )
        }.fillMaxSize()
    )
}

/** Four-point star filled with the AI gradient; spins while generating. */
@Composable
private fun Spark(spinning: Boolean) {
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
        Modifier
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

@ShowcaseComponent(
    name = "Daily Overview Summary Card",
    description = "Summary card states: loading, streaming, done, and template needing setup.",
    group = "Daily Overview"
)
@Composable
fun SummaryCardSample() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            summary = SummaryState.Loading,
            needsSetup = false,
            onRegenerate = {},
            onSetUp = {},
        )
        SummaryCard(
            summary = SummaryState.Streaming(
                text = "You finished Frieren last night with a 13-chapter run",
                label = "Gemini Nano",
                onDevice = true,
            ),
            needsSetup = false,
            onRegenerate = {},
            onSetUp = {},
        )
        SummaryCard(
            summary = SummaryState.Done(
                text = "You read 23 chapters, mostly after 7 PM, and caught up on Frieren. One Piece and Kagurabachi updated overnight.",
                label = "Google · via Koog",
                onDevice = false,
                engine = SummaryEngineId.Provider,
                destination = "Google",
            ),
            needsSetup = false,
            onRegenerate = {},
            onSetUp = {},
        )
        SummaryCard(
            summary = SummaryState.Done(
                text = "You read 23 chapters across 4 titles. 11 new chapters arrived.",
                label = "Yesterday at a glance",
                onDevice = true,
                engine = SummaryEngineId.Template,
            ),
            needsSetup = true,
            onRegenerate = {},
            onSetUp = {},
        )
    }
}
