package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.databinding.FragmentNotificationBinding
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
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
        Scaffold(
            bottomBar = { MultiDeleteBottomButton(notiList) }
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

    @Composable
    private fun MultiDeleteBottomButton(notiList: List<NotificationItem>) {
        Button(
            onClick = {
                val items = mutableListOf<NotificationItem>()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.removeNotification)
                    .setMultiChoiceItems(
                        notiList.map { i -> "${i.source} - ${i.notiTitle}" }.toTypedArray(),
                        null
                    ) { _, i, b ->
                        val item = notiList[i]
                        if (b) items.add(item) else items.remove(item)
                    }
                    .setPositiveButton(R.string.yes) { d, _ ->
                        items.forEach {
                            db.deleteNotification(it)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe()
                                .addTo(disposable)
                            cancelNotification(it)
                        }
                        d.dismiss()
                    }
                    .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                    .show()
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

}