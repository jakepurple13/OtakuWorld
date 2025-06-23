package com.programmersbox.kmpuiviews.presentation.recommendations

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.AiService
import com.programmersbox.datastore.AiSettings
import com.programmersbox.datastore.AnthropicSettings
import com.programmersbox.datastore.GeminiSettings
import com.programmersbox.datastore.OpenAiSettings
import com.programmersbox.kmpuiviews.utils.composables.modifiers.privacySensitive
import com.xemantic.ai.anthropic.Model

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiSettings(
    onDismissRequest: () -> Unit,
    aiSettings: AiSettings,
    onSave: (AiSettings) -> Unit,
) {
    var currentSettings by remember(aiSettings) { mutableStateOf(aiSettings) }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Scaffold(
            bottomBar = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                ) {
                    Button(
                        onClick = { onSave(currentSettings) },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text("Save") }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            currentSettings.aiService.name,
                            onValueChange = { currentSettings = currentSettings.copy(aiService = AiService.valueOf(it)) },
                            label = { Text("AI Service") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            AiService.entries.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    onClick = {
                                        currentSettings = currentSettings.copy(aiService = it)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Crossfade(currentSettings.aiService) { target ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (target) {
                                AiService.Gemini -> GeminiSettings(
                                    geminiSettings = currentSettings.geminiSettings ?: GeminiSettings(),
                                    onModify = { currentSettings = currentSettings.copy(geminiSettings = it) }
                                )

                                AiService.OpenAi -> OpenAiSettings(
                                    openAiSettings = currentSettings.openAiSettings ?: OpenAiSettings(),
                                    onModify = { currentSettings = currentSettings.copy(openAiSettings = it) }
                                )

                                AiService.Anthropic -> AnthropicSettings(
                                    anthropicSettings = currentSettings.anthropicSettings ?: AnthropicSettings(),
                                    onModify = { currentSettings = currentSettings.copy(anthropicSettings = it) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        currentSettings.prompt,
                        onValueChange = { currentSettings = currentSettings.copy(prompt = it) },
                        label = { Text("Prompt (BE VERY CAREFUL ABOUT MODIFYING THIS! THINGS COULD BREAK!)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    currentKey: String,
    onModify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showApiKey by remember { mutableStateOf(false) }

    OutlinedTextField(
        currentKey,
        onValueChange = { onModify(it) },
        label = { Text("API Key") },
        trailingIcon = {
            IconButton(
                onClick = { showApiKey = !showApiKey }
            ) { Icon(Icons.Default.Visibility, null) }
        },
        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = modifier.privacySensitive()
    )
}

@Composable
private fun GeminiSettings(
    geminiSettings: GeminiSettings,
    onModify: (GeminiSettings) -> Unit,
) {

    ApiKeyField(
        currentKey = geminiSettings.apiKey,
        onModify = { onModify(geminiSettings.copy(apiKey = it)) },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        geminiSettings.modelName,
        onValueChange = { onModify(geminiSettings.copy(modelName = it)) },
        label = { Text("Model") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OpenAiSettings(
    openAiSettings: OpenAiSettings,
    onModify: (OpenAiSettings) -> Unit,
) {
    ApiKeyField(
        currentKey = openAiSettings.apiKey,
        onModify = { onModify(openAiSettings.copy(apiKey = it)) },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        openAiSettings.modelName,
        onValueChange = { onModify(openAiSettings.copy(modelName = it)) },
        label = { Text("Model") },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnthropicSettings(
    anthropicSettings: AnthropicSettings,
    onModify: (AnthropicSettings) -> Unit,
) {
    ApiKeyField(
        currentKey = anthropicSettings.apiKey,
        onModify = { onModify(anthropicSettings.copy(apiKey = it)) },
        modifier = Modifier.fillMaxWidth()
    )

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            anthropicSettings.modelName,
            onValueChange = { onModify(anthropicSettings.copy(modelName = it)) },
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Model.entries.forEach {
                DropdownMenuItem(
                    text = { Text(it.id) },
                    onClick = {
                        onModify(anthropicSettings.copy(modelName = it.id))
                        expanded = false
                    }
                )
            }
        }
    }
}
