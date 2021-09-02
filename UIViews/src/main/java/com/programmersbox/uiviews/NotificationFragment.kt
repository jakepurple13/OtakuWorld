package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class NotificationFragment : BaseBottomSheetDialogFragment() {

    private val genericInfo: GenericInfo by inject()
    private val db by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()
    private val notificationManager by lazy { requireContext().notificationManager }
    private val logo: MainLogo by inject()

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent {
            MdcTheme {

                val items by db.getAllNotificationsFlow()
                    .flowOn(Dispatchers.IO)
                    .collectAsState(emptyList())

                val state = rememberBottomSheetScaffoldState()
                /*val scope = rememberCoroutineScope()
                //TODO: Put this in for all scaffolds
                //TODO: Maaaybe not, this also takes over even when not on the screen
                val backCallBack = remember {
                    object : OnBackPressedCallback(state.bottomSheetState.isExpanded) {
                        override fun handleOnBackPressed() {
                            when {
                                state.bottomSheetState.isExpanded -> scope.launch { state.bottomSheetState.collapse() }
                            }
                        }
                    }
                }

                backCallBack.isEnabled = state.bottomSheetState.isExpanded

                DisposableEffect(key1 = requireActivity().onBackPressedDispatcher) {
                    requireActivity().onBackPressedDispatcher.addCallback(backCallBack)
                    onDispose { backCallBack.remove() }
                }*/

                BottomSheetDeleteScaffold(
                    listOfItems = items,
                    state = state,
                    multipleTitle = stringResource(R.string.areYouSureRemoveNoti),
                    topBar = { TopAppBar(title = { Text(stringResource(id = R.string.current_notification_count, items.size)) }) },
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
                                GlideImage(
                                    imageModel = item.imageUrl.orEmpty(),
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    requestBuilder = Glide.with(LocalView.current)
                                        .asDrawable()
                                        .override(360, 480)
                                        .thumbnail(0.5f)
                                        .transform(RoundedCorners(15)),//GranularRoundedCorners(0f, 15f, 15f, 0f)),
                                    modifier = Modifier
                                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                                    failure = {
                                        Image(
                                            painter = rememberDrawablePainter(AppCompatResources.getDrawable(LocalContext.current, logo.logoId)),
                                            contentDescription = item.notiTitle,
                                            modifier = Modifier
                                                .padding(5.dp)
                                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                        )
                                    }
                                )
                            },
                            overlineText = { Text(item.source) },
                            text = { Text(item.notiTitle) },
                            secondaryText = { Text(item.summaryText) },
                            trailing = {
                                var showDropDown by remember { mutableStateOf(false) }

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

                                IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                            }
                        )

                        /*Row {
                            GlideImage(
                                imageModel = item.imageUrl.orEmpty(),
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                requestBuilder = Glide.with(LocalView.current)
                                    .asDrawable()
                                    .override(360, 480)
                                    .thumbnail(0.5f)
                                    .transform(GranularRoundedCorners(0f, 15f, 15f, 0f)),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                                failure = {
                                    Image(
                                        painter = rememberDrawablePainter(AppCompatResources.getDrawable(LocalContext.current, logo.logoId)),
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
                        }*/
                    }
                ) { p ->
                    LazyColumn(contentPadding = p) { items(items) { NotificationItem(item = it, navController = findNavController()) } }
                    /*Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        StaggeredVerticalGrid(
                            columns = 2,
                            modifier = Modifier.padding(it)
                        ) { items.forEach { NotificationItem(it, findNavController()) } }
                    }*/
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
                        contentDescription = null,
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        ) {

            Card(
                onClick = {
                    genericInfo.toSource(item.source)?.getSourceByUrl(item.url)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeBy { navController.navigate(NotificationFragmentDirections.actionNotificationFragmentToDetailsFragment(it)) }
                        ?.addTo(disposable)
                },
                elevation = 5.dp,
                indication = rememberRipple(),
                onClickLabel = item.notiTitle,
                modifier = Modifier
                    .padding(5.dp)
                    .fadeInAnimation()
            ) {

                Column {

                    Row {
                        GlideImage(
                            imageModel = item.imageUrl.orEmpty(),
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            requestBuilder = Glide.with(LocalView.current)
                                .asDrawable()
                                .override(360, 480)
                                .thumbnail(0.5f)
                                .transform(GranularRoundedCorners(0f, 15f, 15f, 0f)),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                            failure = {
                                Image(
                                    painter = rememberDrawablePainter(AppCompatResources.getDrawable(LocalContext.current, logo.logoId)),
                                    contentDescription = item.notiTitle,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(5.dp)
                                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            }
                        )

                        Column(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                            Text(item.source, style = MaterialTheme.typography.overline)
                            Text(item.notiTitle, style = MaterialTheme.typography.subtitle2)
                            Text(item.summaryText, style = MaterialTheme.typography.body2)
                        }

                    }

                    /*Button(
                        onClick = { showPopup = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp, 0.dp, 4.dp, 4.dp)
                    ) { Text(stringResource(R.string.remove), style = MaterialTheme.typography.button) }*/

                }

                //TODO: Below is for a LazyStaggeredVerticalGrid when its finally officially supported
                /*Column {

                Text(item.notiTitle, style = MaterialTheme.typography.h6, modifier = Modifier.padding(top = 5.dp, start = 5.dp, end = 5.dp))
                Text(item.source, style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(horizontal = 5.dp))

                GlideImage(
                    imageModel = item.imageUrl.orEmpty(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalView.current)
                        .asDrawable()
                        .override(360, 480)
                        .placeholder(logo.logoId)
                        .error(logo.logoId)
                        .fallback(logo.logoId)
                        .transform(RoundedCorners(15)),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(5.dp)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    failure = {
                        Image(
                            painter = rememberDrawablePainter(AppCompatResources.getDrawable(LocalContext.current, logo.logoId)),
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

            }*/

            }

        }

    }

}