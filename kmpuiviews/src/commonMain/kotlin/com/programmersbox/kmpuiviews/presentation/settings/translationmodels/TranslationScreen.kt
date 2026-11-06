package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.translation_download_for_offline
import otakuworld.kmpuiviews.generated.resources.translation_subtitle
import otakuworld.kmpuiviews.generated.resources.translation_title
import otakuworld.kmpuiviews.generated.resources.translation_unavailable
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun showTranslationScreen(): MutableState<Boolean> {
    val showTranslationScreen = remember { mutableStateOf(false) }

    if (showTranslationScreen.value) {
        val vm: TranslationViewModel = koinViewModel()
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            // Swipe-down / scrim tap backs out of select mode, the picker or a confirm before closing.
            confirmValueChange = { it != SheetValue.Hidden || !vm.onBack() },
        )
        ModalBottomSheet(
            onDismissRequest = {
                vm.exitSelect()
                vm.closePicker()
                vm.cancelDelete()
                showTranslationScreen.value = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            TranslationScreen(vm)
        }
    }

    return showTranslationScreen
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TranslationScreen(vm: TranslationViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(
        enabled = state.pickerOpen || state.selecting || state.confirmingCode != null || state.confirmingBulk,
    ) { vm.onBack() }

    if (!state.available) {
        UnavailableState()
        return
    }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.pickerOpen,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                }
            },
            label = "mainPicker",
        ) { picker ->
            if (picker) {
                LanguagePicker(
                    languages = state.pickerLanguages,
                    targetCode = state.target.code,
                    query = state.query,
                    onQueryChange = vm::setQuery,
                    onBack = vm::closePicker,
                    onPick = vm::setTarget,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MainList(state = state, vm = vm, listState = listState)
            }
        }

        BulkDeleteBar(
            visible = !state.pickerOpen && state.selecting && state.selected.isNotEmpty(),
            selectedModels = state.downloaded.filter { it.language.code in state.selected },
            selectedBytes = state.selectedBytes,
            confirming = state.confirmingBulk,
            onAsk = vm::askBulkDelete,
            onCancel = vm::cancelBulkDelete,
            onConfirm = vm::confirmBulkDelete,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MainList(
    state: TranslationSheetState,
    vm: TranslationViewModel,
    listState: LazyListState,
) {
    val nowMillis = remember { Clock.System.now().toEpochMilliseconds() }
    val selectable = state.downloaded.filter { !it.required }.map { it.language.code }.toSet()
    // Room for the bulk bar so the last rows can scroll above it.
    val bottomPadding by animateDpAsState(if (state.selecting) 104.dp else 24.dp, label = "bottomPad")

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "title") {
            Column(
                Modifier
                    .animateItem()
                    .padding(top = 4.dp, bottom = 14.dp)
            ) {
                Text(stringResource(Res.string.translation_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(Res.string.translation_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "target") {
            TargetLanguageCard(target = state.target, onClick = vm::openPicker, modifier = Modifier.animateItem())
        }
        item(key = "meter") {
            StorageMeter(
                downloaded = state.downloaded,
                totalBytes = state.totalBytes,
                selected = state.selected,
                selectedBytes = state.selectedBytes,
                selecting = state.selecting,
                modifier = Modifier
                    .animateItem()
                    .padding(top = 12.dp),
            )
        }
        item(key = "downloaded-header") {
            DownloadedHeader(
                selecting = state.selecting,
                selectedCount = state.selected.size,
                canSelect = selectable.isNotEmpty(),
                allSelected = selectable.isNotEmpty() && state.selected == selectable,
                onSelect = { vm.enterSelect() },
                onSelectAll = vm::selectAll,
                onDone = vm::exitSelect,
                modifier = Modifier
                    .animateItem()
                    .padding(top = 10.dp),
            )
        }
        itemsIndexed(state.downloaded, key = { _, m -> m.language.code }) { i, model ->
            val code = model.language.code
            DownloadedRow(
                model = model,
                isTarget = state.target.code == code,
                selecting = state.selecting,
                selected = code in state.selected,
                confirming = state.confirmingCode == code,
                nowMillis = nowMillis,
                shape = groupShape(i, state.downloaded.size),
                onAskDelete = { vm.askDelete(code) },
                onCancelDelete = vm::cancelDelete,
                onConfirmDelete = vm::confirmDelete,
                onLongPress = { vm.enterSelect(code) },
                onToggle = { vm.toggle(code) },
                modifier = Modifier
                    .animateItem()
                    .padding(top = if (i > 0) 3.dp else 0.dp)
                    .fillMaxWidth(),
            )
        }
        item(key = "available-header") {
            SectionLabel(
                stringResource(Res.string.translation_download_for_offline),
                modifier = Modifier
                    .animateItem()
                    .padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
            )
        }
        item(key = "available-search") {
            LanguageSearchField(
                query = state.query,
                onQueryChange = vm::setQuery,
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = 8.dp),
            )
        }
        itemsIndexed(state.notDownloaded, key = { _, m -> m.language.code }) { i, model ->
            val code = model.language.code
            AvailableRow(
                model = model,
                shape = groupShape(i, state.notDownloaded.size),
                onDownload = { vm.download(code) },
                onCancel = { vm.cancelDownload(code) },
                modifier = Modifier
                    .animateItem()
                    .padding(top = if (i > 0) 3.dp else 0.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UnavailableState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 64.dp),
    ) {
        Icon(
            Icons.Default.Translate,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(Res.string.translation_unavailable),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
