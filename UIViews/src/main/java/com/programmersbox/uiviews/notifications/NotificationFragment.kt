package com.programmersbox.uiviews.notifications

import android.app.NotificationManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.NotifySingleWorker
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SavedNotifications
import com.programmersbox.uiviews.utils.*
import com.programmersbox.uiviews.utils.components.AnimatedLazyColumn
import com.programmersbox.uiviews.utils.components.AnimatedLazyListItem
import com.programmersbox.uiviews.utils.components.BottomSheetDeleteScaffold
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

private fun NotificationManager.cancelNotification(item: NotificationItem) {
    cancel(item.id)
    val g = activeNotifications.map { it.notification }.filter { it.group == "otakuGroup" }
    if (g.size == 1) cancel(42)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun NotificationsScreen(
    navController: NavController,
    genericInfo: GenericInfo,
    db: ItemDao,
    notificationManager: NotificationManager,
    logo: MainLogo,
    notificationLogo: NotificationLogo,
    fragmentManager: FragmentManager,
    vm: NotificationScreenViewModel = viewModel()
) {
    val items by db.getAllNotificationsFlow().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        db.getAllNotificationCountFlow()
            .filter { it == 0 }
            .collect {
                notificationManager.cancel(42)
                navController.popBackStack()
            }
    }

    val state = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val logoDrawable = remember { AppCompatResources.getDrawable(context, logo.logoId) }

    BackHandler(state.bottomSheetState.isExpanded) {
        scope.launch { state.bottomSheetState.collapse() }
    }
    val topAppBarScrollState = rememberTopAppBarScrollState()

    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

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
                                    val number = db.deleteAllNotificationsFlow()
                                    launch(Dispatchers.Main) {
                                        onDismiss()
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.deleted_notifications, number),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        context.notificationManager.cancel(42)
                                    }
                                }
                            }
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
                )

            }

            SmallTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(id = R.string.current_notification_count, items.size)) },
                actions = {
                    IconButton(onClick = { showPopup = true }) { Icon(Icons.Default.ClearAll, null) }
                    IconButton(onClick = { scope.launch { state.bottomSheetState.expand() } }) { Icon(Icons.Default.Delete, null) }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        onRemove = { item ->
            vm.deleteNotification(db, item)
            notificationManager.cancelNotification(item)
        },
        onMultipleRemove = { d ->
            Completable.merge(
                d.map {
                    notificationManager.cancelNotification(it)
                    db.deleteNotification(it)
                }
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { d.clear() }
                .addTo(vm.disposable)
        },
        deleteTitle = { stringResource(R.string.removeNoti, it.notiTitle) },
        itemUi = { item ->
            ListItem(
                modifier = Modifier.padding(5.dp),
                leadingContent = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .lifecycle(LocalLifecycleOwner.current)
                            .size(ComposableUtils.IMAGE_WIDTH_PX, ComposableUtils.IMAGE_HEIGHT_PX)
                            .crossfade(true)
                            .build(),
                        placeholder = rememberDrawablePainter(logoDrawable),
                        error = rememberDrawablePainter(logoDrawable),
                        contentScale = ContentScale.FillBounds,
                        contentDescription = item.notiTitle,
                        modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                },
                overlineText = { Text(item.source) },
                headlineText = { Text(item.notiTitle) },
                supportingText = { Text(item.summaryText) },
                trailingContent = {
                    var showDropDown by remember { mutableStateOf(false) }

                    androidx.compose.material3.DropdownMenu(
                        expanded = showDropDown,
                        onDismissRequest = { showDropDown = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.remove_same_name)) },
                            onClick = {
                                Completable.merge(
                                    items
                                        .filter { it.notiTitle == item.notiTitle }
                                        .map {
                                            notificationManager.cancelNotification(it)
                                            db.deleteNotification(it)
                                        }
                                )
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe {
                                        showDropDown = false
                                        Toast.makeText(context, R.string.done, Toast.LENGTH_SHORT).show()
                                    }
                                    .addTo(vm.disposable)
                            }
                        )
                    }

                    IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                }
            )
        }
    ) { p, itemList ->

        AnimatedLazyColumn(
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
        )

        /*LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) { items(itemList) { NotificationItem(item = it!!, navController = findNavController()) } }*/
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@Composable
private fun NotificationItem(
    item: NotificationItem,
    navController: NavController,
    vm: NotificationScreenViewModel,
    notificationManager: NotificationManager,
    db: ItemDao,
    parentFragmentManager: FragmentManager,
    genericInfo: GenericInfo,
    logo: MainLogo,
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
                        vm.deleteNotification(db, item, onDismiss)
                        notificationManager.cancelNotification(item)
                    }
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
        )

    }

    val dismissState = rememberDismissState(
        confirmStateChange = {
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
                }
            )
            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
            }
            val icon = when (direction) {
                DismissDirection.StartToEnd -> Icons.Default.Delete
                DismissDirection.EndToStart -> Icons.Default.Delete
            }
            val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

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
        }
    ) {

        val interactionSource = remember { MutableInteractionSource() }

        androidx.compose.material3.ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .clickable(
                    onClickLabel = item.notiTitle,
                    interactionSource = interactionSource,
                    indication = rememberRipple()
                ) {
                    scope.launch {
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
                            ?.onEach { navController.navigateToDetails(it) }
                            ?.collect()
                    }
                }
        ) {
            Row {
                val logoDrawable = remember { AppCompatResources.getDrawable(context, logo.logoId) }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .lifecycle(LocalLifecycleOwner.current)
                        .crossfade(true)
                        .build(),
                    placeholder = rememberDrawablePainter(logoDrawable),
                    error = rememberDrawablePainter(logoDrawable),
                    contentScale = ContentScale.Crop,
                    contentDescription = item.notiTitle,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 4.dp)
                ) {
                    Text(item.source, style = M3MaterialTheme.typography.labelMedium)
                    Text(item.notiTitle, style = M3MaterialTheme.typography.titleSmall)
                    Text(item.summaryText, style = M3MaterialTheme.typography.bodyMedium)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(horizontal = 2.dp)
                ) {

                    var showDropDown by remember { mutableStateOf(false) }

                    val dropDownDismiss = { showDropDown = false }

                    androidx.compose.material3.DropdownMenu(
                        expanded = showDropDown,
                        onDismissRequest = dropDownDismiss
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.notify)) },
                            onClick = {
                                dropDownDismiss()
                                scope.launch(Dispatchers.IO) {
                                    SavedNotifications.viewNotificationFromDb(context, item, notificationLogo, genericInfo)
                                }
                            }
                        )

                        MenuDefaults.Divider()

                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.notifyAtTime)) },
                            onClick = {
                                dropDownDismiss()
                                val datePicker = MaterialDatePicker.Builder.datePicker()
                                    .setTitleText(R.string.selectDate)
                                    .setCalendarConstraints(
                                        CalendarConstraints.Builder()
                                            .setOpenAt(System.currentTimeMillis())
                                            .setValidator(DateValidatorPointForward.now())
                                            .build()
                                    )
                                    .setSelection(System.currentTimeMillis())
                                    .build()

                                datePicker.addOnPositiveButtonClickListener {
                                    val c = Calendar.getInstance()
                                    val timePicker = MaterialTimePicker.Builder()
                                        .setTitleText(R.string.selectTime)
                                        .setPositiveButtonText(R.string.ok)
                                        .setTimeFormat(
                                            if (DateFormat.is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
                                        )
                                        .setHour(c[Calendar.HOUR_OF_DAY])
                                        .setMinute(c[Calendar.MINUTE])
                                        .build()

                                    timePicker.addOnPositiveButtonClickListener { _ ->
                                        c.timeInMillis = it
                                        c.add(Calendar.DAY_OF_YEAR, 1)
                                        c[Calendar.HOUR_OF_DAY] = timePicker.hour
                                        c[Calendar.MINUTE] = timePicker.minute

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
                                                context.getSystemDateTimeFormat().format(c.timeInMillis)
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    timePicker.show(parentFragmentManager, "timePicker")
                                }

                                datePicker.show(parentFragmentManager, "datePicker")
                            }
                        )

                        MenuDefaults.Divider()

                        androidx.compose.material3.DropdownMenuItem(
                            onClick = {
                                dropDownDismiss()
                                showPopup = true
                            },
                            text = { Text(stringResource(R.string.remove)) }
                        )
                    }

                    IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                }
            }
        }
    }
}