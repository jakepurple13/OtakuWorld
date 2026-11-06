package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationLanguage
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.domain.TranslationModel
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationSheetState(
    val available: Boolean,
    val target: TranslationLanguage,
    val downloaded: List<TranslationModel>, // Downloaded, required first, then by name
    val notDownloaded: List<TranslationModel>, // filtered by query
    val totalBytes: Long,
    val pickerOpen: Boolean,
    val pickerLanguages: List<TranslationModel>, // all, filtered by query
    val query: String,
    val confirmingCode: String?,
    val selecting: Boolean,
    val selected: Set<String>,
    val selectedBytes: Long,
    val confirmingBulk: Boolean,
)

private data class UiState(
    val pickerOpen: Boolean = false,
    val query: String = "",
    val confirmingCode: String? = null,
    val selecting: Boolean = false,
    val selected: Set<String> = emptySet(),
    val confirmingBulk: Boolean = false,
)

private val EnglishFallback = TranslationLanguage("en", "English", "English", "En")

class TranslationViewModel(
    private val handler: TranslationModelHandler,
    private val settings: NewSettingsHandling,
    private val notifier: InAppNotifier,
) : ViewModel() {

    private val ui = MutableStateFlow(UiState())

    val state: StateFlow<TranslationSheetState> = combine(
        handler.models,
        settings.translationTargetLanguage.asFlow(),
        ui,
    ) { models, targetCode, ui ->
        val q = ui.query.trim()
        fun TranslationModel.matches() = q.isEmpty() ||
                language.englishName.contains(q, ignoreCase = true) ||
                language.nativeName.contains(q, ignoreCase = true)

        val downloaded = models
            .filter { it.state is ModelState.Downloaded }
            .sortedWith(compareByDescending<TranslationModel> { it.required }.thenBy { it.language.englishName })
        TranslationSheetState(
            available = handler.isAvailable,
            target = TranslationLanguages.byCode(targetCode)
                ?: TranslationLanguages.byCode("en")
                ?: EnglishFallback,
            downloaded = downloaded,
            notDownloaded = models.filter { it.state !is ModelState.Downloaded && it.matches() },
            totalBytes = downloaded.sumOf { it.sizeBytes ?: 0L },
            pickerOpen = ui.pickerOpen,
            pickerLanguages = models.filter { it.matches() },
            query = ui.query,
            confirmingCode = ui.confirmingCode,
            selecting = ui.selecting,
            selected = ui.selected,
            selectedBytes = downloaded.filter { it.language.code in ui.selected }.sumOf { it.sizeBytes ?: 0L },
            confirmingBulk = ui.confirmingBulk,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TranslationSheetState(
            available = handler.isAvailable,
            target = TranslationLanguages.byCode("en") ?: EnglishFallback,
            downloaded = emptyList(),
            notDownloaded = emptyList(),
            totalBytes = 0L,
            pickerOpen = false,
            pickerLanguages = emptyList(),
            query = "",
            confirmingCode = null,
            selecting = false,
            selected = emptySet(),
            selectedBytes = 0L,
            confirmingBulk = false,
        ),
    )

    init {
        viewModelScope.launch {
            try {
                handler.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifier.error(title = "Couldn't load translation models", subtitle = e.message)
            }
        }
    }

    private fun selectableCodes() = handler.models.value
        .filter { it.state is ModelState.Downloaded && !it.required }
        .map { it.language.code }
        .toSet()

    fun openPicker() = ui.update { it.copy(pickerOpen = true, query = "") }
    fun closePicker() = ui.update { it.copy(pickerOpen = false, query = "") }

    fun setTarget(code: String) {
        ui.update { it.copy(pickerOpen = false, query = "") }
        viewModelScope.launch { settings.translationTargetLanguage.set(code) }
        val model = handler.models.value.firstOrNull { it.language.code == code }
        if (model != null && model.state !is ModelState.Downloaded && model.state !is ModelState.Downloading) {
            handler.download(code)
        }
    }

    fun askDelete(code: String) {
        if (code !in selectableCodes()) return
        ui.update { it.copy(confirmingCode = code) }
    }
    fun cancelDelete() = ui.update { it.copy(confirmingCode = null) }

    fun confirmDelete() {
        val code = ui.value.confirmingCode ?: return
        ui.update { it.copy(confirmingCode = null) }
        deleteCodes(setOf(code))
    }

    fun enterSelect(code: String? = null) = ui.update {
        it.copy(
            selecting = true,
            selected = if (code != null && code in selectableCodes()) setOf(code) else emptySet(),
        )
    }

    fun exitSelect() = ui.update { it.copy(selecting = false, selected = emptySet(), confirmingBulk = false) }

    fun toggle(code: String) {
        if (code !in selectableCodes()) return
        ui.update { it.copy(selected = if (code in it.selected) it.selected - code else it.selected + code) }
    }

    fun selectAll() {
        val all = selectableCodes()
        ui.update { it.copy(selected = if (it.selected == all) emptySet() else all) }
    }

    fun askBulkDelete() = ui.update { if (it.selected.isEmpty()) it else it.copy(confirmingBulk = true) }
    fun cancelBulkDelete() = ui.update { it.copy(confirmingBulk = false) }

    fun confirmBulkDelete() {
        val codes = ui.value.selected
        ui.update { it.copy(confirmingBulk = false, selecting = false, selected = emptySet()) }
        if (codes.isNotEmpty()) deleteCodes(codes)
    }

    fun download(code: String) = handler.download(code)
    fun cancelDownload(code: String) = handler.cancelDownload(code)

    fun setQuery(q: String) = ui.update { it.copy(query = q) }

    /** @return true if the back press was consumed. */
    fun onBack(): Boolean {
        val s = ui.value
        when {
            s.confirmingBulk -> cancelBulkDelete()
            s.selecting -> exitSelect()
            s.pickerOpen -> closePicker()
            s.confirmingCode != null -> cancelDelete()
            else -> return false
        }
        return true
    }

    private fun deleteCodes(codes: Set<String>) {
        viewModelScope.launch {
            try {
                handler.delete(codes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifier.error(title = "Couldn't delete", subtitle = e.message)
                return@launch
            }
            if (settings.translationTargetLanguage.get() in codes) {
                settings.translationTargetLanguage.set("en")
            }
        }
    }
}
