package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.GradientImage
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.OptionsSheetValues
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.optionsSheet
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.all
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.catch_up
import otakuworld.kmpuiviews.generated.resources.clear_selection
import otakuworld.kmpuiviews.generated.resources.delete
import otakuworld.kmpuiviews.generated.resources.deleted_notification
import otakuworld.kmpuiviews.generated.resources.deleted_notification_count
import otakuworld.kmpuiviews.generated.resources.in_tray
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.no_updates_description
import otakuworld.kmpuiviews.generated.resources.no_updates_here
import otakuworld.kmpuiviews.generated.resources.notifications
import otakuworld.kmpuiviews.generated.resources.notifications_older
import otakuworld.kmpuiviews.generated.resources.notifications_this_week
import otakuworld.kmpuiviews.generated.resources.notifications_today
import otakuworld.kmpuiviews.generated.resources.notifications_yesterday
import otakuworld.kmpuiviews.generated.resources.notify
import otakuworld.kmpuiviews.generated.resources.notifyAtTime
import otakuworld.kmpuiviews.generated.resources.ok
import otakuworld.kmpuiviews.generated.resources.remind
import otakuworld.kmpuiviews.generated.resources.removeNoti
import otakuworld.kmpuiviews.generated.resources.selectDate
import otakuworld.kmpuiviews.generated.resources.selectTime
import otakuworld.kmpuiviews.generated.resources.select_all_notifications
import otakuworld.kmpuiviews.generated.resources.selected_count
import otakuworld.kmpuiviews.generated.resources.sent_to_tray
import otakuworld.kmpuiviews.generated.resources.undo
import otakuworld.kmpuiviews.generated.resources.yes
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** The Catch up button shows once the current filter holds more than this many items. */
private const val CATCH_UP_THRESHOLD = 10

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NotificationScreen(
    navController: NavigationActions = LocalNavActions.current,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    vm: NotificationScreenViewModel = koinViewModel(),
    notificationRepository: NotificationRepository = koinInject(),
    itemDao: ItemDao = koinInject(),
) {
    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }
    val notificationScreenInterface: NotificationScreenInterface = koinInject()

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val biometricOpen = rememberBiometricOpening()

    var showNotificationItem by remember { mutableStateOf<NotificationItem?>(null) }

    SourceNotInstalledModal(
        showItem = showNotificationItem?.notiTitle,
        onShowItemDismiss = { showNotificationItem = null },
        source = showNotificationItem?.source,
        url = showNotificationItem?.url
    )

    val toSource: (String) -> KmpApiService? = { s -> sourceRepository.toSourceByApiServiceName(s)?.apiService }

    val onError: (NotificationItem) -> Unit = {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                "Something went wrong. Source might not be installed",
                duration = SnackbarDuration.Long,
                actionLabel = "More Options",
                withDismissAction = true
            )
            showNotificationItem = when (result) {
                SnackbarResult.Dismissed -> null
                SnackbarResult.ActionPerformed -> it
            }
        }
    }

    val openItem: (NotificationItem) -> Unit = { item ->
        scope.launch {
            biometricOpen.openIfNotIncognito(item.url, item.notiTitle) {
                toSource(item.source)
                    ?.let { source ->
                        flow {
                            Cached.cache[item.url]?.let {
                                emit(
                                    it
                                        .toDbModel()
                                        .toItemModel(source)
                                )
                            } ?: emitAll(source.getSourceByUrlFlow(item.url))
                        }
                    }
                    ?.dispatchIo()
                    ?.onStart { showLoadingDialog = true }
                    ?.onEach {
                        showLoadingDialog = false
                        navController.details(it)
                    }
                    ?.launchIn(scope) ?: onError(item)
            }
        }
    }

    val deleteWithUndo: (List<NotificationItem>) -> Unit = { toDelete ->
        if (toDelete.isNotEmpty()) {
            val batch = vm.deleteWithUndo(toDelete)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                var undone = false
                try {
                    val message = toDelete.singleOrNull()
                        ?.let { getString(Res.string.deleted_notification, it.notiTitle) }
                        ?: getString(Res.string.deleted_notification_count, toDelete.size)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = getString(Res.string.undo),
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    )
                    undone = result == SnackbarResult.ActionPerformed
                    if (undone) vm.undoDeletion(batch)
                } finally {
                    // Also runs when the screen leaves composition mid-snackbar.
                    if (!undone) vm.commitDeletion(batch)
                }
            }
        }
    }

    BackHandler(vm.isSelecting) { vm.clearSelection() }
    BackHandler(vm.catchUp != null) { vm.endCatchUp() }

    AnimatedContent(
        targetState = vm.catchUp != null,
        label = "catchUp",
    ) { inCatchUp ->
        if (inCatchUp) {
            val state = vm.catchUp
            CatchUpDeck(
                remaining = vm.catchUpRemaining,
                index = state?.index ?: 0,
                total = state?.urls?.size ?: 0,
                title = vm.filter.catchUpTitle(),
                colorFilter = colorFilter,
                snackbarHostState = snackbarHostState,
                notificationScreenInterface = notificationScreenInterface,
                onClose = vm::endCatchUp,
                onDismiss = { item ->
                    deleteWithUndo(listOf(item))
                    vm.advanceCatchUp()
                },
                onRead = { item ->
                    vm.advanceCatchUp()
                    openItem(item)
                },
                onReminded = { vm.advanceCatchUp() },
            )
        } else {
            NotificationTimeline(
                vm = vm,
                navController = navController,
                itemDao = itemDao,
                colorFilter = colorFilter,
                snackbarHostState = snackbarHostState,
                notificationScreenInterface = notificationScreenInterface,
                toSource = toSource,
                onError = onError,
                onLoadingChange = { showLoadingDialog = it },
                openItem = openItem,
                deleteWithUndo = deleteWithUndo,
                notifySelected = { selected ->
                    vm.clearSelection()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            selected.forEach { notificationScreenInterface.notifyItem(it) }
                        }
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(getString(Res.string.sent_to_tray, selected.size))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationTimeline(
    vm: NotificationScreenViewModel,
    navController: NavigationActions,
    itemDao: ItemDao,
    colorFilter: ColorFilter?,
    snackbarHostState: SnackbarHostState,
    notificationScreenInterface: NotificationScreenInterface,
    toSource: (String) -> KmpApiService?,
    onError: (NotificationItem) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    openItem: (NotificationItem) -> Unit,
    deleteWithUndo: (List<NotificationItem>) -> Unit,
    notifySelected: (List<NotificationItem>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val is24Hour = koinInject<DateTimeFormatHandler>().is24Time()

    OtakuScaffold(
        topBar = {
            AnimatedContent(
                vm.isSelecting,
                transitionSpec = {
                    // Compare targetState to initialState to determine animation direction
                    if (targetState > initialState) {
                        // Moving Forward: Slide in from bottom, slide out to top
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        // Moving Backward: Slide in from top, slide out to bottom
                        (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> height } + fadeOut())
                    }
                }
            ) { target ->
                if (target) {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = { Text(stringResource(Res.string.selected_count, vm.selected.size)) },
                        navigationIcon = {
                            IconButton(onClick = vm::clearSelection) {
                                Icon(Icons.Default.Close, stringResource(Res.string.clear_selection))
                            }
                        },
                        actions = {
                            IconButton(onClick = vm::selectAll) {
                                Icon(Icons.Default.SelectAll, stringResource(Res.string.select_all_notifications))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    )
                } else {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = { Text(stringResource(Res.string.notifications)) },
                        navigationIcon = { BackButton() },
                        actions = {
                            if (vm.filteredItems.size > CATCH_UP_THRESHOLD) {
                                FilledTonalButton(
                                    onClick = vm::startCatchUp,
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.Layers, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(Res.string.catch_up))
                                }
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
        ) {
            Column {
                FilterRow(vm = vm)

                if (vm.filteredItems.isEmpty()) {
                    EmptyTimeline(Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        vm.dayBuckets.forEach { (day, list) ->
                            stickyHeader(key = day) {
                                DayHeader(
                                    day = day,
                                    count = list.size,
                                    modifier = Modifier.animateItem()
                                )
                            }

                            items(list, key = { it.url }) { item ->
                                var optionsSheet by notificationOptionsSheet(
                                    i = item,
                                    scope = scope,
                                    navController = navController,
                                    toSource = toSource,
                                    itemDao = itemDao,
                                    onError = onError,
                                    onLoadingChange = onLoadingChange,
                                )
                                NotificationRow(
                                    item = item,
                                    timeLabel = timeLabel(item.createdAt, day, is24Hour),
                                    isSelected = item.url in vm.selected,
                                    selectionMode = vm.isSelecting,
                                    colorFilter = colorFilter,
                                    notificationScreenInterface = notificationScreenInterface,
                                    onClick = {
                                        if (vm.isSelecting) vm.toggleSelection(item.url) else openItem(item)
                                    },
                                    onLongClick = { vm.toggleSelection(item.url) },
                                    onDelete = { deleteWithUndo(listOf(item)) },
                                    onMore = { optionsSheet = NotificationItemOptionsSheet(item) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = vm.isSelecting,
                enter = slideInVertically { it * 2 } + fadeIn() + scaleIn(),
                exit = slideOutVertically { it * 2 } + fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset)
            ) {
                val selected = vm.selectedItems()
                HorizontalFloatingToolbar(expanded = true) {
                    IconButton(onClick = { notifySelected(selected.filterNot { it.isShowing }) }) {
                        Icon(Icons.Default.Notifications, stringResource(Res.string.notify))
                    }
                    NotifyAt(
                        items = selected,
                        notificationScreenInterface = notificationScreenInterface,
                        onScheduled = vm::clearSelection,
                    ) { showDatePicker ->
                        IconButton(onClick = showDatePicker) {
                            Icon(Icons.Default.Schedule, stringResource(Res.string.notifyAtTime))
                        }
                    }
                    FilledIconButton(
                        onClick = { deleteWithUndo(selected) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    ) { Icon(Icons.Default.Delete, stringResource(Res.string.delete)) }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(vm: NotificationScreenViewModel) {
    @Composable
    fun Chip(filter: NotificationFilter, label: String, count: Int) {
        val selected = vm.filter == filter
        FilterChip(
            selected = selected,
            onClick = { vm.updateFilter(filter) },
            label = { Text("$label  $count") },
            leadingIcon = if (selected) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { Chip(NotificationFilter.All, stringResource(Res.string.all), vm.totalCount) }
        if (vm.trayCount > 0) {
            item { Chip(NotificationFilter.InTray, stringResource(Res.string.in_tray), vm.trayCount) }
        }
        items(vm.sourceCounts, key = { it.first }) { (source, count) ->
            Chip(NotificationFilter.Source(source), source, count)
        }
    }
}

@Composable
private fun DayHeader(
    day: NotificationDay,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
        ) {
            Text(
                day.label(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationRow(
    item: NotificationItem,
    timeLabel: String,
    isSelected: Boolean,
    selectionMode: Boolean,
    colorFilter: ColorFilter?,
    notificationScreenInterface: NotificationScreenInterface,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.removeNoti, item.notiTitle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(Res.string.no)) }
            }
        )
    }

    NotifyAt(
        items = listOf(item),
        notificationScreenInterface = notificationScreenInterface,
    ) { showDatePicker ->
        val currentShowDatePicker by rememberUpdatedState(showDatePicker)
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                when (it) {
                    SwipeToDismissBoxValue.StartToEnd -> currentShowDatePicker()
                    SwipeToDismissBoxValue.EndToStart -> showDeleteConfirm = true
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = !selectionMode,
            enableDismissFromEndToStart = !selectionMode,
            modifier = modifier,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color by animateColorAsState(
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
                    label = "swipeColor"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 24.dp)
                ) {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(stringResource(Res.string.remind), color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }

                        SwipeToDismissBoxValue.EndToStart -> {
                            Spacer(Modifier.weight(1f))
                            Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.onErrorContainer)
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        SwipeToDismissBoxValue.Settled -> Unit
                    }
                }
            }
        ) {
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                label = "rowColor"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(container)
                    .semantics { selected = isSelected }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 72.dp)
                        .clip(MaterialTheme.shapes.small)
                ) {
                    GradientImage(
                        model = item.imageUrl.orEmpty(),
                        placeholder = painterLogo(),
                        error = painterLogo(),
                        contentDescription = item.notiTitle,
                        colorFilter = colorFilter,
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        item.notiTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOf(item.source, timeLabel).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.isShowing) InTrayPill()

                if (!selectionMode) {
                    IconButton(onClick = onMore) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun InTrayPill() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                stringResource(Res.string.in_tray),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyTimeline(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(stringResource(Res.string.no_updates_here), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.no_updates_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NotificationDay.label(): String = stringResource(
    when (this) {
        NotificationDay.Today -> Res.string.notifications_today
        NotificationDay.Yesterday -> Res.string.notifications_yesterday
        NotificationDay.ThisWeek -> Res.string.notifications_this_week
        NotificationDay.Older -> Res.string.notifications_older
    }
)

@Composable
private fun NotificationFilter.catchUpTitle(): String = when (this) {
    NotificationFilter.All -> stringResource(Res.string.catch_up)
    NotificationFilter.InTray -> "${stringResource(Res.string.catch_up)} · ${stringResource(Res.string.in_tray)}"
    is NotificationFilter.Source -> "${stringResource(Res.string.catch_up)} · $name"
}

/** Time for today and yesterday, weekday for this week, month and day for older items. */
private fun timeLabel(createdAt: Long, day: NotificationDay, is24Hour: Boolean): String {
    if (createdAt <= 0L) return ""
    val local = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
    return when (day) {
        NotificationDay.Today, NotificationDay.Yesterday -> {
            val minute = local.minute.toString().padStart(2, '0')
            if (is24Hour) {
                "${local.hour.toString().padStart(2, '0')}:$minute"
            } else {
                val hour = (local.hour % 12).let { if (it == 0) 12 else it }
                "$hour:$minute ${if (local.hour < 12) "AM" else "PM"}"
            }
        }

        NotificationDay.ThisWeek -> local.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        NotificationDay.Older -> "${local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${local.day}"
    }
}

data class NotificationItemOptionsSheet(
    val item: NotificationItem,
    override val imageUrl: String = item.imageUrl.orEmpty(),
    override val title: String = item.notiTitle,
    override val description: String = item.summaryText,
    override val serviceName: String = item.source,
    override val url: String = item.url,
) : OptionsSheetValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun notificationOptionsSheet(
    i: NotificationItem,
    scope: CoroutineScope,
    navController: NavigationActions,
    toSource: (String) -> KmpApiService?,
    onLoadingChange: (Boolean) -> Unit,
    notificationRepository: NotificationRepository = koinInject(),
    itemDao: ItemDao,
    onError: (NotificationItem) -> Unit,
) = optionsSheet<NotificationItemOptionsSheet>(
    onOpen = {
        toSource(i.source)?.let { source ->
            flow {
                Cached.cache[i.url]?.let {
                    emit(
                        it
                            .toDbModel()
                            .toItemModel(source)
                    )
                } ?: emitAll(source.getSourceByUrlFlow(i.url))
            }
        }
            ?.dispatchIo()
            ?.onStart { onLoadingChange(true) }
            ?.onEach {
                onLoadingChange(false)
                navController.details(it)
            }
            ?.launchIn(scope) ?: onError(i)
    }
) {
    val notificationScreenInterface: NotificationScreenInterface = koinInject()
    if (!it.item.isShowing) {
        Card(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    notificationScreenInterface.notifyItem(i)
                }.invokeOnCompletion { dismiss() }
            },
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.notify)) },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }

        HorizontalDivider()

        NotifyAt(
            items = listOf(i),
            notificationScreenInterface = notificationScreenInterface
        ) { dateShow ->
            Card(
                onClick = { dateShow() },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.notifyAtTime)) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

        HorizontalDivider()
    } else {
        OptionsItem(
            title = "Dismiss Notification",
            onClick = {
                scope.launch {
                    itemDao.updateNotification(i.url, false)
                    notificationRepository.cancelById(i.id)
                    dismiss()
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
internal fun NotifyAt(
    items: List<NotificationItem>,
    notificationScreenInterface: NotificationScreenInterface,
    onScheduled: () -> Unit = {},
    content: @Composable ((() -> Unit) -> Unit),
) {
    val dateFormatHandler: DateTimeFormatHandler = koinInject()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val now = remember { Clock.System.now().toEpochMilliseconds() }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds(),
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return now < utcTimeMillis
                }
            }
        }
    )
    val calendar = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val is24HourFormat by rememberUpdatedState(dateFormatHandler.is24Time())
    val timeState = rememberTimePickerState(
        initialHour = calendar.hour,
        initialMinute = calendar.minute,
        is24Hour = is24HourFormat
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(Res.string.selectTime)) },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) { Text(stringResource(Res.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false

                        val currentTimeZone = TimeZone.currentSystemDefault()

                        val selectedDate = Instant
                            .fromEpochMilliseconds(
                                dateState
                                    .selectedDateMillis
                                    ?: Clock
                                        .System
                                        .now()
                                        .toEpochMilliseconds()
                            )
                            .toLocalDateTime(currentTimeZone)

                        val trigger = LocalDateTime(
                            year = selectedDate.year,
                            month = selectedDate.month.number,
                            day = selectedDate.day,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            second = 0,
                            nanosecond = 0
                        )

                        val currentTime = Clock.System.now().toEpochMilliseconds()
                        val triggerTime = trigger.toInstant(currentTimeZone).toEpochMilliseconds()

                        items.forEach {
                            notificationScreenInterface.scheduleNotification(
                                item = it,
                                time = triggerTime - currentTime
                            )
                        }
                        onScheduled()
                    }
                ) { Text(stringResource(Res.string.ok)) }
            }
        ) { TimePicker(state = timeState) }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) { Text(stringResource(Res.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text(stringResource(Res.string.ok)) }
            }
        ) {
            DatePicker(
                state = dateState,
                title = { Text(stringResource(Res.string.selectDate)) }
            )
        }
    }

    content { showDatePicker = true }
}
