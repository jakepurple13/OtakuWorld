package com.programmersbox.kmpuiviews.translation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.ConfirmActions
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.SectionLabel
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.SmallChip
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.formatModelSize
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.groupShape
import com.programmersbox.kmpuiviews.translation.ollama.OllamaModel
import com.programmersbox.kmpuiviews.translation.ollama.OllamaStatus

// ---------- shared bits ----------

/** A row in a grouped list inside an engine panel: `surface` on the panel's `surfaceContainer`. */
@Composable
private fun PanelRow(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}

/** Swaps a row for its delete confirmation in place, like the Android sheet. */
@Composable
private fun ConfirmableRow(
    confirming: Boolean,
    shape: Shape,
    title: String,
    body: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = confirming,
        transitionSpec = {
            fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = true)
        },
        label = "rowConfirm",
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
    ) { showConfirm ->
        if (showConfirm) {
            val colors = MaterialTheme.colorScheme
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(title) }
                        append(" ")
                        append(body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onErrorContainer,
                )
                ConfirmActions(confirmText = "Delete", onCancel = onCancel, onConfirm = onConfirm)
            }
        } else {
            PanelRow(RoundedCornerShape(0.dp)) { content() }
        }
    }
}

/** "● Connected · …" in primary, or the problem in error. */
@Composable
internal fun StatusLine(text: String, ok: Boolean, modifier: Modifier = Modifier) {
    val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun PanelNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A text field with its action button beside it, bottoms aligned. */
@Composable
private fun FieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    buttonText: String,
    onButton: () -> Unit,
    buttonEnabled: Boolean = true,
    placeholder: String? = null,
    leadingButtonIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (buttonEnabled) onButton() }),
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(onClick = onButton, enabled = buttonEnabled, modifier = Modifier.padding(bottom = 4.dp)) {
            leadingButtonIcon?.invoke()
            Text(buttonText)
        }
    }
}

// ---------- Ollama ----------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ColumnScope.OllamaPanel(state: DesktopTranslationState, vm: DesktopTranslationViewModel) {
    FieldRow(
        value = state.ollamaUrl,
        onValueChange = vm::setOllamaUrl,
        label = "Ollama server",
        buttonText = "Check",
        onButton = vm::checkOllama,
        buttonEnabled = !state.ollamaChecking,
    )
    val status = state.ollamaStatus
    AnimatedContent(
        targetState = when {
            state.ollamaChecking || status == null -> "Checking…" to true
            status is OllamaStatus.Connected -> {
                val n = status.models.size
                "Connected · Ollama ${status.version} · $n ${if (n == 1) "model" else "models"}" to true
            }

            else -> "Can't reach Ollama at this address. Is it running?" to false
        },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ollamaStatus",
    ) { (text, ok) -> StatusLine(text, ok) }

    SectionLabel("Model for translation", Modifier.padding(top = 4.dp))
    val models = state.ollamaModels
    if (status is OllamaStatus.Connected && models.isEmpty()) {
        PanelNote("No models yet. Pull one below, like gemma3:4b.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        models.forEachIndexed { i, model ->
            key(model.name) {
                ConfirmableRow(
                    confirming = state.confirmingId == ollamaDeleteId(model.name),
                    shape = groupShape(i, models.size),
                    title = "Delete ${model.name} from Ollama?",
                    body = "Frees ${formatModelSize(model.sizeBytes)}. Other apps that use Ollama lose it too.",
                    onCancel = vm::cancelDelete,
                    onConfirm = vm::confirmDelete,
                ) {
                    OllamaModelRow(model, inUse = model.name == state.ollamaModel, vm = vm)
                }
            }
        }
    }

    val pull = state.pull
    FieldRow(
        value = state.pullName,
        onValueChange = vm::setPullName,
        label = "Pull another model",
        placeholder = "e.g. gemma3:4b",
        buttonText = "Pull",
        onButton = vm::pullModel,
        buttonEnabled = pull == null && state.pullName.isNotBlank() && status is OllamaStatus.Connected,
    )
    AnimatedVisibility(
        visible = pull != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val fraction = pull?.fraction
                Text(
                    "Pulling ${pull?.name.orEmpty()}" + (fraction?.let { " · ${(it * 100).toInt()}%" } ?: "…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (fraction == null) {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    val animated by animateFloatAsState(fraction, label = "pullProgress")
                    LinearWavyProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
                }
            }
            TextButton(onClick = vm::cancelPull) { Text("Cancel") }
        }
    }
}

@Composable
private fun OllamaModelRow(model: OllamaModel, inUse: Boolean, vm: DesktopTranslationViewModel) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(end = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .selectable(selected = inUse, role = Role.RadioButton, onClick = { vm.selectOllamaModel(model.name) })
                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            RadioButton(selected = inUse, onClick = null, modifier = Modifier.padding(horizontal = 10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    AnimatedVisibility(inUse, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        SmallChip("In use", colors.primary, colors.onPrimary)
                    }
                }
                Text(formatModelSize(model.sizeBytes), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        IconButton(onClick = { vm.askDelete(ollamaDeleteId(model.name)) }) {
            Icon(Icons.Default.Delete, "Delete ${model.name}")
        }
    }
}

// ---------- Custom ----------

/** Any OpenAI-compatible server, like LM Studio or llama.cpp. */
@Composable
internal fun ColumnScope.CustomPanel(state: DesktopTranslationState, vm: DesktopTranslationViewModel) {
    FieldRow(
        value = state.serverUrl,
        onValueChange = vm::setServerUrl,
        label = "Server URL",
        buttonText = "Check",
        onButton = vm::checkServer,
        buttonEnabled = state.serverCheck != ServerCheck.Checking,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.serverModel,
            onValueChange = vm::setServerModel,
            label = { Text("Model name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = state.serverKey,
            onValueChange = vm::setServerKey,
            label = { Text("API key (optional)") },
            placeholder = { Text("Leave empty for local servers") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.weight(1f),
        )
    }
    val check = state.serverCheck
    AnimatedContent(
        targetState = check,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "serverCheck",
    ) { c ->
        when (c) {
            null -> PanelNote(
                if (state.serverModel.isBlank()) "Enter the model name your server loaded, then Check."
                else "Press Check to test the connection."
            )

            ServerCheck.Checking -> StatusLine("Checking…", ok = true)
            is ServerCheck.Connected -> StatusLine(
                "Connected · ${c.models} ${if (c.models == 1) "model" else "models"} loaded",
                ok = true,
            )

            is ServerCheck.Failed -> StatusLine(c.message, ok = false)
        }
    }
}

/** Panel container: `surfaceContainer`, like the page's other boxes. */
@Composable
internal fun EnginePanelSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}
