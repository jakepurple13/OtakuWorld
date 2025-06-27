package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
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
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.NotificationSortBy
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
import com.programmersbox.kmpuiviews.presentation.components.ImageFlushListItem
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard2
import com.programmersbox.kmpuiviews.presentation.components.M3ImageCard
import com.programmersbox.kmpuiviews.presentation.components.ModalBottomSheetDelete
import com.programmersbox.kmpuiviews.presentation.components.OptionsSheetValues
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.optionsSheet
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.areYouSureRemoveNoti
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.current_notification_count
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.notify
import otakuworld.kmpuiviews.generated.resources.notifyAtTime
import otakuworld.kmpuiviews.generated.resources.ok
import otakuworld.kmpuiviews.generated.resources.removeNoti
import otakuworld.kmpuiviews.generated.resources.selectDate
import otakuworld.kmpuiviews.generated.resources.selectTime
import otakuworld.kmpuiviews.generated.resources.yes
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)
@Composable
fun NotificationScreen(
    navController: NavigationActions = LocalNavActions.current,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    vm: NotificationScreenViewModel = koinViewModel(),
    notificationRepository: NotificationRepository = koinInject(),
    itemDao: ItemDao = koinInject(),
) {
    val settingsHandling: NewSettingsHandling = koinInject()
    val showBlur by settingsHandling.rememberShowBlur()

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    val items = vm.items

    val state = rememberBottomSheetScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(state.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { state.bottomSheetState.partialExpand() }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showNotificationItem by remember { mutableStateOf<NotificationItem?>(null) }

    SourceNotInstalledModal(
        showItem = showNotificationItem?.notiTitle,
        onShowItemDismiss = { showNotificationItem = null },
        source = showNotificationItem?.source,
        url = showNotificationItem?.url
    )

    var showDeleteModal by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)

    if (showDeleteModal) {
        ModalBottomSheetDelete(
            onDismiss = { showDeleteModal = false },
            listOfItems = vm.groupedList.flatMap {
                listOf(
                    NotificationInfo.Source(it.first),
                    *it.second.map { NotificationInfo.Noti(it) }.toTypedArray()
                )
            },
            gridCells = adaptiveGridCell(),
            state = sheetState,
            multipleTitle = stringResource(Res.string.areYouSureRemoveNoti),
            onRemove = { item ->
                if (item is NotificationInfo.Noti) {
                    vm.deleteNotification(item.item)
                }
            },
            onMultipleRemove = { d ->
                scope.launch {
                    withContext(Dispatchers.Default) {
                        d
                            .filterIsInstance<NotificationInfo.Noti>()
                            .forEach { vm.deleteNotification(it.item) }
                    }
                }
            },
            deleteTitle = { stringResource(Res.string.removeNoti, (it as NotificationInfo.Noti).item.notiTitle) },
            itemUi = { item ->
                if (item is NotificationInfo.Noti) {
                    M3ImageCard(
                        imageUrl = item.item.imageUrl.orEmpty(),
                        name = item.item.notiTitle,
                        placeHolder = { rememberVectorPainter(Icons.Default.Settings) }
                    )
                }
            },
            isTitle = { it is NotificationInfo.Source },
            titleUi = {
                if (it is NotificationInfo.Source)
                    TopAppBar(title = { Text(it.title) }, windowInsets = WindowInsets(0.dp))
            },
            span = {
                when (it) {
                    is NotificationInfo.Noti -> GridItemSpan(1)
                    is NotificationInfo.Source -> GridItemSpan(maxLineSpan)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(Res.string.current_notification_count, items.size)) },
                actions = {
                    IconToggleButton(
                        checked = vm.sortedBy == NotificationSortBy.Grouped,
                        onCheckedChange = { vm.toggleSort() }
                    ) { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                    IconButton(onClick = { showDeleteModal = true }) { Icon(Icons.Default.Delete, null) }
                },
                navigationIcon = { BackButton() }
            )
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        }
    ) { p ->
        Crossfade(
            targetState = vm.sortedBy,
            label = "",
            modifier = Modifier.padding(p)
        ) { target ->
            when (target) {
                NotificationSortBy.Date -> {
                    DateSort(
                        navController = navController,
                        vm = vm,
                        p = LocalNavHostPadding.current,
                        toSource = { s -> sourceRepository.toSourceByApiServiceName(s)?.apiService },
                        onLoadingChange = { showLoadingDialog = it },
                        deleteNotification = vm::deleteNotification,
                        cancelNotification = vm::cancelNotification,
                        showBlur = showBlur,
                        itemDao = itemDao,
                        onError = {
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
                    )
                }

                NotificationSortBy.Grouped -> {
                    GroupedSort(
                        navController = navController,
                        vm = vm,
                        p = LocalNavHostPadding.current,
                        onLoadingChange = { showLoadingDialog = it },
                        showBlur = showBlur,
                        itemDao = itemDao,
                        onError = {
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
                    )
                }
            }
        }

        /*AnimatedLazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 4.dp),
            items = itemList.fastMap {
                AnimatedLazyListItem(key = it.url, value = it) {
                    NotificationItem(
                        item = it,
                        navController = navController,
                        vm = vm,
                        notificationManager = notificationManager,
                        db = db,
                        parentFragmentManager = fragmentManager,
                        genericInfo = genericInfo,
                        logo = logo,
                        notificationLogo = notificationLogo
                    )
                }
            }
        )*/

        /*LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) { items(itemList) { NotificationItem(item = it!!, navController = findNavController()) } }*/
    }
}

sealed class NotificationInfo {
    data class Source(val title: String) : NotificationInfo()
    data class Noti(val item: NotificationItem) : NotificationInfo()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DateSort(
    navController: NavigationActions,
    vm: NotificationScreenViewModel,
    deleteNotification: (item: NotificationItem, block: () -> Unit) -> Unit,
    cancelNotification: (NotificationItem) -> Unit,
    p: PaddingValues,
    toSource: (String) -> KmpApiService?,
    onError: (NotificationItem) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    showBlur: Boolean,
    itemDao: ItemDao,
) {
    val hazeState = remember { HazeState() }

    val scope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = adaptiveGridCell(),
        contentPadding = p,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
    ) {
        vm.groupedList.forEach { item ->
            val expanded = vm.groupedListState[item.first]?.value == true
            stickyHeader {
                Surface(
                    shape = if (expanded) RectangleShape else MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                    onClick = { vm.toggleGroupedState(item.first) },
                    color = if (expanded && showBlur) Color.Transparent else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(if (expanded) RectangleShape else MaterialTheme.shapes.medium)
                        .hazeEffect(hazeState, style = HazeMaterials.thin()) {
                            progressive = HazeProgressive.verticalGradient(
                                startIntensity = 1f,
                                endIntensity = 0f,
                                preferPerformance = true
                            )
                        }
                        .animateItem()
                ) {
                    ListItem(
                        modifier = Modifier.padding(4.dp),
                        headlineContent = { Text(item.first) },
                        leadingContent = { Text(item.second.size.toString()) },
                        trailingContent = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                modifier = Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f, label = "").value)
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        )
                    )
                }
            }

            if (expanded) {
                items(item.second) {
                    NotiItem(
                        i = it,
                        scope = scope,
                        toSource = toSource,
                        onError = onError,
                        onLoadingChange = onLoadingChange,
                        navController = navController,
                        deleteNotification = deleteNotification,
                        cancelNotification = cancelNotification,
                        itemDao = itemDao,
                        modifier = Modifier.hazeSource(hazeState)
                    )
                }
            }
        }
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
            Cached.cache[i.url]?.let {
                flow {
                    emit(
                        it
                            .toDbModel()
                            .toItemModel(source)
                    )
                }
            } ?: source.getSourceByUrlFlow(i.url)
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
            item = i,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotiItem(
    i: NotificationItem,
    scope: CoroutineScope,
    toSource: (String) -> KmpApiService?,
    onError: (NotificationItem) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    navController: NavigationActions,
    itemDao: ItemDao,
    modifier: Modifier = Modifier,
    deleteNotification: (item: NotificationItem, block: () -> Unit) -> Unit,
    cancelNotification: (NotificationItem) -> Unit,
) {
    val biometricOpen = rememberBiometricOpening()
    var optionsSheet by notificationOptionsSheet(
        i = i,
        scope = scope,
        navController = navController,
        toSource = toSource,
        itemDao = itemDao,
        onError = onError,
        onLoadingChange = onLoadingChange,
    )

    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.removeNoti, i.notiTitle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        optionsSheet = null
                        deleteNotification(i, onDismiss)
                        cancelNotification(i)
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                showPopup = true
            }
            false
        }
    )

    //TODO: Maaaaybe remove this and double press starts the delete?
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.wrapContentSize(),
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.StartToEnd -> Color.Red
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                }, label = ""
            )

            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = ""
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.scale(scale),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        content = {
            M3CoverCard2(
                imageUrl = i.imageUrl.orEmpty(),
                name = i.notiTitle,
                placeHolder = { rememberVectorPainter(Icons.Default.Settings) },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .combinedClickable(
                        onClick = {
                            scope.launch {
                                biometricOpen.openIfNotIncognito(i.url, i.notiTitle) {
                                    toSource(i.source)?.let { source ->
                                        Cached.cache[i.url]?.let {
                                            flow {
                                                emit(
                                                    it
                                                        .toDbModel()
                                                        .toItemModel(source)
                                                )
                                            }
                                        } ?: source.getSourceByUrlFlow(i.url)
                                    }
                                        ?.dispatchIo()
                                        ?.onStart { onLoadingChange(true) }
                                        ?.onEach {
                                            onLoadingChange(false)
                                            navController.details(it)
                                        }
                                        ?.launchIn(scope) ?: onError(i)
                                }
                            }
                        },
                        onLongClick = { optionsSheet = NotificationItemOptionsSheet(i) }
                    )
            )
        }
    )
}

@Composable
private fun GroupedSort(
    navController: NavigationActions,
    vm: NotificationScreenViewModel,
    p: PaddingValues,
    onLoadingChange: (Boolean) -> Unit,
    showBlur: Boolean,
    itemDao: ItemDao,
    onError: (NotificationItem) -> Unit,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
) {
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    LazyColumn(
        contentPadding = p + LocalNavHostPadding.current,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        vm.groupedList.forEach { item ->
            val expanded = vm.groupedListState[item.first]?.value == true

            stickyHeader {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                    onClick = { vm.toggleGroupedState(item.first) },
                    color = if (expanded && showBlur) Color.Transparent else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .hazeEffect(hazeState, style = HazeMaterials.thin())
                        .animateItem()
                ) {
                    ListItem(
                        modifier = Modifier.padding(4.dp),
                        headlineContent = { Text(item.first) },
                        leadingContent = { Text(item.second.size.toString()) },
                        trailingContent = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                modifier = Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f, label = "").value)
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        )
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.hazeSource(hazeState)
                    ) {
                        item.second.forEach {
                            NotificationItem(
                                item = it,
                                navController = navController,
                                deleteNotification = vm::deleteNotification,
                                cancelNotification = vm::cancelNotification,
                                toSource = { s -> sourceRepository.toSourceByApiServiceName(s)?.apiService },
                                onLoadingChange = onLoadingChange,
                                itemDao = itemDao,
                                onError = onError,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    item: NotificationItem,
    navController: NavigationActions,
    deleteNotification: (item: NotificationItem, block: () -> Unit) -> Unit,
    cancelNotification: (NotificationItem) -> Unit,
    toSource: (String) -> KmpApiService?,
    onError: (NotificationItem) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    itemDao: ItemDao,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val biometricOpen = rememberBiometricOpening()
    var optionsSheet by notificationOptionsSheet(
        i = item,
        scope = scope,
        navController = navController,
        toSource = toSource,
        itemDao = itemDao,
        onError = onError,
        onLoadingChange = onLoadingChange
    )
    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.removeNoti, item.notiTitle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteNotification(item, onDismiss)
                        cancelNotification(item)
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                showPopup = true
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.StartToEnd -> Color.Red
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                }, label = ""
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Delete
            }
            val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.scale(scale),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        content = {
            ElevatedCard(
                onClick = {
                    scope.launch {
                        biometricOpen.openIfNotIncognito(item.url, item.notiTitle) {
                            toSource(item.source)
                                ?.let { source ->
                                    Cached.cache[item.url]?.let {
                                        flow {
                                            emit(
                                                it
                                                    .toDbModel()
                                                    .toItemModel(source)
                                            )
                                        }
                                    } ?: source.getSourceByUrlFlow(item.url)
                                }
                                ?.dispatchIo()
                                ?.onStart { onLoadingChange(true) }
                                ?.onEach {
                                    onLoadingChange(false)
                                    navController.details(it)
                                }
                                ?.launchIn(scope) ?: onError(item)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                ImageFlushListItem(
                    leadingContent = {
                        GradientImage(
                            model = item.imageUrl.orEmpty(),
                            placeholder = painterLogo(),
                            error = painterLogo(),
                            contentDescription = item.notiTitle,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    },
                    overlineContent = { Text(item.source) },
                    headlineContent = { Text(item.notiTitle) },
                    supportingContent = { Text(item.summaryText) },
                    trailingContent = {
                        IconButton(
                            onClick = { optionsSheet = NotificationItemOptionsSheet(item) }
                        ) { Icon(Icons.Default.MoreVert, null) }
                    }
                )
            }
        },
        modifier = modifier
    )
}

/*
@Composable
private fun NotificationDeleteItem(
    item: NotificationItem,
    logoDrawable: Drawable?,
    onRemoveAllWithSameName: () -> Unit,
) {
    ImageFlushListItem(
        leadingContent = {
            GradientImage(
                model = item.imageUrl.orEmpty(),
                placeholder = rememberDrawablePainter(logoDrawable),
                error = rememberDrawablePainter(logoDrawable),
                contentDescription = item.notiTitle,
                modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
            )
        },
        overlineContent = { Text(item.source) },
        headlineContent = { Text(item.notiTitle) },
        supportingContent = { Text(item.summaryText) },
        trailingContent = {
            var showDropDown by remember { mutableStateOf(false) }

            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = { showDropDown = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.remove_same_name)) },
                    onClick = {
                        showDropDown = false
                        onRemoveAllWithSameName()
                    }
                )
            }

            IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
        }
    )
}*/

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
internal fun NotifyAt(
    item: NotificationItem,
    notificationScreenInterface: NotificationScreenInterface,
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

                        val current = TimeZone.currentSystemDefault()

                        val now = Instant
                            .fromEpochMilliseconds(
                                dateState
                                    .selectedDateMillis
                                    ?: Clock
                                        .System
                                        .now()
                                        .toEpochMilliseconds()
                            )
                            .toLocalDateTime(current)

                        val trigger = LocalDateTime(
                            year = now.year,
                            month = now.month.number,
                            day = now.day,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            second = 0,
                            nanosecond = 0
                        )

                        notificationScreenInterface.scheduleNotification(
                            item = item,
                            time = trigger
                                .toInstant(current)
                                .toEpochMilliseconds() - now
                                .toInstant(current)
                                .toEpochMilliseconds()
                        )
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