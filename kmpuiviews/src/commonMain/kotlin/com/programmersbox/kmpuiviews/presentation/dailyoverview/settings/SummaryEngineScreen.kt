package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.presentation.dailyoverview.components.SummaryCard
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.stringResource
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_change_ai_helper
import otakuworld.kmpuiviews.generated.resources.do_engine_automatic
import otakuworld.kmpuiviews.generated.resources.do_engine_automatic_desc
import otakuworld.kmpuiviews.generated.resources.do_engine_automatic_desc_plain
import otakuworld.kmpuiviews.generated.resources.do_engine_gemma
import otakuworld.kmpuiviews.generated.resources.do_engine_gemma_desc
import otakuworld.kmpuiviews.generated.resources.do_engine_nano_desc
import otakuworld.kmpuiviews.generated.resources.do_engine_numbers
import otakuworld.kmpuiviews.generated.resources.do_engine_numbers_desc
import otakuworld.kmpuiviews.generated.resources.do_engine_ollama
import otakuworld.kmpuiviews.generated.resources.do_engine_ollama_desc
import otakuworld.kmpuiviews.generated.resources.do_engine_provider
import otakuworld.kmpuiviews.generated.resources.do_engine_provider_desc
import otakuworld.kmpuiviews.generated.resources.do_label_nano
import otakuworld.kmpuiviews.generated.resources.do_no_model
import otakuworld.kmpuiviews.generated.resources.do_no_provider
import otakuworld.kmpuiviews.generated.resources.do_preview
import otakuworld.kmpuiviews.generated.resources.do_provider_warning
import otakuworld.kmpuiviews.generated.resources.do_set_up_ai_helper
import otakuworld.kmpuiviews.generated.resources.do_set_up_gemma
import otakuworld.kmpuiviews.generated.resources.do_summary_engine
import otakuworld.kmpuiviews.generated.resources.do_tag_available
import otakuworld.kmpuiviews.generated.resources.do_tag_not_available
import otakuworld.kmpuiviews.generated.resources.do_tag_on_device
import otakuworld.kmpuiviews.generated.resources.do_tag_online
import otakuworld.kmpuiviews.generated.resources.do_tag_ready
import otakuworld.kmpuiviews.generated.resources.do_test
import otakuworld.kmpuiviews.generated.resources.do_test_hint

private enum class TagTone { Neutral, Good, Warn }

private class Tag(val text: String, val tone: TagTone = TagTone.Neutral)

@Composable
fun SummaryEngineScreen(
    viewModel: DailyOverviewSettingsViewModel = koinViewModel(),
) {
    val navActions = LocalNavActions.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val test by viewModel.test.collectAsStateWithLifecycle()
    val open: (NavKey) -> Unit = { navActions.navigate(it) }

    RefreshOnReturn(viewModel::refresh)

    SettingsScaffold(title = stringResource(Res.string.do_summary_engine)) {
        val state = ui ?: return@SettingsScaffold
        val choice = state.prefs.engine

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .selectableGroup()
        ) {
            EngineCard(
                selected = choice == SummaryEngineChoice.Automatic,
                onSelect = { viewModel.choose(SummaryEngineChoice.Automatic) },
                title = stringResource(Res.string.do_engine_automatic),
                // "Right now" waits for the statuses, so it never reads "Right now: ."
                description = if (state.automaticLabel.isEmpty()) {
                    stringResource(Res.string.do_engine_automatic_desc_plain)
                } else {
                    stringResource(Res.string.do_engine_automatic_desc, state.automaticLabel)
                },
            )

            state.engines.forEach { row ->
                val engineChoice = SummaryEngineChoice.valueOf(row.id.name)
                val selected = choice == engineChoice
                val onSelect = { viewModel.choose(engineChoice) }
                val ready = row.status == EngineStatus.Ready
                val reason = (row.status as? EngineStatus.NeedsSetup)?.reason
                when (row.id) {
                    SummaryEngineId.Nano -> EngineCard(
                        selected = selected,
                        enabled = row.status != EngineStatus.Unavailable,
                        onSelect = onSelect,
                        title = stringResource(Res.string.do_label_nano),
                        description = stringResource(Res.string.do_engine_nano_desc),
                        tags = listOf(
                            when {
                                ready -> Tag(stringResource(Res.string.do_tag_available), TagTone.Good)
                                reason != null -> Tag(reason)
                                else -> Tag(stringResource(Res.string.do_tag_not_available), TagTone.Warn)
                            }
                        ),
                    )

                    SummaryEngineId.Gemma -> EngineCard(
                        selected = selected,
                        onSelect = onSelect,
                        title = stringResource(Res.string.do_engine_gemma),
                        description = stringResource(Res.string.do_engine_gemma_desc),
                        tags = listOf(
                            Tag(stringResource(Res.string.do_tag_on_device), TagTone.Good),
                            if (ready) Tag(stringResource(Res.string.do_tag_ready), TagTone.Good)
                            else Tag(stringResource(Res.string.do_no_model)),
                        ),
                        always = {
                            if (ready) {
                                Detail(row.label)
                            } else {
                                // The models page only lists downloads; AI Helper settings is where a
                                // LiteRT Gemma model is picked and downloaded.
                                aiHelperSettingsRoute?.let { route ->
                                    Button(onClick = { open(route) }) { Text(stringResource(Res.string.do_set_up_gemma)) }
                                }
                            }
                        },
                    )

                    SummaryEngineId.Provider -> EngineCard(
                        selected = selected,
                        enabled = ready,
                        onSelect = onSelect,
                        title = stringResource(Res.string.do_engine_provider),
                        description = stringResource(Res.string.do_engine_provider_desc),
                        tags = listOf(Tag(stringResource(Res.string.do_tag_online))),
                        always = {
                            if (!ready) {
                                Detail(stringResource(Res.string.do_no_provider))
                                aiHelperSettingsRoute?.let { route ->
                                    Button(onClick = { open(route) }) { Text(stringResource(Res.string.do_set_up_ai_helper)) }
                                }
                            }
                        },
                        whenSelected = {
                            ProviderBox(row.shortName)
                            WarnBox(stringResource(Res.string.do_provider_warning, row.destination.orEmpty()))
                            aiHelperSettingsRoute?.let { route ->
                                OutlinedButton(onClick = { open(route) }) {
                                    Text(stringResource(Res.string.do_change_ai_helper))
                                }
                            }
                        },
                    )

                    SummaryEngineId.Ollama -> EngineCard(
                        selected = selected,
                        onSelect = onSelect,
                        title = stringResource(Res.string.do_engine_ollama),
                        description = stringResource(Res.string.do_engine_ollama_desc),
                        tags = listOf(
                            Tag(stringResource(Res.string.do_tag_on_device), TagTone.Good),
                            if (ready) Tag(stringResource(Res.string.do_tag_ready), TagTone.Good)
                            else Tag(reason.orEmpty(), TagTone.Warn),
                        ),
                        always = {
                            if (ready) {
                                Detail(row.label)
                            } else {
                                aiHelperSettingsRoute?.let { route ->
                                    Button(onClick = { open(route) }) { Text(stringResource(Res.string.do_set_up_ai_helper)) }
                                }
                            }
                        },
                    )

                    SummaryEngineId.Template -> EngineCard(
                        selected = selected,
                        onSelect = onSelect,
                        title = stringResource(Res.string.do_engine_numbers),
                        description = stringResource(Res.string.do_engine_numbers_desc),
                    )
                }
            }

            when (val summary = test) {
                null -> IdleTestCard(onTest = viewModel::runTest)
                else -> SummaryCard(
                    summary = summary,
                    needsSetup = false,
                    onRegenerate = viewModel::runTest,
                    onSetUp = {},
                    actionLabel = stringResource(Res.string.do_test),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Keeps the last card clear of the floating nav.
            Spacer(Modifier.height(LocalNavHostPadding.current.calculateBottomPadding() + 16.dp))
        }
    }
}

/**
 * Calls [onReturn] each time the entry comes back on screen (e.g. back from the AI Helper), but
 * not on the first open, which the ViewModel already loaded. Nav3 drops covered entries from
 * composition and keeps their saveable state, so the effect reruns on return.
 */
@Composable
internal fun RefreshOnReturn(onReturn: () -> Unit) {
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (opened) onReturn() else opened = true
    }
}

/**
 * A selectable engine card. [always] shows under the header whatever the selection;
 * [whenSelected] expands in only for the selected card. A disabled card dims its header but keeps
 * [always] usable, so a setup button still works.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EngineCard(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    description: String,
    enabled: Boolean = true,
    tags: List<Tag> = emptyList(),
    always: @Composable ColumnScope.() -> Unit = {},
    whenSelected: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) lerp(colors.surfaceContainer, colors.primaryContainer, 0.4f) else colors.surfaceContainer
    )
    val border by animateColorAsState(if (selected) colors.primary else Color.Transparent)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(2.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .animateContentSize()
                .padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(if (enabled) 1f else 0.55f)
            ) {
                RadioDot(selected)
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 5.dp)
                        ) { tags.forEach { TagChip(it) } }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 30.dp)
            ) {
                always()
                if (selected) whenSelected()
            }
        }
    }
}

/** Radio dot whose center springs in, as in the mockup. */
@Composable
private fun RadioDot(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
    val ring by animateColorAsState(if (selected) colors.primary else colors.outline)
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(20.dp)
            .border(2.dp, ring, CircleShape)
            .padding(5.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(colors.primary, CircleShape)
        )
    }
}

@Composable
private fun TagChip(tag: Tag) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (tag.tone) {
        TagTone.Neutral -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        TagTone.Good -> colors.primaryContainer to colors.onPrimaryContainer
        TagTone.Warn -> colors.errorContainer to colors.error
    }
    Text(
        tag.text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun Detail(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ProviderBox(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(Color(0xFF2E9D5B), CircleShape)
        )
        Text(label, fontSize = 12.5.sp)
    }
}

@Composable
private fun WarnBox(text: String) {
    Text(
        text,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun IdleTestCard(onTest: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.do_preview).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.55.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(stringResource(Res.string.do_test_hint), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Box(Modifier.weight(1f))
                AssistChip(
                    onClick = onTest,
                    label = { Text(stringResource(Res.string.do_test), fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = null,
                )
            }
        }
    }
}
