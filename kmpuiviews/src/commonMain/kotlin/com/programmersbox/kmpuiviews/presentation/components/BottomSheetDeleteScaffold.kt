package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.dragselectcompose.core.rememberDragSelectState
import com.dragselectcompose.extensions.dragSelectToggleableItem
import com.dragselectcompose.grid.LazyDragSelectVerticalGrid
import com.dragselectcompose.grid.indicator.SelectedIcon
import com.dragselectcompose.grid.indicator.UnselectedIcon
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.areYouSureRemove
import otakuworld.kmpuiviews.generated.resources.are_you_sure_delete_notifications
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.delete_multiple
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.remove
import otakuworld.kmpuiviews.generated.resources.yes

@ExperimentalMaterial3Api
@Composable
fun <T> BottomSheetDeleteScaffold(
    listOfItems: List<T>,
    multipleTitle: String,
    onRemove: (T) -> Unit,
    onMultipleRemove: (SnapshotStateList<T>) -> Unit,
    itemUi: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    state: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    deleteTitle: @Composable (T) -> String = { stringResource(Res.string.remove) },
    customSingleRemoveDialog: (T) -> Boolean = { true },
    bottomScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    topBar: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    mainView: @Composable (PaddingValues, List<T>) -> Unit,
) {
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = state,
        containerColor = containerColor,
        modifier = modifier.nestedScroll(bottomScrollBehavior.nestedScrollConnection),
        topBar = topBar,
        sheetContent = {

            val itemsToDelete = remember { mutableStateListOf<T>() }

            LaunchedEffect(state) {
                snapshotFlow { state.bottomSheetState.currentValue == SheetValue.PartiallyExpanded }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { itemsToDelete.clear() }
            }

            var showPopup by remember { mutableStateOf(false) }

            if (showPopup) {
                val onDismiss = { showPopup = false }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(multipleTitle) },
                    text = {
                        Text(
                            pluralStringResource(
                                Res.plurals.areYouSureRemove,
                                itemsToDelete.size,
                                itemsToDelete.size
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDismiss()
                                scope.launch { state.bottomSheetState.partialExpand() }
                                onMultipleRemove(itemsToDelete)
                            }
                        ) { Text(stringResource(Res.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.no))
                        }
                    }
                )
            }

            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(Res.string.delete_multiple)) },
                        windowInsets = WindowInsets(0.dp),
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        windowInsets = WindowInsets(0.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { state.bottomSheetState.partialExpand() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(Res.string.cancel)) }

                        Button(
                            onClick = { showPopup = true },
                            enabled = itemsToDelete.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(Res.string.remove)) }
                    }
                }
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = it,
                    modifier = Modifier.padding(4.dp),
                ) {
                    items(
                        listOfItems,
                        key = { i -> i.hashCode().toString() }
                    ) { i ->
                        DeleteItemView(
                            item = i,
                            isInList = i in itemsToDelete,
                            onAddOrRemove = { item ->
                                if (item in itemsToDelete) itemsToDelete.remove(item) else itemsToDelete.add(item)
                            },
                            deleteTitle = deleteTitle,
                            customSingleRemoveDialog = customSingleRemoveDialog,
                            onRemove = onRemove,
                            itemUi = itemUi
                        )
                    }
                }
                /*LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = it,
                    modifier = Modifier.padding(4.dp)
                ) {
                    items(listOfItems) { i ->
                        DeleteItemView(
                            item = i,
                            isInList = i in itemsToDelete,
                            onAddOrRemove = { item ->
                                if (item in itemsToDelete) itemsToDelete.remove(item) else itemsToDelete.add(item)
                            },
                            deleteTitle = deleteTitle,
                            customSingleRemoveDialog = customSingleRemoveDialog,
                            onRemove = onRemove,
                            itemUi = itemUi
                        )
                    }
                }*/
            }
        }
    ) { mainView(it, listOfItems) }
}

@ExperimentalMaterial3Api
@Composable
fun <T> BottomSheetDeleteGridScaffold(
    listOfItems: List<T>,
    multipleTitle: String,
    onRemove: (T) -> Unit,
    onMultipleRemove: (List<T>) -> Unit,
    itemUi: @Composable (T) -> Unit,
    gridCells: GridCells,
    modifier: Modifier = Modifier,
    state: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    deleteTitle: @Composable (T) -> String = { stringResource(Res.string.remove) },
    customSingleRemoveDialog: (T) -> Boolean = { true },
    bottomScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    topBar: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    mainView: @Composable (PaddingValues, List<T>) -> Unit,
) {
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = state,
        containerColor = containerColor,
        modifier = modifier.nestedScroll(bottomScrollBehavior.nestedScrollConnection),
        topBar = topBar,
        sheetContent = {
            val dragState = rememberDragSelectState<T>()

            LaunchedEffect(state) {
                snapshotFlow { state.bottomSheetState.currentValue == SheetValue.PartiallyExpanded }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { dragState.disableSelectionMode() }
            }

            var showPopup by remember { mutableStateOf(false) }

            if (showPopup) {
                val onDismiss = { showPopup = false }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(multipleTitle) },
                    text = {
                        Text(
                            pluralStringResource(
                                Res.plurals.areYouSureRemove,
                                dragState.selected.size,
                                dragState.selected.size
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDismiss()
                                scope.launch { state.bottomSheetState.partialExpand() }
                                onMultipleRemove(dragState.selected)
                            }
                        ) { Text(stringResource(Res.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.no))
                        }
                    }
                )
            }

            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(Res.string.delete_multiple)) },
                        windowInsets = WindowInsets(0.dp),
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        windowInsets = WindowInsets(0.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { state.bottomSheetState.partialExpand() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(Res.string.cancel)) }

                        Button(
                            onClick = { showPopup = true },
                            enabled = dragState.selected.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(Res.string.remove)) }
                    }
                }
            ) { p ->
                LazyDragSelectVerticalGrid(
                    columns = gridCells,
                    items = listOfItems,
                    contentPadding = p,
                    state = dragState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items { m ->
                        Box {
                            DeleteItemDragSelectView(
                                item = m,
                                deleteTitle = deleteTitle,
                                customSingleRemoveDialog = customSingleRemoveDialog,
                                onRemove = onRemove,
                                itemUi = itemUi,
                                modifier = Modifier
                                    .dragSelectToggleableItem(
                                        state = dragState,
                                        item = m,
                                    )
                                    .clickable {
                                        if (dragState.isSelected(m)) dragState.removeSelected(m)
                                        else dragState.addSelected(m)
                                    }
                            )
                            if (dragState.inSelectionMode) {
                                Box(modifier = Modifier.matchParentSize()) {
                                    if (dragState.isSelected(m)) {
                                        SelectedIcon()
                                    } else {
                                        UnselectedIcon()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { mainView(it, listOfItems) }
}

//TODO: Try this out for a bit
@ExperimentalMaterial3Api
@Composable
fun <T> BottomSheetDeleteGridScaffold(
    listOfItems: List<T>,
    multipleTitle: String,
    onRemove: (T) -> Unit,
    onMultipleRemove: (List<T>) -> Unit,
    itemUi: @Composable (T) -> Unit,
    gridCells: GridCells,
    modifier: Modifier = Modifier,
    isTitle: (T) -> Boolean = { false },
    titleUi: @Composable (T) -> Unit = {},
    span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    state: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    deleteTitle: @Composable (T) -> String = { stringResource(Res.string.remove) },
    customSingleRemoveDialog: (T) -> Boolean = { true },
    bottomScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    topBar: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    mainView: @Composable (PaddingValues, List<T>) -> Unit,
) {
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = state,
        containerColor = containerColor,
        modifier = modifier.nestedScroll(bottomScrollBehavior.nestedScrollConnection),
        topBar = topBar,
        sheetContent = {
            val dragState = rememberDragSelectState<T>()

            LaunchedEffect(state) {
                snapshotFlow { state.bottomSheetState.currentValue == SheetValue.PartiallyExpanded }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { dragState.disableSelectionMode() }
            }

            var showPopup by remember { mutableStateOf(false) }

            if (showPopup) {
                val onDismiss = { showPopup = false }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(multipleTitle) },
                    text = {
                        Text(
                            pluralStringResource(
                                Res.plurals.areYouSureRemove,
                                dragState.selected.size,
                                dragState.selected.size
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDismiss()
                                scope.launch { state.bottomSheetState.partialExpand() }
                                onMultipleRemove(dragState.selected)
                            }
                        ) { Text(stringResource(Res.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.no))
                        }
                    }
                )
            }

            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(Res.string.delete_multiple)) },
                        windowInsets = WindowInsets(0.dp),
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        windowInsets = WindowInsets(0.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { state.bottomSheetState.partialExpand() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(Res.string.cancel)) }

                        Button(
                            onClick = { showPopup = true },
                            enabled = dragState.selected.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(Res.string.remove)) }
                    }
                }
            ) { p ->
                LazyDragSelectVerticalGrid(
                    columns = gridCells,
                    items = listOfItems,
                    contentPadding = p,
                    state = dragState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        span = span
                    ) { m ->
                        if (isTitle(m)) {
                            titleUi(m)
                        } else {
                            Box {
                                DeleteItemDragSelectView(
                                    item = m,
                                    deleteTitle = deleteTitle,
                                    customSingleRemoveDialog = customSingleRemoveDialog,
                                    onRemove = onRemove,
                                    itemUi = itemUi,
                                    modifier = Modifier
                                        .dragSelectToggleableItem(
                                            state = dragState,
                                            item = m,
                                        )
                                        .clickable {
                                            if (dragState.isSelected(m)) dragState.removeSelected(m)
                                            else dragState.addSelected(m)
                                        }
                                )
                                if (dragState.inSelectionMode) {
                                    Box(modifier = Modifier.matchParentSize()) {
                                        if (dragState.isSelected(m)) {
                                            SelectedIcon()
                                        } else {
                                            UnselectedIcon()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { mainView(it, listOfItems) }
}

@ExperimentalMaterial3Api
@Composable
fun <T> ModalBottomSheetDelete(
    listOfItems: List<T>,
    multipleTitle: String,
    onRemove: (T) -> Unit,
    onMultipleRemove: (List<T>) -> Unit,
    onDismiss: () -> Unit,
    itemUi: @Composable (T) -> Unit,
    gridCells: GridCells,
    isTitle: (T) -> Boolean = { false },
    titleUi: @Composable (T) -> Unit = {},
    span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    state: SheetState = rememberModalBottomSheetState(),
    deleteTitle: @Composable (T) -> String = { stringResource(Res.string.remove) },
    customSingleRemoveDialog: (T) -> Boolean = { true },
) {
    val dragState = rememberDragSelectState<T>()

    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(multipleTitle) },
            text = {
                Text(
                    pluralStringResource(
                        Res.plurals.areYouSureRemove,
                        dragState.selected.size,
                        dragState.selected.size
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        onMultipleRemove(dragState.selected)
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.delete_multiple)) },
                    actions = {
                        var showPopup by remember { mutableStateOf(false) }

                        if (showPopup) {
                            val onDismiss = { showPopup = false }
                            AlertDialog(
                                onDismissRequest = onDismiss,
                                title = { Text(stringResource(Res.string.are_you_sure_delete_notifications)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            onDismiss()
                                            onMultipleRemove(listOfItems)
                                        }
                                    ) { Text(stringResource(Res.string.yes)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = onDismiss) {
                                        Text(stringResource(Res.string.no))
                                    }
                                }
                            )
                        }

                        IconButton(onClick = { showPopup = true }) { Icon(Icons.Default.ClearAll, null) }
                    },
                    windowInsets = WindowInsets(0.dp),
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                BottomAppBar(
                    contentPadding = PaddingValues(0.dp),
                    windowInsets = WindowInsets(0.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(Res.string.cancel)) }

                    Button(
                        onClick = { showPopup = true },
                        enabled = dragState.selected.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(Res.string.remove)) }
                }
            }
        ) { p ->
            LazyDragSelectVerticalGrid(
                columns = gridCells,
                items = listOfItems,
                contentPadding = p,
                state = dragState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    span = span
                ) { m ->
                    if (isTitle(m)) {
                        titleUi(m)
                    } else {
                        Box {
                            DeleteItemDragSelectView(
                                item = m,
                                deleteTitle = deleteTitle,
                                customSingleRemoveDialog = customSingleRemoveDialog,
                                onRemove = onRemove,
                                itemUi = itemUi,
                                modifier = Modifier
                                    .dragSelectToggleableItem(
                                        state = dragState,
                                        item = m,
                                    )
                                    .clickable {
                                        if (dragState.isSelected(m)) dragState.removeSelected(m)
                                        else dragState.addSelected(m)
                                    }
                            )
                            if (dragState.inSelectionMode) {
                                Box(modifier = Modifier.matchParentSize()) {
                                    if (dragState.isSelected(m)) {
                                        SelectedIcon()
                                    } else {
                                        UnselectedIcon()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DeleteItemView(
    item: T,
    isInList: Boolean,
    onAddOrRemove: (T) -> Unit,
    customSingleRemoveDialog: (T) -> Boolean,
    onRemove: (T) -> Unit,
    itemUi: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    deleteTitle: @Composable (T) -> String = { stringResource(Res.string.remove) },
) {
    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(deleteTitle(item)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        onRemove(item)
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                if (customSingleRemoveDialog(item)) {
                    showPopup = true
                }
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
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
            val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.scale(scale)
                )
            }
        },
        content = {
            val transition = updateTransition(targetState = isInList, label = "")
            val outlineColor = MaterialTheme.colorScheme.outline
            OutlinedCard(
                onClick = { onAddOrRemove(item) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    transition.animateDp(label = "border_width") { target -> if (target) 4.dp else 1.dp }.value,
                    transition.animateColor(label = "border_color") { target -> if (target) Color(0xfff44336) else outlineColor }.value
                )
            ) { itemUi(item) }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DeleteItemDragSelectView(
    item: T,
    customSingleRemoveDialog: (T) -> Boolean,
    onRemove: (T) -> Unit,
    itemUi: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    deleteTitle: @Composable (T) -> String = { stringResource(Res.string.remove) },
) {
    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        val onDismiss = { showPopup = false }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(deleteTitle(item)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        onRemove(item)
                    }
                ) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                if (customSingleRemoveDialog(item)) {
                    showPopup = true
                }
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
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
            val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        color,
                        MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.scale(scale)
                )
            }
        },
        content = { itemUi(item) },
        modifier = modifier
    )
}
