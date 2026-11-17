package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportError
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportState
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportedModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_import_cancel
import otakuworld.kmpuiviews.generated.resources.ai_import_checking
import otakuworld.kmpuiviews.generated.resources.ai_import_checking_desc
import otakuworld.kmpuiviews.generated.resources.ai_import_copying
import otakuworld.kmpuiviews.generated.resources.ai_import_dismiss
import otakuworld.kmpuiviews.generated.resources.ai_import_drop
import otakuworld.kmpuiviews.generated.resources.ai_import_drop_hot
import otakuworld.kmpuiviews.generated.resources.ai_import_err_copy
import otakuworld.kmpuiviews.generated.resources.ai_import_err_load
import otakuworld.kmpuiviews.generated.resources.ai_import_err_load_desc
import otakuworld.kmpuiviews.generated.resources.ai_import_err_space
import otakuworld.kmpuiviews.generated.resources.ai_import_err_space_desc
import otakuworld.kmpuiviews.generated.resources.ai_import_err_type
import otakuworld.kmpuiviews.generated.resources.ai_import_err_type_desc
import otakuworld.kmpuiviews.generated.resources.ai_import_model
import otakuworld.kmpuiviews.generated.resources.ai_import_none
import otakuworld.kmpuiviews.generated.resources.ai_import_options
import otakuworld.kmpuiviews.generated.resources.ai_import_progress
import otakuworld.kmpuiviews.generated.resources.ai_import_remove
import otakuworld.kmpuiviews.generated.resources.ai_import_replace
import otakuworld.kmpuiviews.generated.resources.ai_import_size
import otakuworld.kmpuiviews.generated.resources.ai_import_try_another
import otakuworld.kmpuiviews.generated.resources.ai_import_where

/** Gemma 3 1B is gated on Hugging Face: the user signs in and accepts the license there. */
private const val MODEL_HELP_URL = "https://huggingface.co/litert-community/Gemma3-1B-IT"

// Decimal, like the system file picker shows sizes
private const val MB = 1_000_000L

/** The ⋮ menu for the imported model, shown at the end of the local engine's row. */
@Composable
internal fun LocalModelMenu(
    onReplace: () -> Unit,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, stringResource(Res.string.ai_import_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.ai_import_replace)) },
                onClick = { open = false; onReplace() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.ai_import_remove)) },
                onClick = { open = false; onRemove() },
            )
        }
    }
}

/** A line under the local engine's name: "584 MB · imported". */
@Composable
internal fun LocalModelSize(model: ImportedModel) {
    Text(
        stringResource(Res.string.ai_import_size, (model.sizeBytes / MB).toInt()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Opens the file picker; the import replaces the current model when [replace] was set for that launch. */
internal class ModelPicker(val launch: (replace: Boolean) -> Unit)

@Composable
internal fun rememberModelPicker(onPicked: (PlatformFile, replace: Boolean) -> Unit): ModelPicker {
    var replace by remember { mutableStateOf(false) }
    // Android has no MIME type for .litertlm, so filtering there would hide the file; the import checks the name
    val launcher = rememberFilePickerLauncher(
        type = if (supportsModelFileDrop) FileKitType.File(listOf("litertlm")) else FileKitType.File(),
    ) { file -> file?.let { onPicked(it, replace) } }
    return remember(launcher) {
        ModelPicker { r ->
            replace = r
            launcher.launch()
        }
    }
}

/**
 * The import part of the local engine's row: the button (and drop zone on desktop) when there is no
 * model, then copy progress, the check, or what went wrong.
 */
@Composable
internal fun LocalModelImport(
    state: ImportState,
    hasModel: Boolean,
    /** The engine already runs something (e.g. an AI Helper download), so "no model yet" would be wrong. */
    engineReady: Boolean,
    picker: ModelPicker,
    onDrop: (PlatformFile) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { (fadeIn() + slideInVertically { it / 8 }) togetherWith fadeOut() },
        label = "modelImport",
        modifier = modifier.fillMaxWidth(),
    ) { s ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (s) {
                ImportState.Idle -> if (!hasModel) {
                    if (!engineReady) {
                        Text(
                            stringResource(Res.string.ai_import_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (supportsModelFileDrop) DropZone(onDrop)
                    ImportActions(stringResource(Res.string.ai_import_model)) { picker.launch(false) }
                }

                is ImportState.Copying -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.ai_import_copying, s.name),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onCancel) { Text(stringResource(Res.string.ai_import_cancel)) }
                    }
                    val progress = if (s.total > 0) s.copied.toFloat() / s.total else 0f
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Row {
                        Text(
                            stringResource(Res.string.ai_import_progress, (s.copied / MB).toInt(), (s.total / MB).toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is ImportState.Checking -> {
                    Text(stringResource(Res.string.ai_import_checking), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(Res.string.ai_import_checking_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is ImportState.Failed -> {
                    ImportErrorBox(s.reason)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ImportActions(
                            stringResource(Res.string.ai_import_try_another),
                            modifier = Modifier.weight(1f),
                        ) { picker.launch(hasModel) }
                        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ai_import_dismiss)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportActions(label: String, modifier: Modifier = Modifier, onImport: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        FilledTonalButton(onClick = onImport) {
            Icon(Icons.Default.FileUpload, null, modifier = Modifier.padding(end = 6.dp))
            Text(label)
        }
        TextButton(onClick = { uriHandler.openUri(MODEL_HELP_URL) }) {
            Text(stringResource(Res.string.ai_import_where))
        }
    }
}

@Composable
private fun DropZone(onDrop: (PlatformFile) -> Unit) {
    var hot by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (hot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "dropBorder",
    )
    val fill by animateColorAsState(
        if (hot) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "dropFill",
    )
    Surface(
        color = fill,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, border, MaterialTheme.shapes.large)
            .modelFileDrop(onHover = { hot = it }, onFile = onDrop),
    ) {
        Text(
            stringResource(if (hot) Res.string.ai_import_drop_hot else Res.string.ai_import_drop),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun ImportErrorBox(error: ImportError) {
    val (title, detail) = when (error) {
        ImportError.NotAModel -> stringResource(Res.string.ai_import_err_type) to
                stringResource(Res.string.ai_import_err_type_desc)
        is ImportError.NoSpace -> stringResource(Res.string.ai_import_err_space) to
                stringResource(Res.string.ai_import_err_space_desc, (error.needed / MB).toInt(), (error.available / MB).toInt())
        is ImportError.WontLoad -> stringResource(Res.string.ai_import_err_load) to
                stringResource(Res.string.ai_import_err_load_desc)
        is ImportError.CopyFailed -> stringResource(Res.string.ai_import_err_copy) to error.message.orEmpty()
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
