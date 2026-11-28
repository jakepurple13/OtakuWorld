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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    // 48dp touch target around a ring that can shrink to 26dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .pageButtonSemantics(page, pages)
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            )
    ) {
        RingFace(size, alpha, shown, color, page, active)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingFace(size: Dp, alpha: Float, shown: Float, color: Color, page: Int, active: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(IndicatorContainer)
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
    val currentWidthTarget = if (active) 30.dp else 16.dp
    // Read only at placement, so the bounce moves dots without recomposing them
    val currentWidth = animateDpAsState(currentWidthTarget, bouncy)
    val slotSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val numberStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold).tabular()

    // Touch target is at least 48dp tall around the 28dp window
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .pageButtonSemantics(index + 1, pages)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
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
        ) {
            val first = (index - DOT_WINDOW).coerceAtLeast(0)
            val last = (index + DOT_WINDOW).coerceAtMost(pages - 1)
            for (i in first..last) {
                key(i) {
                    val isCurrent = i == index
                    // Signed distance from the current page, in dots; animates so the row slides
                    val slot = animateFloatAsState((i - index).toFloat(), slotSpec)
                    val width by animateDpAsState(if (isCurrent) currentWidthTarget else DotSize, bouncy)
                    val height by animateDpAsState(if (isCurrent && active) 18.dp else DotSize, bouncy)
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
                                val center = dotCenter(slot.value, currentWidth.value)
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
}

/**
 * Distance from the window's center to a dot [slot] dots away from the current one (negative = before it),
 * leaving room for the current dot's [currentWidth]. Continuous between slots so the row can slide.
 */
private fun dotCenter(slot: Float, currentWidth: Dp): Dp {
    val distance = kotlin.math.abs(slot)
    val firstNeighbor = currentWidth / 2 + DotGap + DotSize / 2
    val magnitude = if (distance >= 1f) firstNeighbor + DotPitch * (distance - 1f) else firstNeighbor * distance
    return if (slot < 0) -magnitude else magnitude
}

/** Announces "Page N of M" and acts as a button that opens the page list. */
private fun Modifier.pageButtonSemantics(page: Int, pages: Int): Modifier = semantics(mergeDescendants = true) {
    contentDescription = "Page $page of $pages"
    role = Role.Button
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
