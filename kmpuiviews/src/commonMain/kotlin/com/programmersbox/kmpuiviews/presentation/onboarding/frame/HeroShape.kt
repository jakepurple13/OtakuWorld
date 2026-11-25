package com.programmersbox.kmpuiviews.presentation.onboarding.frame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape as Hero

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Hero.polygon(): RoundedPolygon = when (this) {
    Hero.Cookie -> MaterialShapes.Cookie9Sided
    Hero.Diamond -> MaterialShapes.Diamond
    Hero.Arch -> MaterialShapes.Arch
    Hero.Clover -> MaterialShapes.Clover4Leaf
    Hero.Bun -> MaterialShapes.Bun
    Hero.Sunny -> MaterialShapes.Sunny
    Hero.Flower -> MaterialShapes.Flower
    Hero.Done, Hero.Hidden -> MaterialShapes.Circle
}

/**
 * The step hero: a Material expressive shape that morphs, rotates and scales between steps, with the
 * step icon counter-rotated on top so it always reads upright. [Hero.Hidden] collapses it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroShape(
    shape: Hero,
    icon: ImageVector,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val reduced = onboardingReducedMotion()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val iconSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    // Morph progress runs 0 -> 1 between `from` and `to`; a new target restarts from the current shape.
    var from by remember { mutableStateOf(shape) }
    var to by remember { mutableStateOf(shape) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(shape) {
        if (shape == to) return@LaunchedEffect
        // Hidden keeps whatever shape was showing so only the scale collapses.
        val target = if (shape == Hero.Hidden) to else shape
        from = to
        to = target
        progress.snapTo(0f)
        if (reduced) progress.snapTo(1f) else progress.animateTo(1f, spatial)
    }
    val morph = remember(from, to) { Morph(from.polygon().normalized(), to.polygon().normalized()) }

    val rotation by animateFloatAsState(
        shape.rotation,
        if (reduced) snapSpec() else spatial,
        label = "heroRotation",
    )
    val animatedScale by animateFloatAsState(
        shape.scale,
        if (reduced) snapSpec() else spatial,
        label = "heroScale",
    )
    // A spring can overshoot below zero (toward Hidden); never mirror the hero.
    val scale = animatedScale.coerceAtLeast(0f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            // Tagged after scale so its semantics bounds include the scale (rotation excluded).
            .testTag(OnboardingTags.Hero)
            .rotate(rotation)
            .clip(MorphShape(morph, progress.value))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                if (reduced) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    scaleIn(iconSpatial) togetherWith scaleOut()
                }
            },
            label = "heroIcon",
        ) { vector ->
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(size / 2)
                    .rotate(-rotation),
            )
        }
    }
}

private fun snapSpec() = androidx.compose.animation.core.snap<Float>()

/** Draws a [Morph] at [progress] scaled to the layout size (the polygons are normalized to 0..1). */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cubics = morph.asCubics(progress)
        if (cubics.isNotEmpty()) {
            path.moveTo(cubics.first().anchor0X * size.width, cubics.first().anchor0Y * size.height)
            cubics.forEach {
                path.cubicTo(
                    it.control0X * size.width, it.control0Y * size.height,
                    it.control1X * size.width, it.control1Y * size.height,
                    it.anchor1X * size.width, it.anchor1Y * size.height,
                )
            }
            path.close()
        }
        return Outline.Generic(path)
    }
}
