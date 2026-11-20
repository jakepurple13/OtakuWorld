package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion

private const val IntervalTag = "onb_updates_interval"

/**
 * Step 7: background update checks. The master switch only flips `shouldRun`; interval, network and
 * the battery options are kept untouched while it is off. Writing `mediaCheckerSettings` is all that
 * is needed to take effect: the platform `BackgroundWorkHandlerImpl` observes that flow and
 * (re)schedules or cancels the periodic work itself.
 */
fun OnboardingScope.updatesStep(handling: NewSettingsHandling, updateNoun: String) {
    val spec = OnboardingStepSpec(
        id = "updates",
        title = "Never miss a $updateNoun",
        body = "Choose how often to check your favorites for new ones.",
        icon = Icons.Default.Notifications,
        shape = HeroShape.Sunny,
        summaryLabel = "Updates",
        summary = {
            val settings by handling.mediaCheckerSettings.rememberPreference()
            when {
                !settings.shouldRun -> "Update checks off"
                settings.networkType == MediaCheckerNetworkType.Unmetered -> "Every ${settings.interval}h · Wi‑Fi only"
                else -> "Every ${settings.interval}h · any network"
            }
        },
    )
    step(spec) {
        var settings by handling.mediaCheckerSettings.rememberPreference()
        Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
            StepLayout(spec) {
                val reduced = onboardingReducedMotion()
                SwitchSetting(
                    settingTitle = { Text("Check for updates") },
                    summaryValue = { Text("Look for new ${updateNoun}s in the background") },
                    settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
                    value = settings.shouldRun,
                    updateValue = { settings = settings.copy(shouldRun = it) },
                )
                AnimatedVisibility(
                    visible = settings.shouldRun,
                    enter = if (reduced) EnterTransition.None else expandVertically(),
                    exit = if (reduced) ExitTransition.None else shrinkVertically(),
                ) {
                    Column(
                        Modifier.fillMaxWidth().testTag(OnboardingTags.UpdatesOptions),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IntervalRow(
                            interval = settings.interval,
                            ringing = !reduced,
                            onInterval = { settings = settings.copy(interval = it) },
                        )
                        Text(
                            "Network",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // A stored Metered value has no chip: neither is selected and the value is kept until a chip is picked.
                        val networks = listOf(
                            MediaCheckerNetworkType.Connected to "Any network",
                            MediaCheckerNetworkType.Unmetered to "Wi‑Fi only",
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            networks.forEachIndexed { index, (type, label) ->
                                SegmentedButton(
                                    selected = settings.networkType == type,
                                    onClick = { settings = settings.copy(networkType = type) },
                                    shape = SegmentedButtonDefaults.itemShape(index, networks.size),
                                    label = { Text(label) },
                                )
                            }
                        }
                        CategoryGroup {
                            item {
                                SwitchSetting(
                                    settingTitle = { Text("Only when charging") },
                                    settingIcon = { Icon(Icons.Default.BatteryChargingFull, null) },
                                    value = settings.requiresCharging,
                                    updateValue = { settings = settings.copy(requiresCharging = it) },
                                )
                            }
                            item {
                                SwitchSetting(
                                    settingTitle = { Text("Skip on low battery") },
                                    settingIcon = { Icon(Icons.Default.Battery1Bar, null) },
                                    value = settings.requiresBatteryNotLow,
                                    updateValue = { settings = settings.copy(requiresBatteryNotLow = it) },
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !settings.shouldRun,
                    enter = if (reduced) EnterTransition.None else expandVertically(),
                    exit = if (reduced) ExitTransition.None else shrinkVertically(),
                ) {
                    Text(
                        "You can still pull to refresh on Notifications. Turn checks on anytime in Settings › Notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalRow(
    interval: Long,
    ringing: Boolean,
    onInterval: (Long) -> Unit,
) {
    val hours = interval.coerceIn(1L, 24L)
    // Local value follows the drag; the stored value (which reschedules the worker) is written on release.
    var slider by remember(hours) { mutableFloatStateOf(hours.toFloat()) }
    val progress by animateFloatAsState(
        targetValue = slider / 24f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(96.dp),
                strokeWidth = 9.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text("${slider.toInt()}h", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f)) {
            RingingBell(ringing)
            Text(
                "Check favorites every",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = slider,
                onValueChange = { slider = it },
                valueRange = 1f..24f,
                steps = 22,
                onValueChangeFinished = { onInterval(slider.toLong().coerceIn(1L, 24L)) },
                modifier = Modifier.testTag(IntervalTag),
            )
        }
    }
}

/** Bell that rocks every 2.4 s; static when [ringing] is false (reduced motion). */
@Composable
private fun RingingBell(ringing: Boolean) {
    val angle = if (ringing) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2400
                    0f at 0 using LinearEasing
                    14f at 100 using LinearEasing
                    -12f at 220 using LinearEasing
                    10f at 340 using LinearEasing
                    -8f at 460 using LinearEasing
                    4f at 580 using LinearEasing
                    0f at 700 using LinearEasing
                    0f at 2400
                },
                repeatMode = RepeatMode.Restart,
            ),
        ).value
    } else {
        0f
    }
    Icon(
        Icons.Default.Notifications,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(20.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.1f)
                rotationZ = angle
            },
    )
}
