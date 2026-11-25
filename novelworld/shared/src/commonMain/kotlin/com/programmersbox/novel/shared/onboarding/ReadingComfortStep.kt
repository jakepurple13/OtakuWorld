package com.programmersbox.novel.shared.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.novel.shared.NovelTextSizeRange
import kotlin.math.roundToInt

internal const val ReadingSampleTag = "onb_reading_sample"
internal const val ReadingSizeTag = "onb_reading_size"

private const val SampleParagraph =
    "The ferry horn sounded twice before Mio looked up. Rain had followed her across the harbor, " +
            "tapping on the lantern glass, and the ledger in her lap was still open to the page with " +
            "her grandmother's handwriting."

/** Step 5 for NovelWorld: pick the reader's text size against a sample paragraph. */
fun OnboardingScope.readingComfortStep(textSize: DataStoreHandler<Int>) {
    val spec = OnboardingStepSpec(
        id = "app",
        title = "Reading comfort",
        body = "Set a text size you can read for hours.",
        icon = Icons.Default.FormatSize,
        shape = HeroShape.Clover,
        summaryLabel = "Reader",
        summary = {
            val size by textSize.asState()
            "${size}sp text"
        },
    )
    step(spec) { ReadingComfortContent(spec, textSize) }
}

@Composable
private fun ReadingComfortContent(spec: OnboardingStepSpec, textSize: DataStoreHandler<Int>) {
    var saved by textSize.asState()
    // Last value while dragging; the setting is written once, on release.
    var draft by remember { mutableStateOf<Int?>(null) }
    val shown = (draft ?: saved).coerceIn(NovelTextSizeRange)
    val reduced = onboardingReducedMotion()
    val animatedSize by animateFloatAsState(
        targetValue = shown.toFloat(),
        animationSpec = if (reduced) snap() else tween(180),
        label = "sampleTextSize",
    )

    Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
        StepLayout(
            spec,
            preview = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        SampleParagraph,
                        fontSize = animatedSize.sp,
                        lineHeight = (animatedSize * 1.5f).sp,
                        modifier = Modifier.padding(16.dp).testTag(ReadingSampleTag),
                    )
                }
            },
        ) {
            Text(
                "Text size · ${shown}sp",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = shown.toFloat(),
                onValueChange = { draft = it.roundToInt().coerceIn(NovelTextSizeRange) },
                onValueChangeFinished = {
                    draft?.let { saved = it }
                    draft = null
                },
                valueRange = NovelTextSizeRange.first.toFloat()..NovelTextSizeRange.last.toFloat(),
                steps = NovelTextSizeRange.last - NovelTextSizeRange.first - 1,
                modifier = Modifier.testTag(ReadingSizeTag),
            )
        }
    }
}
