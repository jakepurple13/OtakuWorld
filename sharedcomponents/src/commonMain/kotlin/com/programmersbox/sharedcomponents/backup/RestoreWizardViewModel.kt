package com.programmersbox.sharedcomponents.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update

data class RestoreWizardUiState<F>(
    val step: RestoreWizardStep = RestoreWizardStep.PickFile,
    val file: F? = null,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

class RestoreWizardViewModel<F>(
    private val uiInfos: List<BackupUiInfo>,
    private val peekZip: suspend (F) -> Map<String, BackupDataSummary>,
    private val resultsFlow: Flow<List<ItemResult>> = emptyFlow(),
    private val startRestore: (F, Set<String>) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreWizardUiState<F>())
    val state: StateFlow<RestoreWizardUiState<F>> = _state.asStateFlow()

    fun pickFile(file: F) {
        viewModelScope.launch {
            val summaries = peekZip(file)
            val items = uiInfos
                .filter { summaries.containsKey(it.key) }
                .map { WizardItemState(uiInfo = it, summary = summaries[it.key]) }
            _state.update { it.copy(file = file, step = RestoreWizardStep.SelectItems, items = items) }
        }
    }

    fun toggleSelected(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(selected = !it.selected) else it })
        }
    }

    fun toggleExpanded(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(expanded = !it.expanded) else it })
        }
    }

    fun selectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = true) }) }
    }

    fun deselectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = false) }) }
    }

    fun goToReview() {
        _state.update { s ->
            s.copy(step = RestoreWizardStep.Review, items = s.items.filter { it.selected })
        }
    }

    fun confirm() {
        val file = _state.value.file ?: return
        val keys = _state.value.items.map { it.uiInfo.key }.toSet()
        _state.update { it.copy(step = RestoreWizardStep.Executing) }
        startRestore(file, keys)
        viewModelScope.launch {
            resultsFlow.collect { results ->
                _state.update { it.copy(results = results) }
                if (results.map { r -> r.key }.toSet() == keys) {
                    _state.update { it.copy(step = RestoreWizardStep.Complete) }
                }
            }
        }
    }
}
