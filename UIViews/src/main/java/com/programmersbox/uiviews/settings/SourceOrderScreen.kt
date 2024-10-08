package com.programmersbox.uiviews.settings

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedSplitButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.components.plus
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourceOrderScreen(
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    itemDao: ItemDao = LocalItemDao.current,
) {
    val sourcesInOrder by sourceRepository
        .sources
        .collectAsStateWithLifecycle(emptyList())

    //var showSourceChooser by showSourceChooser()

    val sourceOrder by itemDao
        .getSourceOrder()
        .collectAsStateWithLifecycle(emptyList())

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

    var checked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton() },
                title = { Text("Source Order") },
                actions = {
                    ElevatedSplitButton(
                        onLeadingButtonClick = {
                            //showSourceChooser = true
                        },
                        checked = checked,
                        onTrailingButtonClick = { checked = !checked },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Edit,
                                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                contentDescription = "Localized description"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Custom")
                        },
                        trailingContent = {
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
                    )
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