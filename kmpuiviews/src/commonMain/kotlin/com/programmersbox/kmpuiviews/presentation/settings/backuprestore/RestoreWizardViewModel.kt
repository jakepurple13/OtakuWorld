package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestoreWizardUiState<F>(
    val step: RestoreWizardStep = RestoreWizardStep.PickFile,
    val file: F? = null,
    val items: List<WizardItemState> = emptyList(),
    val results: List<ItemResult> = emptyList(),
)

private const val LISTS_KEY = "lists.json"

class RestoreWizardViewModel<F>(
    private val uiInfos: List<BackupUiInfo>,
    private val peekZip: suspend (F) -> Map<String, BackupDataSummary>,
    private val peekListContents: suspend (F) -> List<CustomList> = { emptyList() },
    private val resultsFlow: Flow<List<ItemResult>> = emptyFlow(),
    private val startRestore: (F, Set<String>, Set<String>?) -> Unit,
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

            if (items.any { it.uiInfo.key == LISTS_KEY }) {
                val subItems = peekListContents(file).map {
                    ListSubItemState(
                        id = it.item.uuid,
                        name = it.item.name,
                        coverUrl = it.list.firstOrNull()?.imageUrl,
                        itemCount = it.list.size,
                        requiresBiometric = it.item.useBiometric,
                    )
                }
                _state.update { s ->
                    s.copy(items = s.items.map { item -> if (item.uiInfo.key == LISTS_KEY) item.copy(subItems = subItems) else item })
                }
            }
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

    fun toggleListSelected(listId: String) {
        _state.update { s ->
            s.copy(items = s.items.map { item ->
                if (item.uiInfo.key != LISTS_KEY) item
                else item.copy(subItems = item.subItems?.map { if (it.id == listId) it.copy(selected = !it.selected) else it })
            })
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

    fun goToSelectItems() {
        _state.update { s ->
            s.copy(step = RestoreWizardStep.SelectItems)
        }
    }

    fun goToChooseFile() {
        _state.update { s ->
            s.copy(step = RestoreWizardStep.PickFile)
        }
    }

    fun confirm() {
        val file = _state.value.file ?: return
        val keys = _state.value.items
            .filter { it.selected }
            .map { it.uiInfo.key }
            .toSet()
        val selectedListIds = _state.value.items
            .find { it.uiInfo.key == LISTS_KEY }
            ?.subItems
            ?.takeIf { it.isNotEmpty() }
            ?.let { all -> all.filter { it.selected }.map { it.id }.toSet().takeIf { it.size != all.size } }
        _state.update { it.copy(step = RestoreWizardStep.Executing) }
        startRestore(file, keys, selectedListIds)
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
