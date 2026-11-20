package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.GenreCard
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingAppConfig
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import kotlinx.coroutines.delay

/** Step 1: genre cards dealt into a fan, then "Coming up" chips. Hero hidden (the fan is the hero). */
fun OnboardingScope.welcomeStep(
    appName: String,
    config: OnboardingAppConfig,
    comingUp: List<Pair<ImageVector, String>>,
) {
    val spec = OnboardingStepSpec(
        id = "welcome",
        title = "Welcome to $appName",
        body = "Your library, set up your way. About a minute, and you can change all of it later.",
        icon = Icons.Default.Star,
        shape = HeroShape.Hidden,
    )
    step(spec) {
        StepLayout(spec) {
            GenreFan(config.genres)
            Text(
                "Coming up",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ComingUpChips(comingUp)
        }
    }
}

private val CardWidth = 78.dp
private val CardHeight = 117.dp

@Composable
private fun GenreFan(genres: List<GenreCard>) {
    val reduced = onboardingReducedMotion()
    val density = LocalDensity.current
    val belowPx = with(density) { 160.dp.toPx() }
    val spreadPx = with(density) { 30.dp.toPx() }
    Box(Modifier.fillMaxWidth().height(160.dp)) {
        val mid = (genres.size - 1) / 2f
        genres.forEachIndexed { k, genre ->
            // 0 = dealt 160 dp below, 1 = resting in the fan.
            val deal = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(reduced) {
                if (reduced) {
                    deal.snapTo(1f)
                } else {
                    delay(120L + k * 90L)
                    deal.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                }
            }
            GenreCardView(
                genre,
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .graphicsLayer {
                        val p = deal.value
                        translationY = (1f - p) * belowPx
                        translationX = (k - mid) * spreadPx * p
                        rotationZ = (k - mid) * 9f * p
                        transformOrigin = TransformOrigin(0.5f, 1.2f)
                    },
            )
        }
    }
}

@Composable
private fun GenreCardView(genre: GenreCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .shadow(8.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.hsv(genre.hue.mod(360f), 0.6f, 0.85f),
                        Color.hsv((genre.hue + 30f).mod(360f), 0.55f, 0.5f),
                    )
                )
            )
            .size(CardWidth, CardHeight),
    ) {
        Icon(genre.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(
            genre.name,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComingUpChips(items: List<Pair<ImageVector, String>>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { (icon, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}
