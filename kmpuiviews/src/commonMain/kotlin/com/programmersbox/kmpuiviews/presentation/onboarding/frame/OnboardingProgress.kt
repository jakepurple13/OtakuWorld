package com.programmersbox.kmpuiviews.presentation.onboarding.frame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion

/** One segment per step; the current one is 2.4x wide, and it and every earlier one is filled. */
@Composable
fun OnboardingProgress(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val reduced = onboardingReducedMotion()
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag(OnboardingTags.Progress),
    ) {
        repeat(count) { index ->
            val spec = if (reduced) snap<Float>() else spatial
            val grow by animateFloatAsState(if (index == current) 1.4f else 0f, spec, label = "segmentWidth")
            val fill by animateFloatAsState(if (index <= current) 1f else 0f, spec, label = "segmentFill")
            Box(
                modifier = Modifier
                    .weight(1f + grow)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("${OnboardingTags.Progress}_$index"),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
