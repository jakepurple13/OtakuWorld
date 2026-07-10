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

data class BackupWizardUiState(
    val step: BackupWizardStep = BackupWizardStep.SelectItems,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

class BackupWizardViewModel<F>(
    uiInfos: List<BackupUiInfo>,
    private val resultsFlow: Flow<List<ItemResult>> = emptyFlow(),
    private val startBackup: (F, Set<String>) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BackupWizardUiState(items = uiInfos.map { WizardItemState(uiInfo = it) })
    )
    val state: StateFlow<BackupWizardUiState> = _state.asStateFlow()

    fun toggleSelected(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(selected = !it.selected) else it })
        }
    }

    fun toggleExpanded(key: String) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(expanded = !it.expanded) else it })
        }
        loadSummaryIfNeeded(key)
    }

    fun selectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = true) }) }
    }

    fun deselectAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(selected = false) }) }
    }

    fun goToReview() {
        _state.update { s ->
            s.copy(step = BackupWizardStep.Review, items = s.items.filter { it.selected })
        }
        _state.value.items.forEach { if (it.summary == null) loadSummaryIfNeeded(it.uiInfo.key) }
    }

    fun confirm(file: F) {
        val keys = _state.value.items.map { it.uiInfo.key }.toSet()
        _state.update { it.copy(step = BackupWizardStep.Executing) }
        startBackup(file, keys)
        viewModelScope.launch {
            resultsFlow.collect { results ->
                _state.update { it.copy(results = results) }
                if (results.map { r -> r.key }.toSet() == keys) {
                    _state.update { it.copy(step = BackupWizardStep.Complete) }
                }
            }
        }
    }

    private fun loadSummaryIfNeeded(key: String) {
        val current = _state.value.items.find { it.uiInfo.key == key } ?: return
        if (current.summary != null) return
        viewModelScope.launch {
            val summary = current.uiInfo.currentSummary()
            _state.update { s ->
                s.copy(items = s.items.map { if (it.uiInfo.key == key) it.copy(summary = summary) else it })
            }
        }
    }
}
