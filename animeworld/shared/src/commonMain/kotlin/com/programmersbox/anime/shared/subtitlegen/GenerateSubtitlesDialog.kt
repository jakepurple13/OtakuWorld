package com.programmersbox.anime.shared.subtitlegen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.downloads.EpisodeTitle

/**
 * Fits [this] to what the model allows: an English-only model fixes From to English, and To falls
 * back to the first enabled option when the current one isn't offered for From.
 */
fun GenerationRequest.coerced(englishOnlyModel: Boolean): GenerationRequest {
    val from = if (englishOnlyModel) fromOptions(true).first() else from
    val enabled = targetOptions(from).filter { it.enabled }.map { it.language }
    val to = enabled.firstOrNull { it.code == to.code } ?: enabled.first()
    return GenerationRequest(from, to)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateSubtitlesDialog(
    title: EpisodeTitle,
    initial: GenerationRequest,
    model: ModelSummary,
    onOpenSettings: () -> Unit,
    onGenerate: (GenerationRequest, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var request by remember(model.englishOnly) { mutableStateOf(initial.coerced(model.englishOnly)) }
    var rememberChoices by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate subtitles") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    listOfNotNull(title.show, title.episode).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageField(
                        label = "From",
                        value = request.from.name,
                        enabled = !model.englishOnly,
                        modifier = Modifier.weight(1f)
                    ) { close ->
                        fromOptions(model.englishOnly).forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.name) },
                                onClick = {
                                    request = request.copy(from = language).coerced(model.englishOnly)
                                    close()
                                }
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguageField(
                        label = "To",
                        value = request.to.name,
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    ) { close ->
                        targetOptions(request.from).forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.language.name)
                                        Text(
                                            option.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = if (option.enabled) 1f else 0.6f)
                                        )
                                    }
                                },
                                enabled = option.enabled,
                                onClick = {
                                    request = request.copy(to = option.language)
                                    close()
                                }
                            )
                        }
                    }
                }

                Text(
                    "Whisper translates into English only. Run it again with another “To” language to add a second track.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val translatesPoorly = !model.translatesWell && request.translate
                val linkColor = MaterialTheme.colorScheme.primary
                AnimatedContent(targetState = translatesPoorly, label = "modelHint") { poor ->
                    val hint = if (poor) {
                        modelHint("${model.name} translates poorly into English. ", "Choose another model", linkColor, onOpenSettings)
                    } else {
                        val name = if (model.downloaded) model.name else "${model.name} (downloads first, ${model.sizeLabel})"
                        modelHint("Model: $name · ", "Change", linkColor, onOpenSettings)
                    }
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (poor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberChoices = !rememberChoices }
                ) {
                    Checkbox(checked = rememberChoices, onCheckedChange = { rememberChoices = it })
                    Text("Use these choices next time", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGenerate(request, rememberChoices) }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun modelHint(prefix: String, link: String, linkColor: Color, onClick: () -> Unit): AnnotatedString =
    buildAnnotatedString {
        append(prefix)
        withLink(
            LinkAnnotation.Clickable(
                tag = link,
                styles = TextLinkStyles(SpanStyle(color = linkColor)),
                linkInteractionListener = { onClick() }
            )
        ) { append(link) }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageField(
    label: String,
    value: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    items: @Composable (close: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items { expanded = false }
        }
    }
}
