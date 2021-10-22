package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class RecentlyViewedFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = RecentlyViewedFragment()
    }

    private val dao by lazy { HistoryDatabase.getInstance(requireContext()).historyDao() }

    private val info by inject<GenericInfo>()
    private val logo: MainLogo by inject()
    private val disposable = CompositeDisposable()

    private val format = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault())

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent { MdcTheme { RecentlyViewedUi() } }
        }

    @ExperimentalMaterialApi
    @Composable
    private fun RecentlyViewedUi() {

        val recentItems by dao.getRecentlyViewed().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()

        var sortedChoice by remember { mutableStateOf<SortRecentlyBy<*>>(SortRecentlyBy.TIMESTAMP) }

        var reverse by remember { mutableStateOf(false) }

        var clearAllDialog by remember { mutableStateOf(false) }

        if (clearAllDialog) {

            val onDismissRequest = { clearAllDialog = false }

            AlertDialog(
                onDismissRequest = onDismissRequest,
                title = { Text(stringResource(R.string.clear_all_history)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            lifecycleScope.launch(Dispatchers.IO) { println("Deleted " + dao.deleteAllRecentHistory() + " rows") }
                            onDismissRequest()
                        }
                    ) { Text(stringResource(R.string.yes), style = MaterialTheme.typography.button) }
                },
                dismissButton = {
                    TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.no), style = MaterialTheme.typography.button) }
                }
            )

        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.Close, null) } },
                    title = { Text(stringResource(R.string.history)) },
                    actions = {

                        IconButton(onClick = { clearAllDialog = true }) { Icon(Icons.Default.DeleteForever, null) }

                        val rotateIcon: @Composable (SortRecentlyBy<*>) -> Float = {
                            animateFloatAsState(if (it == sortedChoice && reverse) 180f else 0f).value
                        }

                        GroupButton(
                            selected = sortedChoice,
                            options = listOf(
                                GroupButtonModel(SortRecentlyBy.TITLE) {
                                    Icon(
                                        Icons.Default.SortByAlpha,
                                        null,
                                        modifier = Modifier.rotate(rotateIcon(SortRecentlyBy.TITLE))
                                    )
                                },
                                GroupButtonModel(SortRecentlyBy.TIMESTAMP) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        null,
                                        modifier = Modifier.rotate(rotateIcon(SortRecentlyBy.TIMESTAMP))
                                    )
                                }
                            )
                        ) { if (sortedChoice != it) sortedChoice = it else reverse = !reverse }
                    }
                )
            }
        ) { p ->
            LazyColumn(
                contentPadding = p,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    recentItems
                        .let {
                            when (val s = sortedChoice) {
                                is SortRecentlyBy.TITLE -> it.sortedBy(s.sort)
                                is SortRecentlyBy.TIMESTAMP -> it.sortedByDescending(s.sort)
                            }
                        }
                        .let { if (reverse) it.reversed() else it }
                ) { HistoryItem(it, scope) }
            }
        }

    }

    @ExperimentalMaterialApi
    @Composable
    private fun HistoryItem(item: RecentModel, scope: CoroutineScope) {
        var showPopup by remember { mutableStateOf(false) }

        if (showPopup) {
            val onDismiss = { showPopup = false }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.removeNoti, item.title)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch { dao.deleteRecent(item) }
                            onDismiss()
                        }
                    ) { Text(stringResource(R.string.yes), style = MaterialTheme.typography.button) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no), style = MaterialTheme.typography.button) } }
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
                    info.toSource(item.source)
                        ?.getSourceByUrl(item.url)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.doOnError { context?.showErrorToast() }
                        ?.subscribeBy { m ->
                            findNavController().navigate(RecentlyViewedFragmentDirections.actionRecentlyViewedFragmentToDetailsFragment(m))
                        }
                        ?.addTo(disposable)
                }
            ) {
                ListItem(
                    text = { Text(item.title) },
                    overlineText = { Text(item.source) },
                    secondaryText = { Text(format.format(item.timestamp)) },
                    icon = {
                        Surface(shape = MaterialTheme.shapes.medium) {
                            GlideImage(
                                imageModel = item.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                                failure = {
                                    Image(
                                        painter = rememberDrawablePainter(
                                            AppCompatResources.getDrawable(LocalContext.current, logo.logoId)
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                    )
                                }
                            )
                        }
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showPopup = true }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            }

                            IconButton(
                                onClick = {
                                    info.toSource(item.source)
                                        ?.getSourceByUrl(item.url)
                                        ?.subscribeOn(Schedulers.io())
                                        ?.observeOn(AndroidSchedulers.mainThread())
                                        ?.subscribeBy { m ->
                                            findNavController()
                                                .navigate(RecentlyViewedFragmentDirections.actionRecentlyViewedFragmentToDetailsFragment(m))
                                        }
                                        ?.addTo(disposable)
                                }
                            ) { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) }
                        }
                    }
                )
            }
        }
    }

    sealed class SortRecentlyBy<K>(val sort: (RecentModel) -> K) {
        object TIMESTAMP : SortRecentlyBy<Long>(RecentModel::timestamp)
        object TITLE : SortRecentlyBy<String>(RecentModel::title)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}