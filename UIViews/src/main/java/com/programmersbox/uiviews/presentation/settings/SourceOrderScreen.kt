package com.programmersbox.uiviews.presentation.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.uiviews.theme.LocalItemDao
import com.programmersbox.uiviews.theme.LocalSourcesRepository
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourceOrderScreen(
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    itemDao: ItemDao = LocalItemDao.current,
) {
    val scope = rememberCoroutineScope()

    val sourcesInOrder by sourceRepository
        .sources
        .collectAsStateWithLifecycle(emptyList())

    val sourceOrder by itemDao
        .getSourceOrder()
        .collectAsStateWithLifecycle(emptyList())

    var sourceSortOrder by remember { mutableStateOf(SourceSort.Custom) }

    val modifiedOrder = remember(sourceOrder.isNotEmpty()) {
        sourceOrder
            .sortedBy { it.order }
            .toMutableStateList()
    }

    LaunchedEffect(sourcesInOrder) {
        sourcesInOrder.forEachIndexed { index, sourceInformation ->
            itemDao.insertSourceOrder(
                SourceOrder(
                    source = sourceInformation.packageName,
                    name = sourceInformation.apiService.serviceName,
                    order = index
                )
            )
        }
    }

    val haptic = LocalHapticFeedback.current

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        sourceSortOrder = SourceSort.Custom
        runCatching {
            val tmp = modifiedOrder[to.index].copy(order = from.index)
            modifiedOrder[to.index] = modifiedOrder[from.index].copy(order = to.index)
            modifiedOrder[from.index] = tmp
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            modifiedOrder.forEach { itemDao.updateSourceOrder(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton() },
                title = { Text("Source Order") },
                actions = {
                    var checked by remember { mutableStateOf(false) }
                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.ElevatedLeadingButton(
                                onClick = {
                                    scope.launch { sourceSortOrder.sorting(sourceOrder, itemDao, modifiedOrder) }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                    contentDescription = "Localized description"
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(sourceSortOrder.name)
                            }
                        },
                        trailingButton = {
                            SplitButtonDefaults.ElevatedTrailingButton(
                                checked = checked,
                                onCheckedChange = { checked = it }
                            ) {
                                val rotation: Float by animateFloatAsState(
                                    targetValue = if (checked) 180f else 0f,
                                    label = "Trailing Icon Rotation"
                                )
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    modifier =
                                    Modifier
                                        .size(SplitButtonDefaults.TrailingIconSize)
                                        .graphicsLayer {
                                            this.rotationZ = rotation
                                        },
                                    contentDescription = "Localized description"
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = checked,
                        onDismissRequest = { checked = false },
                    ) {
                        @Composable
                        fun SortDropDownMenuItem(
                            sourceSort: SourceSort,
                            leadingIcon: @Composable () -> Unit,
                        ) {
                            DropdownMenuItem(
                                text = { Text(sourceSort.name) },
                                onClick = {
                                    checked = false
                                    sourceSortOrder = sourceSort
                                    scope.launch {
                                        sourceSort.sorting(sourceOrder, itemDao, modifiedOrder)
                                    }
                                },
                                leadingIcon = leadingIcon
                            )
                        }

                        SortDropDownMenuItem(
                            sourceSort = SourceSort.Alphabetical,
                            leadingIcon = { Icon(Icons.Filled.SortByAlpha, contentDescription = "Sort By Alpha") }
                        )

                        SortDropDownMenuItem(
                            sourceSort = SourceSort.Custom,
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Custom") }
                        )

                        SortDropDownMenuItem(
                            sourceSort = SourceSort.Random,
                            leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = "Random") }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding + LocalNavHostPadding.current,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = modifiedOrder,
                key = { it.source }
            ) { item ->
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = item.source,
                ) {
                    val interactionSource = remember { MutableInteractionSource() }

                    OutlinedCard(
                        onClick = {},
                        interactionSource = interactionSource,
                    ) {
                        ListItem(
                            leadingContent = { Text(item.order.toString()) },
                            headlineContent = { Text(item.name) },
                            trailingContent = {
                                IconButton(
                                    modifier = Modifier.longPressDraggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        interactionSource = interactionSource,
                                    ),
                                    onClick = {},
                                ) {
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

//TODO: put this into datastore
enum class SourceSort {
    Alphabetical {
        override suspend fun sorting(sourceOrder: List<SourceOrder>, itemDao: ItemDao, modifiedOrder: MutableList<SourceOrder>) {
            sourceOrder
                .sortedBy { it.name }
                .mapIndexed { index, so -> so.copy(order = index) }
                .onEach { itemDao.updateSourceOrder(it) }
                .let {
                    modifiedOrder.clear()
                    modifiedOrder.addAll(it)
                }
        }
    },
    Custom,
    Random {
        override suspend fun sorting(sourceOrder: List<SourceOrder>, itemDao: ItemDao, modifiedOrder: MutableList<SourceOrder>) {
            sourceOrder
                .shuffled()
                .mapIndexed { index, so -> so.copy(order = index) }
                .onEach { itemDao.updateSourceOrder(it) }
                .let {
                    modifiedOrder.clear()
                    modifiedOrder.addAll(it)
                }
        }
    };

    open suspend fun sorting(sourceOrder: List<SourceOrder>, itemDao: ItemDao, modifiedOrder: MutableList<SourceOrder>) {}
}