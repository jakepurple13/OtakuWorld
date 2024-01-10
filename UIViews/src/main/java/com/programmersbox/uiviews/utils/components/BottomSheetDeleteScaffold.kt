package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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
    deleteTitle: @Composable (T) -> String = { stringResource(R.string.remove) },
    customSingleRemoveDialog: (T) -> Boolean = { true },
    bottomScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    topBar: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    mainView: @Composable (PaddingValues, List<T>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                            context.resources.getQuantityString(
                                R.plurals.areYouSureRemove,
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
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
                )
            }

            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.delete_multiple)) },
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
                        ) { Text(stringResource(id = R.string.cancel)) }

                        Button(
                            onClick = { showPopup = true },
                            enabled = itemsToDelete.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(id = R.string.remove)) }
                    }
                }
            ) {
                AnimatedLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = it,
                    modifier = Modifier.padding(4.dp),
                    items = listOfItems.fastMap { i ->
                        AnimatedLazyListItem(key = i.hashCode().toString(), value = i) {
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
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@LightAndDarkPreviews
@Composable
private fun BottomSheetDeleteScaffoldPreview() {
    PreviewTheme {
        val state: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(SheetValue.Expanded)
        )
        BottomSheetDeleteScaffold(
            listOfItems = listOf(1, 2, 3, 4, 5),
            multipleTitle = "Delete",
            customSingleRemoveDialog = { false },
            onRemove = {},
            onMultipleRemove = {},
            itemUi = { Text(it.toString()) },
            state = state
        ) { padding, list ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
            ) {
                items(list) {
                    Text(it.toString())
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
    deleteTitle: @Composable (T) -> String = { stringResource(R.string.remove) },
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
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
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

@LightAndDarkPreviews
@Composable
private fun DeleteItemPreview() {
    PreviewTheme {
        Column {
            DeleteItemView(
                item = 1,
                isInList = false,
                onAddOrRemove = {},
                customSingleRemoveDialog = { false },
                onRemove = {},
                itemUi = { Text(it.toString()) }
            )

            DeleteItemView(
                item = 1,
                isInList = true,
                onAddOrRemove = {},
                customSingleRemoveDialog = { false },
                onRemove = {},
                itemUi = { Text(it.toString()) }
            )
        }
    }
}