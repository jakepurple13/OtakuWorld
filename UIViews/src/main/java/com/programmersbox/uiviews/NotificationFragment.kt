package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.databinding.FragmentNotificationBinding
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class NotificationFragment : BaseBottomSheetDialogFragment() {

    private val genericInfo: GenericInfo by inject()

    private val db by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()

    private val notificationManager by lazy { requireContext().notificationManager }

    private lateinit var binding: FragmentNotificationBinding

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.setContent {
            MdcTheme {

                val items by db.getAllNotificationsFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAsState(emptyList())

                NotificationLayout(items)

            }
        }

        db.getAllNotificationCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it == 0 }
            .subscribeBy { findNavController().popBackStack() }
            .addTo(disposable)
    }

    private fun cancelNotification(item: NotificationItem) {
        notificationManager.cancel(item.id)
        val g = notificationManager.activeNotifications.map { it.notification }.filter { it.group == "otakuGroup" }
        if (g.size == 1) notificationManager.cancel(42)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    private fun NotificationLayout(notiList: List<NotificationItem>) {

        val state = rememberBottomSheetScaffoldState()
        val scope = rememberCoroutineScope()

        BottomSheetScaffold(
            scaffoldState = state,
            sheetShape = MaterialTheme.shapes.medium.copy(CornerSize(4.dp), CornerSize(4.dp), CornerSize(0.dp), CornerSize(0.dp)),
            sheetPeekHeight = ButtonDefaults.MinHeight,
            sheetContent = {

                val itemsToDelete = remember { mutableStateListOf<NotificationItem>() }

                var showPopup by remember { mutableStateOf(false) }

                if (showPopup) {

                    val onDismiss = { showPopup = false }

                    AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text(stringResource(R.string.areYouSureRemoveNoti)) },
                        text = { Text(resources.getQuantityString(R.plurals.areYouSureRemove, itemsToDelete.size, itemsToDelete.size)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onDismiss()
                                    scope.launch { state.bottomSheetState.collapse() }

                                    Completable.merge(
                                        itemsToDelete.map {
                                            cancelNotification(it)
                                            db.deleteNotification(it)
                                        }
                                    )
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { itemsToDelete.clear() }
                                        .addTo(disposable)
                                }
                            ) { Text(stringResource(R.string.yes), style = MaterialTheme.typography.button) }
                        },
                        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.no), style = MaterialTheme.typography.button) } }
                    )

                }

                Scaffold(
                    topBar = {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (state.bottomSheetState.isCollapsed) state.bottomSheetState.expand()
                                    else state.bottomSheetState.collapse()
                                    itemsToDelete.clear()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(0f)
                        ) {
                            Text(
                                stringResource(R.string.delete_multiple),
                                style = MaterialTheme.typography.button
                            )
                        }
                    },
                    bottomBar = {
                        BottomAppBar(
                            contentPadding = PaddingValues(0.dp)//Modifier.padding(0.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch { state.bottomSheetState.collapse() }
                                    itemsToDelete.clear()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 5.dp)
                            ) { Text(stringResource(id = R.string.no), style = MaterialTheme.typography.button) }

                            Button(
                                onClick = { showPopup = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 5.dp)
                            ) { Text(stringResource(id = R.string.remove), style = MaterialTheme.typography.button) }
                        }
                    }
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = it,
                        state = rememberLazyListState(),
                        modifier = Modifier.padding(5.dp)
                    ) { items(notiList) { NotificationItemView(it, itemsToDelete) } }
                }
            }
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                StaggeredVerticalGrid(
                    columns = 2,
                    modifier = Modifier.padding(it)
                ) { notiList.forEach { NotificationItem(it, binding.root.findNavController()) } }
            }
        }
    }

    private val logo: NotificationLogo by inject()

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
                    Button(
                        onClick = {
                            db.deleteNotification(item)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { onDismiss() }
                                .addTo(disposable)
                            cancelNotification(item)
                        }
                    ) { Text(stringResource(R.string.yes), style = MaterialTheme.typography.button) }
                },
                dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.no), style = MaterialTheme.typography.button) } }
            )

        }

        Card(
            onClick = {
                genericInfo.toSource(item.source)?.getSourceByUrl(item.url)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribeBy { navController.navigate(NotificationFragmentDirections.actionNotificationFragmentToDetailsFragment(it)) }
                    ?.addTo(disposable)
            },
            elevation = 5.dp,
            interactionSource = MutableInteractionSource(),
            indication = rememberRipple(),
            onClickLabel = item.notiTitle,
            modifier = Modifier
                .padding(5.dp)
                .fadeInAnimation()
        ) {

            Column {

                Text(item.notiTitle, style = MaterialTheme.typography.h6, modifier = Modifier.padding(top = 5.dp, start = 5.dp, end = 5.dp))
                Text(item.source, style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(horizontal = 5.dp))

                GlideImage(
                    imageModel = item.imageUrl.orEmpty(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalView.current)
                        .asBitmap()
                        .override(360, 480)
                        .placeholder(logo.notificationId)
                        .error(logo.notificationId)
                        .fallback(logo.notificationId)
                        .transform(RoundedCorners(15)),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(5.dp)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    failure = {
                        Image(
                            painter = painterResource(logo.notificationId),
                            contentDescription = item.notiTitle,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(5.dp)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    }
                )

                Text(
                    item.summaryText,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .padding(bottom = 5.dp)
                )

                Button(
                    onClick = { showPopup = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp, 0.dp, 4.dp, 4.dp)
                ) { Text(stringResource(R.string.remove), style = MaterialTheme.typography.button) }

            }

        }

    }

    @ExperimentalMaterialApi
    @Composable
    private fun NotificationItemView(item: NotificationItem, deleteItemList: SnapshotStateList<NotificationItem>) {

        var showPopup by remember { mutableStateOf(false) }

        if (showPopup) {

            val onDismiss = { showPopup = false }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.removeNoti, item.notiTitle)) },
                confirmButton = {
                    Button(
                        onClick = {
                            db.deleteNotification(item)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { onDismiss() }
                                .addTo(disposable)
                            cancelNotification(item)
                        }
                    ) { Text(stringResource(R.string.yes), style = MaterialTheme.typography.button) }
                },
                dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.no), style = MaterialTheme.typography.button) } }
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
            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
            dismissThresholds = { FractionalThreshold(0.5f) },
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
                        contentDescription = "Localized description",
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(),
                onClick = { if (item in deleteItemList) deleteItemList.remove(item) else deleteItemList.add(item) },
                backgroundColor = animateColorAsState(if (item in deleteItemList) Color(0xfff44336) else MaterialTheme.colors.surface).value
            ) {

                Row {
                    GlideImage(
                        imageModel = item.imageUrl.orEmpty(),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        requestBuilder = Glide.with(LocalView.current)
                            .asBitmap()
                            .override(360, 480)
                            .thumbnail(0.5f)
                            .transform(GranularRoundedCorners(0f, 15f, 15f, 0f)),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                        failure = {
                            Image(
                                painter = painterResource(logo.notificationId),
                                contentDescription = item.notiTitle,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(5.dp)
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            )
                        }
                    )

                    Column(modifier = Modifier.padding(start = 5.dp)) {
                        Text(item.notiTitle)
                        Text(item.source)
                    }

                }

            }
        }

    }

}