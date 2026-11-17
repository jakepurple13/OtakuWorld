@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.domain.aitext.AiEnginePreference
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.ai.AiCard
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.presentation.dailyoverview.settings.aiHelperSettingsRoute
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_feature_daily
import otakuworld.kmpuiviews.generated.resources.ai_feature_daily_desc
import otakuworld.kmpuiviews.generated.resources.ai_feature_daily_engine_desc
import otakuworld.kmpuiviews.generated.resources.ai_feature_errors
import otakuworld.kmpuiviews.generated.resources.ai_feature_errors_desc
import otakuworld.kmpuiviews.generated.resources.ai_feature_lists
import otakuworld.kmpuiviews.generated.resources.ai_feature_lists_desc
import otakuworld.kmpuiviews.generated.resources.ai_feature_recap
import otakuworld.kmpuiviews.generated.resources.ai_feature_recap_desc
import otakuworld.kmpuiviews.generated.resources.ai_feature_tldr
import otakuworld.kmpuiviews.generated.resources.ai_feature_tldr_desc
import otakuworld.kmpuiviews.generated.resources.ai_feature_writing
import otakuworld.kmpuiviews.generated.resources.ai_feature_writing_desc
import otakuworld.kmpuiviews.generated.resources.ai_group_daily
import otakuworld.kmpuiviews.generated.resources.ai_group_details
import otakuworld.kmpuiviews.generated.resources.ai_group_library
import otakuworld.kmpuiviews.generated.resources.ai_group_reading
import otakuworld.kmpuiviews.generated.resources.ai_group_updates
import otakuworld.kmpuiviews.generated.resources.ai_pref_automatic
import otakuworld.kmpuiviews.generated.resources.ai_pref_automatic_desc
import otakuworld.kmpuiviews.generated.resources.ai_pref_gemma
import otakuworld.kmpuiviews.generated.resources.ai_pref_gemma_desc
import otakuworld.kmpuiviews.generated.resources.ai_pref_nano
import otakuworld.kmpuiviews.generated.resources.ai_pref_nano_desc
import otakuworld.kmpuiviews.generated.resources.ai_set_up
import otakuworld.kmpuiviews.generated.resources.ai_settings_engine
import otakuworld.kmpuiviews.generated.resources.ai_settings_footer
import otakuworld.kmpuiviews.generated.resources.ai_settings_master
import otakuworld.kmpuiviews.generated.resources.ai_settings_master_desc
import otakuworld.kmpuiviews.generated.resources.ai_settings_nothing_ready
import otakuworld.kmpuiviews.generated.resources.ai_settings_prefer
import otakuworld.kmpuiviews.generated.resources.ai_settings_right_now
import otakuworld.kmpuiviews.generated.resources.ai_settings_title
import otakuworld.kmpuiviews.generated.resources.ai_status_ready
import otakuworld.kmpuiviews.generated.resources.ai_status_setup
import otakuworld.kmpuiviews.generated.resources.ai_status_unavailable
import otakuworld.kmpuiviews.generated.resources.ai_try_hint
import otakuworld.kmpuiviews.generated.resources.ai_try_it
import otakuworld.kmpuiviews.generated.resources.ai_try_run
import otakuworld.kmpuiviews.generated.resources.do_summary_engine

private data class FeatureRow(val toggle: AiToggle, val title: StringResource, val desc: StringResource)

private val featureGroups: List<Pair<StringResource, List<FeatureRow>>> = listOf(
    Res.string.ai_group_details to listOf(
        FeatureRow(AiToggle.Tldr, Res.string.ai_feature_tldr, Res.string.ai_feature_tldr_desc),
    ),
    Res.string.ai_group_reading to listOf(
        FeatureRow(AiToggle.ChapterRecap, Res.string.ai_feature_recap, Res.string.ai_feature_recap_desc),
        FeatureRow(AiToggle.WritingTools, Res.string.ai_feature_writing, Res.string.ai_feature_writing_desc),
    ),
    Res.string.ai_group_library to listOf(
        FeatureRow(AiToggle.ListSuggestions, Res.string.ai_feature_lists, Res.string.ai_feature_lists_desc),
    ),
    Res.string.ai_group_updates to listOf(
        FeatureRow(AiToggle.ErrorExplanations, Res.string.ai_feature_errors, Res.string.ai_feature_errors_desc),
    ),
)

@Composable
fun AiSettingsScreen(
    viewModel: AiSettingsViewModel = koinViewModel(),
) {
    val navActions = LocalNavActions.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val sample by viewModel.sample.collectAsStateWithLifecycle()

    // Statuses change outside the app (Nano downloads, Gemma set up in AI Helper)
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SettingsScaffold(
        title = stringResource(Res.string.ai_settings_title),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val state = ui ?: return@SettingsScaffold
        val bodyAlpha by animateFloatAsState(if (state.enabled) 1f else 0.45f, label = "aiBodyAlpha")

        CategoryGroupListItem {
            segmentedListItem(
                onClick = { viewModel.setEnabled(!state.enabled) },
                leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                supportingContent = { Text(stringResource(Res.string.ai_settings_master_desc)) },
                trailingContent = { Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled) },
            ) { Text(stringResource(Res.string.ai_settings_master)) }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.alpha(bodyAlpha)
        ) {
            EngineCard(
                state = state,
                sample = sample,
                onPreference = viewModel::setPreference,
                onTry = viewModel::trySample,
                onSetUp = aiHelperSettingsRoute?.let { route -> { navActions.navigate(route) } },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            featureGroups.forEach { (group, rows) ->
                CategoryGroupListItem {
                    categorySetting { Text(stringResource(group)) }
                    rows.forEach { row -> featureSwitch(row, state, viewModel::setFeature) }
                }
            }

            CategoryGroupListItem {
                categorySetting { Text(stringResource(Res.string.ai_group_daily)) }
                featureSwitch(
                    FeatureRow(AiToggle.DailyOverview, Res.string.ai_feature_daily, Res.string.ai_feature_daily_desc),
                    state,
                    viewModel::setFeature,
                )
                segmentedListItem(
                    onClick = { navActions.navigate(Screen.DailyOverview.Engine) },
                    enabled = state.enabled && AiToggle.DailyOverview !in state.disabled,
                    supportingContent = { Text(stringResource(Res.string.ai_feature_daily_engine_desc)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                ) { Text(stringResource(Res.string.do_summary_engine)) }
            }

            Text(
                stringResource(Res.string.ai_settings_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

private fun CategoryGroupScope.featureSwitch(
    row: FeatureRow,
    state: AiSettingsUi,
    onChange: (AiToggle, Boolean) -> Unit,
) {
    val on = row.toggle !in state.disabled
    segmentedListItem(
        onClick = { onChange(row.toggle, !on) },
        enabled = state.enabled,
        supportingContent = { Text(stringResource(row.desc)) },
        trailingContent = {
            Switch(checked = on, enabled = state.enabled, onCheckedChange = { onChange(row.toggle, it) })
        },
    ) { Text(stringResource(row.title)) }
}

@Composable
private fun EngineCard(
    state: AiSettingsUi,
    sample: AiTextState,
    onPreference: (AiEnginePreference) -> Unit,
    onTry: () -> Unit,
    onSetUp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(stringResource(Res.string.ai_settings_engine), style = MaterialTheme.typography.titleSmall)
            Column {
                Text(
                    stringResource(Res.string.ai_settings_right_now),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedContent(
                    targetState = state.current,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "aiCurrentEngine",
                ) { current ->
                    Text(
                        current ?: stringResource(Res.string.ai_settings_nothing_ready),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            state.engines.forEach { engine ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(engine.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    when (engine.status) {
                        EngineStatus.Ready -> StatusLabel(stringResource(Res.string.ai_status_ready), good = true)
                        EngineStatus.Unavailable -> StatusLabel(stringResource(Res.string.ai_status_unavailable), good = false)
                        is EngineStatus.NeedsSetup -> if (onSetUp != null) {
                            TextButton(onClick = onSetUp) { Text(stringResource(Res.string.ai_set_up)) }
                        } else {
                            StatusLabel(stringResource(Res.string.ai_status_setup), good = false)
                        }
                    }
                }
            }

            if (state.showPreference) {
                Text(
                    stringResource(Res.string.ai_settings_prefer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val options = AiEnginePreference.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, preference ->
                        SegmentedButton(
                            selected = state.preference == preference,
                            onClick = { onPreference(preference) },
                            enabled = state.enabled,
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(stringResource(preference.label())) }
                    }
                }
                Text(
                    stringResource(state.preference.description()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (sample is AiTextState.Idle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.ai_try_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onTry, enabled = state.enabled) { Text(stringResource(Res.string.ai_try_run)) }
                }
            } else {
                AiCard(
                    label = stringResource(Res.string.ai_try_it),
                    state = sample,
                    onRetry = onTry,
                    headerAction = {
                        TextButton(onClick = onTry, enabled = state.enabled) { Text(stringResource(Res.string.ai_try_run)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(text: String, good: Boolean) {
    Surface(
        color = if (good) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (good) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

private fun AiEnginePreference.label() = when (this) {
    AiEnginePreference.Automatic -> Res.string.ai_pref_automatic
    AiEnginePreference.Nano -> Res.string.ai_pref_nano
    AiEnginePreference.Gemma -> Res.string.ai_pref_gemma
}

private fun AiEnginePreference.description() = when (this) {
    AiEnginePreference.Automatic -> Res.string.ai_pref_automatic_desc
    AiEnginePreference.Nano -> Res.string.ai_pref_nano_desc
    AiEnginePreference.Gemma -> Res.string.ai_pref_gemma_desc
}
