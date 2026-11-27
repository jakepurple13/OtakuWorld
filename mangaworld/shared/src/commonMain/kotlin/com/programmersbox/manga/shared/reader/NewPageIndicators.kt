package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** [page] / [pages] as a fraction for the progress indicators below. */
private fun fraction(page: Int, pages: Int): Float = if (pages <= 0) 0f else (page.toFloat() / pages).coerceIn(0f, 1f)

@Composable
private fun progressColor(page: Int, pages: Int): Color {
    val color by animateColorAsState(
        targetValue = if (page >= pages) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    return color
}

private val ProgressSpring = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

/**
 * Small ring in the corner that fills with chapter progress. While [active] it grows and shows [page];
 * otherwise it shrinks to a bare ring.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PageRing(
    page: Int,
    pages: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val size by animateDpAsState(if (active) 44.dp else 26.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val alpha by animateFloatAsState(if (active) 1f else .75f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val shown by animateFloatAsState(fraction(page, pages), ProgressSpring)
    val color = progressColor(page, pages)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(IndicatorContainer)
            .clickable(onClick = onClick)
    ) {
        CircularWavyProgressIndicator(
            progress = { shown },
            color = color,
            trackColor = Color.White.copy(alpha = .18f),
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        )
        AnimatedVisibility(
            visible = active,
            enter = fadeIn() + scaleIn(initialScale = .6f),
            exit = fadeOut() + scaleOut(targetScale = .6f),
        ) {
            RollingNumber(
                page,
                MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold).tabular()
            )
        }
    }
}

/** Top progress line that ripples while [turning] and lies flat otherwise. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WavyProgressLine(
    page: Int,
    pages: Int,
    visible: Boolean,
    turning: Boolean,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState(fraction(page, pages), ProgressSpring)
    val amplitude by animateFloatAsState(if (turning) 1f else 0f, tween(400))
    val color = progressColor(page, pages)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier.fillMaxWidth(),
    ) {
        LinearWavyProgressIndicator(
            progress = { shown },
            amplitude = { amplitude },
            color = color,
            trackColor = Color.Gray.copy(alpha = .35f),
            stopSize = 0.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Pager-style dots showing about [DOT_WINDOW] pages around the current one. The window slides as pages turn;
 * while [active] the current dot stretches into a pill with the page number.
 */
@Composable
internal fun PageDotWindow(
    index: Int,
    pages: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val alpha by animateFloatAsState(if (active) 1f else .7f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val bouncy = spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy)
    val currentWidth by animateDpAsState(if (active) 30.dp else 16.dp, bouncy)
    val currentHeight by animateDpAsState(if (active) 18.dp else DotSize, bouncy)
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val numberStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold).tabular()

    Box(
        modifier
            .size(DotWindowWidth, DotWindowHeight)
            .graphicsLayer {
                this.alpha = alpha
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(DotWindowFade, blendMode = BlendMode.DstIn)
            }
            .clip(CircleShape)
            .background(IndicatorContainer)
            .clickable(onClick = onClick)
    ) {
        val first = (index - DOT_WINDOW).coerceAtLeast(0)
        val last = (index + DOT_WINDOW).coerceAtMost(pages - 1)
        for (i in first..last) {
            key(i) {
                val isCurrent = i == index
                val width = if (isCurrent) currentWidth else DotSize
                val height = if (isCurrent) currentHeight else DotSize
                // Distance from the window's center to this dot's center
                val target = when {
                    i < index -> -(currentWidth / 2 + DotGap + DotSize / 2 + DotPitch * (index - 1 - i))
                    i > index -> currentWidth / 2 + DotGap + DotSize / 2 + DotPitch * (i - index - 1)
                    else -> 0.dp
                }
                val center by animateDpAsState(target, slide)
                val color by animateColorAsState(
                    when {
                        isCurrent -> primary
                        i < index -> primary.copy(alpha = .55f)
                        else -> Color.White.copy(alpha = .45f)
                    }
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (DotWindowWidth / 2 + center - width / 2).roundToPx(),
                                ((DotWindowHeight - height) / 2).roundToPx()
                            )
                        }
                        .size(width, height)
                        .background(color, CircleShape)
                ) {
                    if (isCurrent && active) Text("${i + 1}", style = numberStyle, color = onPrimary)
                }
            }
        }
    }
}

private val DotSize = 6.dp
private val DotGap = 6.dp
private val DotPitch = DotSize + DotGap
private val DotWindowWidth = 150.dp
private val DotWindowHeight = 28.dp
private val DotWindowFade = Brush.horizontalGradient(
    0f to Color.Transparent,
    .22f to Color.Black,
    .78f to Color.Black,
    1f to Color.Transparent,
)
