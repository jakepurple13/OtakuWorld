package com.programmersbox.koogintegration.screens.settings

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.koogintegration.AIProvider
import com.programmersbox.koogintegration.KoogDataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KoogSettingsScreen(
    viewModel: KoogSettingsViewModel = koinViewModel(),
    onBack: () -> Unit = {},
) {
    val state = viewModel.screenState
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Otaku AI Helper") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
        ) {
            item {
                PreferenceTextField(
                    value = state.apiKey,
                    onValueChange = { viewModel.screenState = viewModel.screenState.copy(apiKey = it) },
                    label = "API Key",
                    onSave = { viewModel.updateApiKey(state.apiKey) }
                )
            }

            item {
                AIProviderDropdown(
                    selectedProvider = state.modelCompany,
                    onProviderSelected = { viewModel.updateCompany(it.name) }
                )
            }

            item {
                AIModelDropdown(
                    selectedProvider = state.modelName,
                    onProviderSelected = { viewModel.updateModelName(it) },
                    modelList = when (state.modelCompany) {
                        AIProvider.OPEN_AI -> OpenAIModels.models
                        AIProvider.ANTHROPIC -> AnthropicModels.models
                        AIProvider.GOOGLE -> GoogleModels.models
                        AIProvider.DEEP_SEEK -> DeepSeekModels.models
                        AIProvider.OPEN_ROUTER -> OpenRouterModels.models
                        AIProvider.MISTRAL -> MistralAIModels.models
                        AIProvider.OLLAMA -> OllamaModels.models
                        AIProvider.NONE -> emptyList()
                    }
                )
            }
        }
    }
}

class KoogSettingsViewModel(
    private val koogDataStore: KoogDataStore,
) : ViewModel() {
    var screenState by mutableStateOf(SettingScreenState())

    init {
        combine(
            koogDataStore.apiKeyFlow,
            koogDataStore.modelCompanyFlow,
            koogDataStore.modelNameFlow
        ) { (apiKey, company, model) ->
            SettingScreenState(
                apiKey = apiKey,
                modelCompany = runCatching { AIProvider.valueOf(company) }.getOrElse { AIProvider.NONE },
                modelName = model
            )
        }
            .onEach { screenState = it }
            .launchIn(viewModelScope)
    }

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch { koogDataStore.storeApiKey(apiKey) }
    }

    fun updateCompany(company: String) {
        viewModelScope.launch { koogDataStore.storeModelCompany(company) }
    }

    fun updateModelName(modelName: String) {
        viewModelScope.launch { koogDataStore.storeModelName(modelName) }
    }
}

@Stable
data class SettingScreenState(
    val apiKey: String = "",
    val modelCompany: AIProvider = AIProvider.NONE,
    val modelName: String = "",
)


@Composable
fun PreferenceTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = onSave
            ) { Icon(Icons.Default.Save, null) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AIProviderDropdown(
    selectedProvider: AIProvider,
    onProviderSelected: (AIProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedProvider.displayName,
            onValueChange = {},
            label = { Text("Select AI Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            // The menuAnchor modifier must be applied to the text field inside the box
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AIProvider.entries.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(text = provider.displayName) },
                    onClick = {
                        onProviderSelected(provider)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AIModelDropdown(
    selectedProvider: String,
    modelList: List<LLModel>,
    onProviderSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedProvider,
            onValueChange = {},
            label = { Text("Select AI Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            // The menuAnchor modifier must be applied to the text field inside the box
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modelList.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(text = provider.id) },
                    onClick = {
                        onProviderSelected(provider.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}