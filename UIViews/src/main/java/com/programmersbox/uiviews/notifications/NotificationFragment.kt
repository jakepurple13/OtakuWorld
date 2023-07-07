package com.programmersbox.uiviews.notifications

import android.app.NotificationManager
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.material.datepicker.DateValidatorPointForward
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.NotificationSortBy
import com.programmersbox.uiviews.NotifySingleWorker
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SavedNotifications
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.uiviews.utils.MockAppIcon
import com.programmersbox.uiviews.utils.MockInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.components.AnimatedLazyColumn
import com.programmersbox.uiviews.utils.components.AnimatedLazyListItem
import com.programmersbox.uiviews.utils.components.BottomSheetDeleteScaffold
import com.programmersbox.uiviews.utils.components.GradientImage
import com.programmersbox.uiviews.utils.components.ImageFlushListItem
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.navigateToDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

fun NotificationManager.cancelNotification(item: NotificationItem) {
    cancel(item.id)
    val g = activeNotifications.map { it.notification }.filter { it.group == "otakuGroup" }
    if (g.size == 1) cancel(42)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun NotificationsScreen(
    logo: MainLogo,
    notificationLogo: NotificationLogo,
    navController: NavController = LocalNavController.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    db: ItemDao = LocalItemDao.current,
    settingsHandling: SettingsHandling = LocalSettingsHandling.current,
    vm: NotificationScreenViewModel = viewModel { NotificationScreenViewModel(db, settingsHandling, genericInfo) },
    cancelNotificationById: (Int) -> Unit,
    cancelNotification: (NotificationItem) -> Unit,
) {
    val items = vm.items

    val context = LocalContext.current
    val logoDrawable = remember { AppCompatResources.getDrawable(context, logo.logoId) }

    LaunchedEffect(Unit) {
        db.getAllNotificationCount()
            .filter { it == 0 }
            .collect {
                cancelNotificationById(42)
                navController.popBackStack()
            }
    }

    val state = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BackHandler(state.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { state.bottomSheetState.partialExpand() }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BottomSheetDeleteScaffold(
        listOfItems = items,
        state = state,
        multipleTitle = stringResource(R.string.areYouSureRemoveNoti),
        bottomScrollBehavior = scrollBehavior,
        topBar = {
            var showPopup by remember { mutableStateOf(false) }

            if (showPopup) {
                val onDismiss = { showPopup = false }
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(stringResource(R.string.are_you_sure_delete_notifications)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val number = db.deleteAllNotifications()
                                    launch(Dispatchers.Main) {
                                        onDismiss()
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.deleted_notifications, number),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        cancelNotificationById(42)
                                    }
                                }
                            }
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
                )
            }

            InsetSmallTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(id = R.string.current_notification_count, items.size)) },
                actions = {
                    IconButton(onClick = { showPopup = true }) { Icon(Icons.Default.ClearAll, null) }
                    IconToggleButton(
                        checked = vm.sortedBy == NotificationSortBy.Grouped,
                        onCheckedChange = { vm.toggleSort() }
                    ) { Icon(Icons.Default.Sort, null) }
                },
                navigationIcon = { BackButton() }
            )
        },
        onRemove = { item ->
            vm.deleteNotification(db, item)
            cancelNotification(item)
        },
        onMultipleRemove = { d ->
            scope.launch {
                withContext(Dispatchers.Default) {
                    d.forEach {
                        cancelNotification(it)
                        db.deleteNotification(it)
                    }
                }
                d.clear()
            }
        },
        deleteTitle = { stringResource(R.string.removeNoti, it.notiTitle) },
        itemUi = { item ->
            NotificationDeleteItem(
                item = item,
                logoDrawable = logoDrawable,
                onRemoveAllWithSameName = {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            items
                                .filter { it.notiTitle == item.notiTitle }
                                .forEach {
                                    cancelNotification(it)
                                    db.deleteNotification(it)
                                }
                        }
                        Toast.makeText(context, R.string.done, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    ) { p, itemList ->
        Crossfade(targetState = vm.sortedBy, label = "") { target ->
            when (target) {
                NotificationSortBy.Date -> {
                    AnimatedLazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                        items = itemList.fastMap {
                            AnimatedLazyListItem(key = it.url, value = it) {
                                NotificationItem(
                                    item = it,
                                    navController = navController,
                                    deleteNotification = vm::deleteNotification,
                                    cancelNotification = cancelNotification,
                                    db = db,
                                    genericInfo = genericInfo,
                                    logoDrawable = logoDrawable,
                                    notificationLogo = notificationLogo
                                )
                            }
                        }
                    )
                }

                NotificationSortBy.Grouped -> {
                    LazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {

                        vm.groupedList.toList().forEach { item ->
                            val expanded = vm.groupedListState[item.first]?.value == true

                            stickyHeader {
                                Surface(
                                    shape = M3MaterialTheme.shapes.medium,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { vm.toggleGroupedState(item.first) }
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
                                        }
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
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item.second.forEach {
                                            NotificationItem(
                                                item = it,
                                                navController = navController,
                                                deleteNotification = vm::deleteNotification,
                                                cancelNotification = cancelNotification,
                                                db = db,
                                                genericInfo = genericInfo,
                                                logoDrawable = logoDrawable,
                                                notificationLogo = notificationLogo
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                NotificationSortBy.UNRECOGNIZED -> {}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    item: NotificationItem,
    navController: NavController,
    deleteNotification: (db: ItemDao, item: NotificationItem, block: () -> Unit) -> Unit,
    cancelNotification: (NotificationItem) -> Unit,
    db: ItemDao,
    genericInfo: GenericInfo,
    logoDrawable: Drawable?,
    notificationLogo: NotificationLogo,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.removeNoti, item.notiTitle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteNotification(db, item, onDismiss)
                        cancelNotification(item)
                    }
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
        )
    }

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                showPopup = true
            }
            false
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.Default -> Color.Transparent
                    DismissValue.DismissedToEnd -> Color.Red
                    DismissValue.DismissedToStart -> Color.Red
                }, label = ""
            )
            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
            }
            val icon = when (direction) {
                DismissDirection.StartToEnd -> Icons.Default.Delete
                DismissDirection.EndToStart -> Icons.Default.Delete
            }
            val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f, label = "")

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
                    tint = M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
                )
            }
        },
        dismissContent = {
            ElevatedCard(
                onClick = {
                    genericInfo
                        .toSource(item.source)
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
                        ?.onStart { showLoadingDialog = true }
                        ?.onEach {
                            showLoadingDialog = false
                            navController.navigateToDetails(it)
                        }
                        ?.launchIn(scope)
                },
                modifier = Modifier.padding(horizontal = 4.dp)
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

                        val dropDownDismiss = { showDropDown = false }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.notify)) },
                                onClick = {
                                    dropDownDismiss()
                                    scope.launch(Dispatchers.IO) {
                                        SavedNotifications.viewNotificationFromDb(context, item, notificationLogo, genericInfo)
                                    }
                                }
                            )

                            Divider()

                            NotifyAt(
                                item = item,
                                onDropDownDismiss = dropDownDismiss
                            )

                            Divider()

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    showPopup = true
                                },
                                text = { Text(stringResource(R.string.remove)) }
                            )
                        }

                        IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyAt(
    item: NotificationItem,
    onDropDownDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateValidatorPointForward.now().isValid(utcTimeMillis)
                }
            }
        }
    )
    val calendar = remember { Calendar.getInstance() }
    val is24HourFormat by rememberUpdatedState(DateFormat.is24HourFormat(context))
    val timeState = rememberTimePickerState(
        initialHour = calendar[Calendar.HOUR_OF_DAY],
        initialMinute = calendar[Calendar.MINUTE],
        is24Hour = is24HourFormat
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(id = R.string.selectTime)) },
            confirmButton = {
                val dateTimeFormatter = LocalSystemDateTimeFormat.current
                TextButton(
                    onClick = {
                        showTimePicker = false
                        val c = Calendar.getInstance()
                        c.timeInMillis = dateState.selectedDateMillis ?: 0L
                        c[Calendar.HOUR_OF_DAY] = timeState.hour
                        c[Calendar.MINUTE] = timeState.minute

                        WorkManager.getInstance(context)
                            .enqueueUniqueWork(
                                item.notiTitle,
                                ExistingWorkPolicy.REPLACE,
                                OneTimeWorkRequestBuilder<NotifySingleWorker>()
                                    .setInputData(
                                        Data.Builder()
                                            .putString("notiData", item.toJson())
                                            .build()
                                    )
                                    .setInitialDelay(c.timeInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                                    .build()
                            )

                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.willNotifyAt,
                                dateTimeFormatter.format(c.timeInMillis)
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text(stringResource(R.string.ok)) }
            }
        ) {
            DatePicker(
                state = dateState,
                title = { Text(stringResource(R.string.selectDate)) }
            )
        }
    }

    DropdownMenuItem(
        text = { Text(stringResource(R.string.notifyAtTime)) },
        onClick = {
            onDropDownDismiss()
            showDatePicker = true
        }
    )
}

@Composable
private fun NotificationDeleteItem(
    item: NotificationItem,
    logoDrawable: Drawable?,
    onRemoveAllWithSameName: () -> Unit
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
                    text = { Text(stringResource(id = R.string.remove_same_name)) },
                    onClick = {
                        showDropDown = false
                        onRemoveAllWithSameName()
                    }
                )
            }

            IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
        }
    )
}

@LightAndDarkPreviews
@Composable
private fun NotificationPreview() {
    PreviewTheme {
        NotificationsScreen(
            logo = MockAppIcon,
            notificationLogo = NotificationLogo(R.drawable.ic_site_settings),
            cancelNotification = {},
            cancelNotificationById = {}
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun NotificationItemPreview() {
    PreviewTheme {
        NotificationItem(
            item = NotificationItem(1, "", "world", "hello", null, "MANGA_READ", "Title"),
            navController = rememberNavController(),
            deleteNotification = { _, _, _ -> },
            db = LocalItemDao.current,
            genericInfo = MockInfo,
            cancelNotification = {},
            logoDrawable = null,
            notificationLogo = NotificationLogo(R.drawable.ic_site_settings)
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun NotificationDeleteItemPreview() {
    PreviewTheme {
        Column {
            NotificationDeleteItem(
                item = NotificationItem(1, "", "world", "hello", "", "MANGA_READ", "Title"),
                logoDrawable = null,
                onRemoveAllWithSameName = {}
            )
        }
    }
}