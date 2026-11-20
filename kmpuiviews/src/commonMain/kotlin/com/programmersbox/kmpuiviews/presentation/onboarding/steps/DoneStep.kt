package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.ConfettiBurst
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import kotlinx.coroutines.delay

/** Last step: confetti and one check row per spec that has a [OnboardingStepSpec.summary]. */
fun OnboardingScope.doneStep(summaries: List<OnboardingStepSpec>) {
    val spec = OnboardingStepSpec(
        id = "done",
        title = "You're all set",
        body = "Here is what you picked. Change any of it in Settings.",
        icon = Icons.Default.Check,
        shape = HeroShape.Done,
    )
    step(spec) {
        val rows = summaries.filter { it.summaryLabel != null && it.summary != null }
        val scheme = MaterialTheme.colorScheme
        val colors = listOf(
            scheme.primary,
            scheme.primaryContainer,
            scheme.tertiary,
            scheme.secondary,
            scheme.tertiaryContainer,
            scheme.secondaryContainer,
        )
        Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
            StepLayout(spec) {
                rows.forEachIndexed { i, row -> SummaryRow(i, row.summaryLabel.orEmpty(), row.summary!!()) }
            }
            ConfettiBurst(trigger = spec.id, colors = colors)
        }
    }
}

@Composable
private fun SummaryRow(index: Int, label: String, value: String) {
    val reduced = onboardingReducedMotion()
    val shown = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            shown.snapTo(1f)
        } else {
            delay(600L + index * 60L)
            shown.animateTo(1f)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = shown.value
                translationY = (1f - shown.value) * 12.dp.toPx()
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(5.dp),
        )
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
