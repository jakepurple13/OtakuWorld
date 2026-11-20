package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.kmpuiviews.presentation.tips.Tip
import com.programmersbox.kmpuiviews.presentation.tips.TipGesture
import com.programmersbox.kmpuiviews.presentation.tips.buildTips
import com.programmersbox.kmpuiviews.presentation.tips.sharedTips
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import org.koin.compose.koinInject

/**
 * Step 8: one practice tip. Shows the first Hold tip that has a demo (shared tips first, then the
 * app's) and reports via [onPracticeChange] while a pointer is down on it, so the host pager can stop
 * swiping. [tips] defaults to the shared tips plus the running app's `ComposeSettingsDsl.tips`.
 */
fun OnboardingScope.tipsStep(
    onPracticeChange: (Boolean) -> Unit,
    tips: List<Tip>? = null,
) {
    val spec = OnboardingStepSpec(
        id = "tips",
        title = "Hidden gestures",
        body = "Some actions sit behind a long press.",
        icon = Icons.Outlined.BackHand,
        shape = HeroShape.Flower,
    )
    step(spec) {
        val all = tips ?: run {
            val dsl: ComposeSettingsDsl = koinInject()
            remember(dsl) { sharedTips + buildTips(dsl.tips) }
        }
        val tip = remember(all) { all.firstOrNull { it.gesture == TipGesture.Hold && it.demo != null } }
        var done by remember(tip) { mutableStateOf(false) }
        val updatedPractice by rememberUpdatedState(onPracticeChange)
        DisposableEffect(Unit) { onDispose { updatedPractice(false) } }

        Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
            StepLayout(spec) {
                val reduced = onboardingReducedMotion()
                val demo = tip?.demo
                if (demo != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        updatedPractice(event.changes.any { it.pressed })
                                    }
                                }
                            },
                    ) { demo { done = true } }

                    val hint = if (done) "Nice. That menu works on every cover." else "Press and hold the cover."
                    if (reduced) {
                        HintText(hint)
                    } else {
                        AnimatedContent(
                            targetState = hint,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { HintText(it) }
                    }
                }
                Text(
                    "More in Settings › Discover › Tips & Gestures",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
