package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.rememberImagePainter
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class HistoryFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = HistoryFragment()
    }

    private val dao by lazy { HistoryDatabase.getInstance(requireContext()).historyDao() }
    private val info by inject<GenericInfo>()
    private val logo: MainLogo by inject()
    private val disposable = CompositeDisposable()

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent { M3MaterialTheme(currentColorScheme) { RecentlyViewedUi() } }
        }

    class HistoryViewModel(private val dao: HistoryDao) : ViewModel() {
        val historyItems = Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = true
            )
        ) { dao.getRecentlyViewedPaging() }
    }

    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @Composable
    private fun RecentlyViewedUi(hm: HistoryViewModel = viewModel(factory = factoryCreate { HistoryViewModel(dao) })) {

        val recentItems = hm.historyItems.flow.collectAsLazyPagingItems()
        val scope = rememberCoroutineScope()

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
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.no)) } }
            )

        }

        val scrollBehavior = remember { TopAppBarDefaults.exitUntilCollapsedScrollBehavior(exponentialDecay()) }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                    },
                    title = { Text(stringResource(R.string.history)) },
                    actions = {
                        IconButton(onClick = { clearAllDialog = true }) { Icon(Icons.Default.DeleteForever, null) }
                    }
                )
            }
        ) { p ->
            LazyColumn(
                contentPadding = p,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recentItems) { item ->
                    if (item != null) {
                        HistoryItem(item, scope)
                    } else {
                        HistoryItemPlaceholder()
                    }
                }
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
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        ) {

            var showLoadingDialog by remember { mutableStateOf(false) }

            if (showLoadingDialog) {
                Dialog(
                    onDismissRequest = { showLoadingDialog = false },
                    DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(M3MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
                    ) {
                        Column {
                            CircularProgressIndicator(
                                color = M3MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Text(text = stringResource(id = R.string.loading), Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }

            Surface(
                onClick = {
                    info.toSource(item.source)
                        ?.getSourceByUrl(item.url)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.doOnError {
                            showLoadingDialog = false
                            context?.showErrorToast()
                        }
                        ?.doOnSubscribe { showLoadingDialog = true }
                        ?.subscribeBy { m ->
                            showLoadingDialog = false
                            findNavController().navigate(HistoryFragmentDirections.actionHistoryFragmentToDetailsFragment(m))
                        }
                        ?.addTo(disposable)
                },
                tonalElevation = 5.dp,
                shape = MaterialTheme.shapes.medium,
                indication = rememberRipple()
            ) {
                ListItem(
                    text = { Text(item.title) },
                    overlineText = { Text(item.source) },
                    secondaryText = { Text(requireContext().getSystemDateTimeFormat().format(item.timestamp)) },
                    icon = {
                        Surface(shape = MaterialTheme.shapes.medium) {
                            Image(
                                painter = rememberImagePainter(data = item.imageUrl) {
                                    placeholder(logo.logoId)
                                    error(logo.logoId)
                                    crossfade(true)
                                    lifecycle(LocalLifecycleOwner.current)
                                    size(ComposableUtils.IMAGE_WIDTH_PX, ComposableUtils.IMAGE_HEIGHT_PX)
                                },
                                contentScale = ContentScale.FillBounds,
                                contentDescription = item.title,
                                modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            )
                        }
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showPopup = true }) { Icon(imageVector = Icons.Default.Delete, contentDescription = null) }
                            IconButton(
                                onClick = {
                                    info.toSource(item.source)
                                        ?.getSourceByUrl(item.url)
                                        ?.subscribeOn(Schedulers.io())
                                        ?.observeOn(AndroidSchedulers.mainThread())
                                        ?.subscribeBy { m ->
                                            findNavController().navigate(HistoryFragmentDirections.actionHistoryFragmentToDetailsFragment(m))
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

    @ExperimentalMaterialApi
    @Composable
    private fun HistoryItemPlaceholder() {
        val placeholderColor = androidx.compose.material3.contentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
            .copy(0.1f)
            .compositeOver(M3MaterialTheme.colorScheme.surface)

        Surface(
            tonalElevation = 5.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.placeholder(true, color = placeholderColor)
        ) {
            ListItem(
                modifier = Modifier.placeholder(true, color = placeholderColor),
                text = { Text("Otaku") },
                overlineText = { Text("Otaku") },
                secondaryText = { Text("Otaku") },
                icon = {
                    Surface(shape = MaterialTheme.shapes.medium) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    }
                },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
            )
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