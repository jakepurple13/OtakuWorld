package com.programmersbox.kmpuiviews.translation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.SectionLabel
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.SmallChip
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.formatModelSize
import com.programmersbox.kmpuiviews.translation.EngineKind
import org.koin.compose.viewmodel.koinViewModel

/** Two columns side by side from this width; stacked below it. */
private val WideLayout = 720.dp

private data class EngineInfo(val kind: EngineKind, val title: String, val description: String, val tags: List<String>)

private val engineInfo = listOf(
    EngineInfo(
        EngineKind.Ollama,
        "Ollama",
        "A local LLM from your running Ollama. One model covers many languages.",
        listOf("2–5 GB", "Slower", "Better with slang"),
    ),
    EngineInfo(
        EngineKind.Custom,
        "Custom",
        "Any OpenAI-compatible server, like LM Studio, llama.cpp or vLLM.",
        listOf("Your server", "Any model it loads"),
    ),
)

/** Settings › Integrations › Translation on desktop: target, storage, engine choice, the engine's models, and a test box. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopTranslationScreen(vm: DesktopTranslationViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translation") },
                navigationIcon = { BackButton() },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= WideLayout
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 28.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                val itemModifier = Modifier.widthIn(max = 960.dp).fillMaxWidth()
                item(key = "top") {
                    TwoUp(wide, itemModifier.animateItem()) { m ->
                        TargetBox(state, vm::setTarget, m)
                        StorageBox(state, m)
                    }
                }
                item(key = "engines") {
                    Column(itemModifier.animateItem(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("Engine")
                        val cards: @Composable (Modifier) -> Unit = { m ->
                            engineInfo.forEach { info ->
                                EngineCard(info, selected = state.engine == info.kind, onClick = { vm.setEngine(info.kind) }, modifier = m)
                            }
                        }
                        if (wide) {
                            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                cards(Modifier.weight(1f).fillMaxHeight())
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { cards(Modifier.fillMaxWidth()) }
                        }
                    }
                }
                item(key = "panel") {
                    AnimatedContent(
                        targetState = state.engine,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(300)) { it / 12 }) togetherWith
                                fadeOut(tween(90)) using SizeTransform(clip = false)
                        },
                        label = "enginePanel",
                        modifier = itemModifier.animateItem(),
                    ) { engine ->
                        EnginePanelSurface {
                            when (engine) {
                                EngineKind.Ollama -> OllamaPanel(state, vm)
                                EngineKind.Custom -> CustomPanel(state, vm)
                            }
                        }
                    }
                }
                item(key = "try") {
                    TryIt(state, vm, wide, itemModifier.animateItem())
                }
            }
        }
    }
}

/** Two equal, equally tall boxes side by side when [wide], else stacked; [content] gets each box's modifier. */
@Composable
private fun TwoUp(wide: Boolean, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    if (wide) {
        Row(modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            content(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) { content(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun InfoBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun BoxLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetBox(state: DesktopTranslationState, onPick: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    InfoBox(modifier) {
        BoxLabel("Translate into · used everywhere")
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = languageName(state.target),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.targets.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(languageName(code)) },
                        onClick = {
                            expanded = false
                            onPick(code)
                        },
                    )
                }
            }
        }
        Text(
            "Applies everywhere translation is used, like descriptions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StorageBox(state: DesktopTranslationState, modifier: Modifier) {
    InfoBox(modifier) {
        BoxLabel("Storage")
        AnimatedContent(
            targetState = state.storageBytes?.let { formatModelSize(it) } ?: "—",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "storageValue",
        ) { Text(it, style = MaterialTheme.typography.headlineSmall) }
        AnimatedContent(
            targetState = state.storageLabel,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
            },
            label = "storageLabel",
        ) { BoxLabel(it) }
    }
}

@Composable
private fun EngineCard(info: EngineInfo, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.primaryContainer.copy(alpha = 0.45f).compositeOver(colors.surface) else colors.surface,
        label = "engineBg",
    )
    val border by animateColorAsState(if (selected) colors.primary else colors.outlineVariant, label = "engineBorder")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 8.dp))
                Text(info.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(info.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                info.tags.forEach { SmallChip(it, colors.secondaryContainer, colors.onSecondaryContainer) }
            }
        }
    }
}

@Composable
private fun TryIt(state: DesktopTranslationState, vm: DesktopTranslationViewModel, wide: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Try it", Modifier.weight(1f))
            FilledTonalButton(onClick = vm::runTest, enabled = !state.testRunning && state.testInput.isNotBlank()) {
                Text("Translate")
            }
        }
        TwoUp(wide, Modifier.fillMaxWidth()) { m ->
            InfoBox(m) {
                BoxLabel(state.detectedSource?.let { "Detected: ${languageName(it)}" } ?: "Detected automatically")
                TextField(
                    value = state.testInput,
                    onValueChange = vm::setTestInput,
                    minLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            InfoBox(m) {
                BoxLabel(languageName(state.target))
                AnimatedContent(
                    targetState = state.testRunning,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false) },
                    label = "testOutput",
                ) { running ->
                    if (running) {
                        Column(Modifier.fillMaxWidth()) {
                            ShimmerLine(1f)
                            ShimmerLine(0.6f)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            when {
                                state.testError != null -> Text(
                                    state.testError,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )

                                state.testOutput != null -> Text(state.testOutput, style = MaterialTheme.typography.bodyLarge)
                                else -> Text(
                                    "Press Translate to try the selected engine.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.testTiming?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A loading placeholder line with a highlight sweeping across it. */
@Composable
private fun ShimmerLine(widthFraction: Float) {
    val colors = MaterialTheme.colorScheme
    val sweep by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerSweep",
    )
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .padding(vertical = 6.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        listOf(colors.surfaceContainerHighest, colors.surface, colors.surfaceContainerHighest),
                        startX = size.width * (sweep - 0.5f),
                        endX = size.width * (sweep + 0.5f),
                    )
                )
            }
    )
}
