package com.programmersbox.uiviews

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.rememberImagePainter
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class NotificationFragment : BaseBottomSheetDialogFragment() {

    private val genericInfo: GenericInfo by inject()
    private val db by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()
    private val notificationManager by lazy { requireContext().notificationManager }
    private val logo: MainLogo by inject()
    private val notificationLogo: NotificationLogo by inject()

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent {
            M3MaterialTheme(currentColorScheme) {

                val items by db.getAllNotificationsFlow().collectAsState(emptyList())

                val state = rememberBottomSheetScaffoldState()
                val scope = rememberCoroutineScope()

                BackHandler(state.bottomSheetState.isExpanded && currentScreen.value == R.id.setting_nav) {
                    scope.launch { state.bottomSheetState.collapse() }
                }

                val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

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
                                            db.deleteAllNotifications()
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribeBy {
                                                    onDismiss()
                                                    Toast.makeText(
                                                        requireContext(),
                                                        getString(R.string.deleted_notifications, it),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addTo(disposable)
                                            notificationManager.cancel(42)
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
                                IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                            }
                        )
                    },
                    onRemove = { item ->
                        db.deleteNotification(item)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                        cancelNotification(item)
                    },
                    onMultipleRemove = { d ->
                        Completable.merge(
                            d.map {
                                cancelNotification(it)
                                db.deleteNotification(it)
                            }
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { d.clear() }
                            .addTo(disposable)
                    },
                    itemUi = { item ->
                        ListItem(
                            modifier = Modifier.padding(5.dp),
                            icon = {
                                Image(
                                    painter = rememberImagePainter(data = item.imageUrl.orEmpty()) {
                                        placeholder(logo.logoId)
                                        error(logo.logoId)
                                        crossfade(true)
                                        lifecycle(LocalLifecycleOwner.current)
                                        size(480, 360)
                                    },
                                    contentDescription = item.notiTitle,
                                    modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            },
                            overlineText = { Text(item.source) },
                            text = { Text(item.notiTitle) },
                            secondaryText = { Text(item.summaryText) },
                            trailing = {
                                var showDropDown by remember { mutableStateOf(false) }

                                MdcTheme {
                                    DropdownMenu(
                                        expanded = showDropDown,
                                        onDismissRequest = { showDropDown = false }
                                    ) {
                                        DropdownMenuItem(
                                            onClick = {
                                                Completable.merge(
                                                    items
                                                        .filter { it.notiTitle == item.notiTitle }
                                                        .map {
                                                            cancelNotification(it)
                                                            db.deleteNotification(it)
                                                        }
                                                )
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe {
                                                        showDropDown = false
                                                        Toast.makeText(requireContext(), R.string.done, Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addTo(disposable)
                                            }
                                        ) { Text(stringResource(id = R.string.remove_same_name)) }
                                    }
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
                            AnimatedLazyListItem(key = it.url, value = it) { NotificationItem(item = it, navController = findNavController()) }
                        }
                    )
                    //TODO: This will be used when native lazycolumn/lazyrow gains support for animations
                    /*LazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) { items(itemList) { NotificationItem(item = it, navController = findNavController()) } }*/
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            db.getAllNotificationCountFlow()
                .flowWithLifecycle(lifecycle)
                .filter { it == 0 }
                .collect {
                    notificationManager.cancel(42)
                    findNavController().popBackStack()
                }
        }
    }

    private fun cancelNotification(item: NotificationItem) {
        notificationManager.cancel(item.id)
        val g = notificationManager.activeNotifications.map { it.notification }.filter { it.group == "otakuGroup" }
        if (g.size == 1) notificationManager.cancel(42)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    @ExperimentalMaterialApi
    @Composable
    private fun NotificationItem(item: NotificationItem, navController: NavController) {

        var showPopup by remember { mutableStateOf(false) }

        if (showPopup) {

            val onDismiss = { showPopup = false }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.removeNoti, item.notiTitle)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            db.deleteNotification(item)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { onDismiss() }
                                .addTo(disposable)
                            cancelNotification(item)
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

            Surface(
                onClick = {
                    genericInfo.toSource(item.source)
                        ?.getSourceByUrl(item.url)
                        ?.subscribeOn(Schedulers.io())
                        ?.doOnError { context?.showErrorToast() }
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeBy { navController.navigate(NotificationFragmentDirections.actionNotificationFragmentToDetailsFragment(it)) }
                        ?.addTo(disposable)
                },
                tonalElevation = 5.dp,
                indication = rememberRipple(),
                onClickLabel = item.notiTitle,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {

                Row {
                    Image(
                        painter = rememberImagePainter(data = item.imageUrl.orEmpty()) {
                            placeholder(logo.logoId)
                            error(logo.logoId)
                            crossfade(true)
                            lifecycle(LocalLifecycleOwner.current)
                            size(480, 360)
                        },
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

                        MdcTheme {
                            DropdownMenu(
                                expanded = showDropDown,
                                onDismissRequest = dropDownDismiss
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            SavedNotifications.viewNotificationFromDb(requireContext(), item, notificationLogo, genericInfo)
                                        }
                                    }
                                ) { Text(stringResource(R.string.notify)) }
                                DropdownMenuItem(
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
                                                    if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
                                                )
                                                .setHour(c[Calendar.HOUR_OF_DAY])
                                                .setMinute(c[Calendar.MINUTE])
                                                .build()

                                            timePicker.addOnPositiveButtonClickListener { _ ->
                                                c.timeInMillis = it
                                                c.add(Calendar.DAY_OF_YEAR, 1)
                                                c[Calendar.HOUR_OF_DAY] = timePicker.hour
                                                c[Calendar.MINUTE] = timePicker.minute

                                                WorkManager.getInstance(requireContext())
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
                                                    requireContext(),
                                                    getString(
                                                        R.string.willNotifyAt,
                                                        requireContext().getSystemDateTimeFormat().format(c.timeInMillis)
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            timePicker.show(parentFragmentManager, "timePicker")
                                        }

                                        datePicker.show(parentFragmentManager, "datePicker")
                                    }
                                ) { Text(stringResource(R.string.notifyAtTime)) }
                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        showPopup = true
                                    }
                                ) { Text(stringResource(R.string.remove)) }
                            }
                        }

                        IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                    }
                }
            }
        }
    }
}