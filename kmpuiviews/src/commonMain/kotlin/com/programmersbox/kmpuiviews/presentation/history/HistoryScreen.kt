package com.programmersbox.kmpuiviews.presentation.history

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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemContentType
import app.cash.paging.compose.itemKey
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.GradientImage
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.BiometricOpen
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.printLogs
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.clear_all_history
import otakuworld.kmpuiviews.generated.resources.history
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.removeNoti
import otakuworld.kmpuiviews.generated.resources.yes

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
    val biometrics = rememberBiometricOpening()
    var clearAllDialog by remember { mutableStateOf(false) }

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    if (clearAllDialog) {
        val onDismissRequest = { clearAllDialog = false }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(Res.string.clear_all_history)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            printLogs { "Deleted " + dao.deleteAllRecentHistory() + " rows" }
                        }
                        onDismissRequest()
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(Res.string.no)) } }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showRecentModel by remember { mutableStateOf<RecentModel?>(null) }

    SourceNotInstalledModal(
        showItem = showRecentModel?.title,
        onShowItemDismiss = { showRecentModel = null },
        source = showRecentModel?.source,
        url = showRecentModel?.url
    )

    OtakuHazeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton() },
                title = { Text(stringResource(Res.string.history)) },
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
                        scope = scope,
                        biometrics = biometrics,
                        colorFilter = colorFilter,
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
fun HistoryItem(
    item: RecentModel,
    dao: HistoryDao,
    scope: CoroutineScope,
    biometrics: BiometricOpen,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    onError: () -> Unit,
) {
    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.removeNoti, item.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { dao.deleteRecent(item) }
                        onDismiss()
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } }
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
            val navController = LocalNavActions.current

            Surface(
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
                onClick = {
                    scope.launch {
                        biometrics.openIfNotIncognito(item.url, item.title) {
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
                                        navController.details(m)
                                    }
                                    ?.collect() ?: onError()
                            }
                        }
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
                        val logo = painterLogo()
                        GradientImage(
                            model = item.imageUrl,
                            placeholder = logo,
                            error = logo,
                            colorFilter = colorFilter,
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
                                        biometrics.openIfNotIncognito(item.url, item.title) {
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
                                                        navController.details(m)
                                                    }
                                                    ?.collect() ?: onError()
                                            }
                                        }
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
fun HistoryItemPlaceholder() {
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