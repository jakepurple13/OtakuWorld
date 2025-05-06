package com.programmersbox.uiviews.presentation.history

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.GradientImage
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.dispatchIo
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@ExperimentalMaterial3Api
@Composable
fun HistoryUi(
    dao: HistoryDao = LocalHistoryDao.current,
    hm: HistoryViewModel = koinViewModel(),
) {
    val recentItems = hm.historyItems.collectAsLazyPagingItems()
    val recentSize by hm.historyCount.collectAsStateWithLifecycle(0)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var clearAllDialog by remember { mutableStateOf(false) }

    val logoDrawable = koinInject<AppLogo>().logo

    if (clearAllDialog) {
        val onDismissRequest = { clearAllDialog = false }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.clear_all_history)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) { println("Deleted " + dao.deleteAllRecentHistory() + " rows") }
                        onDismissRequest()
                    }
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.no)) } }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showRecentModel by remember { mutableStateOf<RecentModel?>(null) }

    SourceNotInstalledModal(
        showItem = showRecentModel?.title,
        onShowItemDismiss = { showRecentModel = null },
        source = showRecentModel?.source
    )

    OtakuHazeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton() },
                title = { Text(stringResource(R.string.history)) },
                actions = {
                    Text("$recentSize")
                    IconButton(
                        onClick = { clearAllDialog = true }
                    ) { Icon(Icons.Default.DeleteForever, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
            )
        },
        blurTopBar = true,
        topBarBlur = {
            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
        }
    ) { p ->
        LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = recentItems.itemCount,
                key = recentItems.itemKey { it.url },
                contentType = recentItems.itemContentType { it }
            ) {
                val item = recentItems[it]
                if (item != null) {
                    HistoryItem(
                        item = item,
                        dao = dao,
                        logoDrawable = logoDrawable,
                        scope = scope,
                        onError = {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    "Something went wrong. Source might not be installed",
                                    duration = SnackbarDuration.Long,
                                    actionLabel = "More Options",
                                    withDismissAction = true
                                )
                                showRecentModel = when (result) {
                                    SnackbarResult.Dismissed -> null
                                    SnackbarResult.ActionPerformed -> item
                                }
                            }
                        },
                    )
                } else {
                    HistoryItemPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    item: RecentModel,
    dao: HistoryDao,
    logoDrawable: Drawable?,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
) {
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
        modifier = modifier,
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
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = ""
            )

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
        },
        content = {
            var showLoadingDialog by remember { mutableStateOf(false) }

            LoadingDialog(
                showLoadingDialog = showLoadingDialog,
                onDismissRequest = { showLoadingDialog = false }
            )

            val info = LocalSourcesRepository.current
            val navController = LocalNavController.current

            Surface(
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
                onClick = {
                    scope.launch {
                        info.toSourceByApiServiceName(item.source)
                            ?.apiService
                            ?.getSourceByUrlFlow(item.url)
                            ?.dispatchIo()
                            ?.onStart { showLoadingDialog = true }
                            ?.catch {
                                showLoadingDialog = false
                                onError()
                            }
                            ?.onEach { m ->
                                showLoadingDialog = false
                                navController.navigateToDetails(m)
                            }
                            ?.collect() ?: onError()
                    }
                }
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            item.title,
                            //modifier = Modifier.customSharedElement(OtakuTitleElement(source = item.title, origin = item.title))
                        )
                    },
                    overlineContent = { Text(item.source) },
                    supportingContent = { Text(LocalSystemDateTimeFormat.current.format(item.timestamp.toLocalDateTime())) },
                    leadingContent = {
                        GradientImage(
                            model = item.imageUrl,
                            placeholder = rememberDrawablePainter(logoDrawable),
                            error = rememberDrawablePainter(logoDrawable),
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                .clip(MaterialTheme.shapes.medium)
                            /*.customSharedElement(
                                OtakuImageElement(
                                    source = item.title,
                                    origin = item.imageUrl
                                )
                            )*/
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showPopup = true }
                            ) { Icon(imageVector = Icons.Default.Delete, contentDescription = null) }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        info.toSourceByApiServiceName(item.source)
                                            ?.apiService
                                            ?.getSourceByUrlFlow(item.url)
                                            ?.dispatchIo()
                                            ?.onStart { showLoadingDialog = true }
                                            ?.catch {
                                                showLoadingDialog = false
                                                onError()
                                            }
                                            ?.onEach { m ->
                                                showLoadingDialog = false
                                                navController.navigateToDetails(m)
                                            }
                                            ?.collect() ?: onError()
                                    }
                                }
                            ) { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) }
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun HistoryItemPlaceholder() {
    Surface(
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.m3placeholder(
            true,
            highlight = PlaceholderHighlight.shimmer()
        )
    ) {
        ListItem(
            headlineContent = { Text("Otaku") },
            overlineContent = { Text("Otaku") },
            supportingContent = { Text("Otaku") },
            leadingContent = {
                Surface(shape = MaterialTheme.shapes.medium) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                }
            },
            modifier = Modifier.m3placeholder(
                true,
                highlight = PlaceholderHighlight.shimmer()
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@LightAndDarkPreviews
@Composable
private fun HistoryScreenPreview() {
    PreviewTheme {
        HistoryUi()
    }
}

@LightAndDarkPreviews
@Composable
private fun HistoryItemPreview() {
    PreviewTheme {
        HistoryItem(
            item = RecentModel(
                title = "Title",
                description = "Description",
                url = "url",
                imageUrl = "imageUrl",
                source = "MANGA_READ"
            ),
            dao = LocalHistoryDao.current,
            scope = rememberCoroutineScope(),
            onError = {},
            logoDrawable = null
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun HistoryPlaceholderItemPreview() {
    PreviewTheme {
        HistoryItemPlaceholder()
    }
}