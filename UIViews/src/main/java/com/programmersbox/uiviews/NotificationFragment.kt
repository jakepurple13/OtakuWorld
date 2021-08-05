package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

                BottomSheetDeleteScaffold(
                    listOfItems = items,
                    multipleTitle = stringResource(R.string.areYouSureRemoveNoti),
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

                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        StaggeredVerticalGrid(
                            columns = 2,
                            modifier = Modifier.padding(it)
                        ) { items.forEach { NotificationItem(it, findNavController()) } }
                    }
                }

            }
        }

        lifecycleScope.launch {
            db.getAllNotificationCountFlow()
                .flowWithLifecycle(lifecycle)
                .filter { it == 0 }
                .collect { findNavController().popBackStack() }
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

                Text(item.notiTitle, style = MaterialTheme.typography.h6, modifier = Modifier.padding(top = 5.dp, start = 5.dp, end = 5.dp))
                Text(item.source, style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(horizontal = 5.dp))

                GlideImage(
                    imageModel = item.imageUrl.orEmpty(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalView.current)
                        .asBitmap()
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

            }

        }

    }

}