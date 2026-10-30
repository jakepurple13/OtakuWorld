package com.programmersbox.kmpuiviews.presentation.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.AnimatedSelectedText
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.GradientImage
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.OptionsSheetValues
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.optionsSheet
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuImageElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuTitleElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedItemKey
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.customSharedElement
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.all
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.clear_all
import otakuworld.kmpuiviews.generated.resources.clear_all_history
import otakuworld.kmpuiviews.generated.resources.clear_all_history_body
import otakuworld.kmpuiviews.generated.resources.clear_all_history_menu
import otakuworld.kmpuiviews.generated.resources.clear_search
import otakuworld.kmpuiviews.generated.resources.clear_selection
import otakuworld.kmpuiviews.generated.resources.history
import otakuworld.kmpuiviews.generated.resources.history_empty_description
import otakuworld.kmpuiviews.generated.resources.history_empty_title
import otakuworld.kmpuiviews.generated.resources.history_item_count
import otakuworld.kmpuiviews.generated.resources.history_no_matches
import otakuworld.kmpuiviews.generated.resources.history_no_matches_description
import otakuworld.kmpuiviews.generated.resources.history_removed_count
import otakuworld.kmpuiviews.generated.resources.history_reset_filters
import otakuworld.kmpuiviews.generated.resources.history_this_week
import otakuworld.kmpuiviews.generated.resources.history_today
import otakuworld.kmpuiviews.generated.resources.history_yesterday
import otakuworld.kmpuiviews.generated.resources.more_options
import otakuworld.kmpuiviews.generated.resources.remove_from_history
import otakuworld.kmpuiviews.generated.resources.remove_selected
import otakuworld.kmpuiviews.generated.resources.removed
import otakuworld.kmpuiviews.generated.resources.search_history
import otakuworld.kmpuiviews.generated.resources.select_all
import otakuworld.kmpuiviews.generated.resources.source_not_installed
import otakuworld.kmpuiviews.generated.resources.undo
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun HistoryUi(
    vm: HistoryViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val biometrics = rememberBiometricOpening()
    val sourceRepository = LocalSourcesRepository.current
    val navController = LocalNavActions.current
    val installedSources by sourceRepository.sources.collectAsStateWithLifecycle()
    var clearAllDialog by remember { mutableStateOf(false) }
    var showLoadingDialog by remember { mutableStateOf(false) }
    var showRecentModel by remember { mutableStateOf<RecentModel?>(null) }

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    // Sources load after startup, so an empty list means "not known yet" rather than "none installed".
    fun isMissing(source: String) = installedSources.isNotEmpty() && installedSources.none { it.apiService.serviceName == source }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    val removeWithUndo: (List<RecentModel>) -> Unit = { toRemove ->
        if (toRemove.isNotEmpty()) {
            val batch = vm.removeWithUndo(toRemove)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                var undone = false
                try {
                    val message = toRemove.singleOrNull()
                        ?.let { getString(Res.string.removed, it.title) }
                        ?: getString(Res.string.history_removed_count, toRemove.size)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = getString(Res.string.undo),
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    )
                    undone = result == SnackbarResult.ActionPerformed
                    if (undone) vm.undoRemoval(batch)
                } finally {
                    // Also runs when the screen leaves composition mid-snackbar.
                    if (!undone) vm.commitRemoval(batch)
                }
            }
        }
    }

    val onError: (RecentModel) -> Unit = { item ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                "Something went wrong. Source might not be installed",
                duration = SnackbarDuration.Long,
                actionLabel = "More Options",
                withDismissAction = true
            )
            showRecentModel = when (result) {
                SnackbarResult.Dismissed -> null
                SnackbarResult.ActionPerformed -> item
            }
        }
    }

    val openItem: (RecentModel) -> Unit = { item ->
        if (isMissing(item.source)) {
            showRecentModel = item
        } else {
            scope.launch {
                biometrics.openIfNotIncognito(item.url, item.title) {
                    scope.launch {
                        sourceRepository.toSourceByApiServiceName(item.source)
                            ?.apiService
                            ?.getSourceByUrlFlow(item.url)
                            ?.dispatchIo()
                            ?.onStart { showLoadingDialog = true }
                            ?.catch {
                                showLoadingDialog = false
                                onError(item)
                            }
                            ?.onEach { m ->
                                showLoadingDialog = false
                                navController.details(m)
                            }
                            ?.collect() ?: onError(item)
                    }
                }
            }
        }
    }

    var optionsSheetItem by optionsSheet<HistoryItemOptionsSheet>(
        onOpen = { openItem(it.item) }
    ) {
        UtilityCard(
            icon = Icons.Default.Delete,
            title = stringResource(Res.string.remove_from_history),
            onClick = {
                removeWithUndo(listOf(it.item))
                dismiss()
            },
            modifier = Modifier.weight(1f)
        )
    }

    if (clearAllDialog) {
        val onDismissRequest = { clearAllDialog = false }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.DeleteForever, null) },
            title = { Text(stringResource(Res.string.clear_all_history)) },
            text = { Text(stringResource(Res.string.clear_all_history_body, vm.historyCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearAll()
                        onDismissRequest()
                    }
                ) { Text(stringResource(Res.string.clear_all), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    SourceNotInstalledModal(
        showItem = showRecentModel?.title,
        onShowItemDismiss = { showRecentModel = null },
        source = showRecentModel?.source,
        url = showRecentModel?.url,
        additionOptions = {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.remove_from_history)) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                colors = ListItemDefaults.colors(
                    headlineColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.clickable {
                    showRecentModel?.let { removeWithUndo(listOf(it)) }
                    showRecentModel = null
                }
            )
        }
    )

    BackHandler(vm.isSelecting) { vm.clearSelection() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val navHostPadding = LocalNavHostPadding.current

    OtakuHazeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(navHostPadding)
            )
        },
        topBar = {
            Column {
                AnimatedContent(
                    vm.isSelecting,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "historyTopBar"
                ) { selecting ->
                    if (selecting) {
                        TopAppBar(
                            title = { AnimatedSelectedText(vm.selected.size) },
                            navigationIcon = {
                                IconButton(onClick = vm::clearSelection) {
                                    Icon(Icons.Default.Close, stringResource(Res.string.clear_selection))
                                }
                            },
                            actions = {
                                IconButton(onClick = vm::selectAll) {
                                    Icon(Icons.Default.SelectAll, stringResource(Res.string.select_all))
                                }
                                IconButton(onClick = { removeWithUndo(vm.selectedItems()) }) {
                                    Icon(Icons.Default.Delete, stringResource(Res.string.remove_selected))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                        )
                    } else {
                        MediumFlexibleTopAppBar(
                            scrollBehavior = scrollBehavior,
                            navigationIcon = { BackButton() },
                            title = { Text(stringResource(Res.string.history)) },
                            subtitle = { Text(pluralStringResource(Res.plurals.history_item_count, vm.historyCount, vm.historyCount)) },
                            actions = {
                                HistoryOverflowMenu(
                                    enabled = vm.historyCount > 0,
                                    onSelectAll = vm::selectAll,
                                    onClearAll = { clearAllDialog = true },
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            ),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !vm.isSelecting && vm.historyCount > 0,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        HistorySearchField(
                            query = vm.searchQuery,
                            onQueryChange = vm::updateSearchQuery,
                        )
                        SourceFilterRow(
                            sources = vm.sources,
                            selected = vm.sourceFilter,
                            onSelect = vm::updateSourceFilter,
                        )
                    }
                }
            }
        },
        blurTopBar = true,
        topBarBlur = {
            progressive(
                HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
            )
        }
    ) { p ->
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()

        LazyColumn(
            // Top padding goes on the list itself so sticky headers pin below the top bar, not behind it.
            contentPadding = PaddingValues(bottom = p.calculateBottomPadding()),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = p.calculateTopPadding())
        ) {
            when {
                vm.isLoading -> items(8, key = { "placeholder_$it" }) {
                    HistoryItemPlaceholder(modifier = Modifier.animateItem())
                }

                vm.historyCount == 0 -> item(key = "empty") {
                    HistoryEmptyState(
                        icon = Icons.Default.History,
                        title = stringResource(Res.string.history_empty_title),
                        description = stringResource(Res.string.history_empty_description),
                        modifier = Modifier.animateItem()
                    )
                }

                vm.filteredItems.isEmpty() -> item(key = "no_matches") {
                    HistoryEmptyState(
                        icon = Icons.Default.SearchOff,
                        title = stringResource(Res.string.history_no_matches),
                        description = stringResource(Res.string.history_no_matches_description),
                        modifier = Modifier.animateItem()
                    ) {
                        TextButton(onClick = vm::resetSearchAndFilter) {
                            Text(stringResource(Res.string.history_reset_filters))
                        }
                    }
                }

                else -> vm.groups.forEach { (group, list) ->
                    stickyHeader(key = group.key, contentType = "header") {
                        HistoryGroupHeader(
                            group = group,
                            count = list.size,
                            modifier = Modifier.animateItem()
                        )
                    }

                    items(list, key = { it.url }, contentType = { "item" }) { item ->
                        HistoryItem(
                            item = item,
                            sourceName = sourceRepository.displayNameFor(item.source),
                            timeLabel = relativeTimeLabel(item.timestamp, now, timeZone),
                            isSelected = item.url in vm.selected,
                            selectionMode = vm.isSelecting,
                            isMissing = isMissing(item.source),
                            searchQuery = vm.searchQuery,
                            colorFilter = colorFilter,
                            onClick = {
                                if (vm.isSelecting) vm.toggleSelection(item.url) else openItem(item)
                            },
                            onLongClick = { vm.toggleSelection(item.url) },
                            onRemove = { removeWithUndo(listOf(item)) },
                            onInfo = { optionsSheetItem = HistoryItemOptionsSheet(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryOverflowMenu(
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, stringResource(Res.string.more_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.select_all)) },
                leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onSelectAll()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.clear_all_history_menu), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onClearAll()
                }
            )
        }
    }
}

@Composable
private fun HistorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.search_history)) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, stringResource(Res.string.clear_search))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun SourceFilterRow(
    sources: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val sourceRepository = LocalSourcesRepository.current

    @Composable
    fun Chip(source: String?, label: String) {
        val isSelected = selected == source
        FilterChip(
            selected = isSelected,
            onClick = { onSelect(source) },
            label = { Text(label) },
            leadingIcon = if (isSelected && source != null) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item(key = "all") { Chip(null, stringResource(Res.string.all)) }
        items(sources, key = { it }) { source ->
            Chip(source, sourceRepository.displayNameFor(source))
        }
    }
}

@Composable
private fun HistoryGroupHeader(
    group: HistoryGroup,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
        ) {
            Text(
                group.label(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryGroup.label(): String = when (this) {
    HistoryGroup.Today -> stringResource(Res.string.history_today)
    HistoryGroup.Yesterday -> stringResource(Res.string.history_yesterday)
    HistoryGroup.ThisWeek -> stringResource(Res.string.history_this_week)
    is HistoryGroup.MonthOf -> "${month.name.lowercase().replaceFirstChar { it.uppercase() }} $year"
}

data class HistoryItemOptionsSheet(
    val item: RecentModel,
    override val imageUrl: String = item.imageUrl,
    override val title: String = item.title,
    override val description: String = item.description,
    override val serviceName: String = item.source,
    override val url: String = item.url,
) : OptionsSheetValues

@Composable
private fun highlightMatch(title: String, query: String): AnnotatedString {
    val trimmed = query.trim()
    val start = if (trimmed.isEmpty()) -1 else title.indexOf(trimmed, ignoreCase = true)
    if (start < 0) return AnnotatedString(title)
    val highlight = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    return buildAnnotatedString {
        append(title)
        addStyle(highlight, start, start + trimmed.length)
    }
}

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    item: RecentModel,
    sourceName: String,
    timeLabel: String,
    isSelected: Boolean,
    selectionMode: Boolean,
    isMissing: Boolean,
    searchQuery: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnRemove by rememberUpdatedState(onRemove)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) currentOnRemove()
            // Stay settled: the state is saved per item key, so a dismissed value would come back with Undo.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !selectionMode,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer
                else Color.Transparent,
                label = "swipeColor"
            )
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp)
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    ) {
        val container by animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background,
            label = "rowColor"
        )
        ListItem(
            headlineContent = {
                Text(
                    highlightMatch(item.title, searchQuery),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.customSharedElement(OtakuTitleElement(SharedItemKey(item.url, item.source)))
                )
            },
            supportingContent = {
                Text(
                    if (isMissing) stringResource(Res.string.source_not_installed, sourceName) else sourceName,
                    color = if (isMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 72.dp)
                        .clip(MaterialTheme.shapes.small)
                        .customSharedElement(OtakuImageElement(SharedItemKey(item.url, item.source)))
                        .alpha(if (isMissing) .6f else 1f)
                ) {
                    val logo = painterLogo()
                    GradientImage(
                        model = item.imageUrl,
                        placeholder = logo,
                        error = logo,
                        contentDescription = item.title,
                        colorFilter = if (isMissing) GrayscaleFilter else colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = .7f))
                        ) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!selectionMode) {
                        IconButton(onClick = onInfo) {
                            Icon(Icons.Outlined.Info, stringResource(Res.string.more_options))
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = container),
            modifier = Modifier
                .semantics { selected = isSelected }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
        )
    }
}

@Composable
private fun HistoryEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp)
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        action()
    }
}

@Composable
fun HistoryItemPlaceholder(modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text("Otaku") },
        supportingContent = { Text("Otaku") },
        leadingContent = {
            Box(
                Modifier
                    .size(width = 48.dp, height = 72.dp)
                    .clip(MaterialTheme.shapes.small)
                    .m3placeholder(true, highlight = PlaceholderHighlight.shimmer())
            )
        },
        trailingContent = { Text("12m") },
        modifier = modifier.m3placeholder(
            true,
            highlight = PlaceholderHighlight.shimmer()
        ),
    )
}
