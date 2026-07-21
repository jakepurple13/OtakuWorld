package com.programmersbox.koogintegration.screens.settings

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.AnthropicLLMProvider
import ai.koog.prompt.llm.DeepSeekLLMProvider
import ai.koog.prompt.llm.GoogleLLMProvider
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.MistralAILLMProvider
import ai.koog.prompt.llm.OllamaLLMProvider
import ai.koog.prompt.llm.OpenAILLMProvider
import ai.koog.prompt.llm.OpenRouterLLMProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.KoogDataStore
import com.programmersbox.koogintegration.ModelManager
import com.programmersbox.koogintegration.NoLLMProvider
import com.programmersbox.koogintegration.canDownloadModel
import com.programmersbox.koogintegration.getModelLinkToDownload
import com.programmersbox.koogintegration.platformModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KoogSettingsScreen(
    viewModel: KoogSettingsViewModel = koinViewModel(),
    onBack: () -> Unit = {},
) {
    val state = viewModel.screenState
    val downloadState = viewModel.modelDownloadState
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
                    onProviderSelected = { viewModel.updateCompany(it.display) }
                )
            }

            if (downloadState.isDownloading) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        LinearWavyProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${downloadState.downloadedBytes}/${downloadState.totalBytes}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            item {
                AIModelDropdown(
                    selectedProvider = state.modelName,
                    onProviderSelected = { viewModel.updateModelName(it) },
                    canDownload = canDownloadModel(state.modelCompany.display),
                    isModelDownloaded = state.hasModelDownloaded,
                    downloadModel = viewModel::downloadModel,
                    modelList = when (state.modelCompany) {
                        OpenAILLMProvider -> OpenAIModels.models
                        AnthropicLLMProvider -> AnthropicModels.models
                        GoogleLLMProvider -> GoogleModels.models
                        DeepSeekLLMProvider -> DeepSeekModels.models
                        OpenRouterLLMProvider -> OpenRouterModels.models
                        MistralAILLMProvider -> MistralAIModels.models
                        OllamaLLMProvider -> OllamaModels.models
                        NoLLMProvider -> emptyList()
                        else -> platformModels(state.modelCompany.display)
                    }
                )
            }
        }
    }
}

class KoogSettingsViewModel(
    private val koogDataStore: KoogDataStore,
    private val agentMaker: AgentMaker,
    private val modelManager: ModelManager,
) : ViewModel() {
    var screenState by mutableStateOf(SettingScreenState())
    var modelDownloadState by mutableStateOf(ModelDownloadState())

    init {
        combine(
            koogDataStore.apiKeyFlow,
            koogDataStore.modelCompanyFlow,
            koogDataStore.modelNameFlow
        ) { (apiKey, company, model) ->
            SettingScreenState(
                apiKey = apiKey,
                modelCompany = agentMaker.mapStringToProvider(company),
                modelName = model,
                hasModelDownloaded = modelManager.hasModelDownloaded(model)
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

    fun downloadModel(
        id: String,
    ) {
        val link = getModelLinkToDownload(id) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            modelManager.getOrDownloadModel(
                modelUrl = link,
                fileName = id,
                onProgress = { bytesDownloaded, totalBytes ->
                    modelDownloadState = modelDownloadState.copy(
                        progress = bytesDownloaded.toFloat() / (totalBytes?.toFloat() ?: 1f),
                        totalBytes = totalBytes ?: 0L,
                        downloadedBytes = bytesDownloaded,
                        isDownloading = true
                    )
                }
            )
            modelDownloadState = ModelDownloadState()
        }
    }
}

@Stable
data class SettingScreenState(
    val apiKey: String = "",
    val modelCompany: LLMProvider = NoLLMProvider,
    val modelName: String = "",
    val hasModelDownloaded: Boolean = false,
)

@Stable
data class ModelDownloadState(
    val progress: Float = 0f,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val isDownloading: Boolean = false,
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
    selectedProvider: LLMProvider,
    onProviderSelected: (LLMProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val agentMaker = koinInject<AgentMaker>()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedProvider.display,
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
            agentMaker.providerList.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(text = provider.display) },
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
    canDownload: Boolean = false,
    isModelDownloaded: Boolean = false,
    downloadModel: (String) -> Unit = {},
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
            leadingIcon = if (canDownload) {
                {
                    IconButton(
                        onClick = { downloadModel(selectedProvider) },
                        enabled = !isModelDownloaded
                    ) { Icon(Icons.Default.Download, null) }
                }
            } else null,
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