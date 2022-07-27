package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import com.programmersbox.uiviews.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun <T> BottomSheetDeleteScaffold(
    modifier: Modifier = Modifier,
    listOfItems: List<T>,
    state: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    multipleTitle: String,
    onRemove: (T) -> Unit,
    onMultipleRemove: (SnapshotStateList<T>) -> Unit,
    deleteTitle: @Composable (T) -> String = { stringResource(R.string.remove) },
    customSingleRemoveDialog: (T) -> Boolean = { true },
    topAppBarScrollState: TopAppBarState = rememberTopAppBarState(),
    bottomScrollBehavior: TopAppBarScrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) },
    topBar: @Composable (() -> Unit)? = null,
    itemUi: @Composable (T) -> Unit,
    mainView: @Composable (PaddingValues, List<T>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BottomSheetScaffold(
        scaffoldState = state,
        modifier = Modifier
            .nestedScroll(bottomScrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = topBar,
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        sheetShape = MaterialTheme.shapes.medium.copy(CornerSize(4.dp), CornerSize(4.dp), CornerSize(0.dp), CornerSize(0.dp)),
        sheetPeekHeight = ButtonDefaults.MinHeight + 4.dp,
        sheetContent = {

            val itemsToDelete = remember { mutableStateListOf<T>() }

            LaunchedEffect(state) {
                snapshotFlow { state.bottomSheetState.isCollapsed }
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
                                scope.launch { state.bottomSheetState.collapse() }
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
                    Button(
                        onClick = {
                            scope.launch {
                                if (state.bottomSheetState.isCollapsed) state.bottomSheetState.expand()
                                else state.bottomSheetState.collapse()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(ButtonDefaults.MinHeight + 4.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    ) { Text(stringResource(R.string.delete_multiple)) }
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        containerColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .containerColor(scrollBehavior.state.collapsedFraction).value,
                        contentColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .titleContentColor(scrollBehavior.state.collapsedFraction).value
                    ) {
                        Button(
                            onClick = { scope.launch { state.bottomSheetState.collapse() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { Text(stringResource(id = R.string.cancel)) }

                        Button(
                            onClick = { showPopup = true },
                            enabled = itemsToDelete.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { Text(stringResource(id = R.string.remove)) }
                    }
                }
            ) {
                AnimatedLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = it,
                    modifier = Modifier.padding(5.dp),
                    items = listOfItems.fastMap { i ->
                        AnimatedLazyListItem(key = i.hashCode().toString(), value = i) {
                            DeleteItemView(
                                item = i,
                                deleteItemList = itemsToDelete,
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
                    modifier = Modifier.padding(5.dp)
                ) {
                    items(listOfItems) { i ->
                        DeleteItemView(
                            item = i,
                            deleteItemList = itemsToDelete,
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
@ExperimentalMaterialApi
@Composable
private fun <T> DeleteItemView(
    item: T,
    deleteItemList: SnapshotStateList<T>,
    customSingleRemoveDialog: (T) -> Boolean,
    deleteTitle: @Composable (T) -> String = { stringResource(R.string.remove) },
    onRemove: (T) -> Unit,
    itemUi: @Composable (T) -> Unit
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

    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                if (customSingleRemoveDialog(item)) {
                    showPopup = true
                }
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
            val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

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
        }
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (item in deleteItemList) deleteItemList.remove(item) else deleteItemList.add(item) },
            border = BorderStroke(
                animateDpAsState(targetValue = if (item in deleteItemList) 5.dp else 1.dp).value,
                animateColorAsState(if (item in deleteItemList) Color(0xfff44336) else MaterialTheme.colorScheme.outline).value
            )
        ) { itemUi(item) }
    }

}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun <T : Any> BottomSheetDeleteScaffoldPaging(
    modifier: Modifier = Modifier,
    listOfItems: LazyPagingItems<T>,
    state: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    multipleTitle: String,
    onRemove: (T) -> Unit,
    onMultipleRemove: (SnapshotStateList<T>) -> Unit,
    customSingleRemoveDialog: (T) -> Boolean = { true },
    bottomScrollBehavior: TopAppBarScrollBehavior,
    deleteTitle: @Composable (T) -> String = { stringResource(R.string.remove) },
    topBar: @Composable (() -> Unit)? = null,
    itemUi: @Composable (T) -> Unit,
    mainView: @Composable (PaddingValues, LazyPagingItems<T>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BottomSheetScaffold(
        scaffoldState = state,
        modifier = Modifier
            .nestedScroll(bottomScrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = topBar,
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        sheetShape = MaterialTheme.shapes.medium.copy(CornerSize(4.dp), CornerSize(4.dp), CornerSize(0.dp), CornerSize(0.dp)),
        sheetPeekHeight = ButtonDefaults.MinHeight + 4.dp,
        sheetContent = {

            val itemsToDelete = remember { mutableStateListOf<T>() }

            LaunchedEffect(state) {
                snapshotFlow { state.bottomSheetState.isCollapsed }
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
                                scope.launch { state.bottomSheetState.collapse() }
                                onMultipleRemove(itemsToDelete)
                            }
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
                )

            }

            val topAppBarScrollState = rememberTopAppBarState()
            val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Button(
                        onClick = {
                            scope.launch {
                                if (state.bottomSheetState.isCollapsed) state.bottomSheetState.expand()
                                else state.bottomSheetState.collapse()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(ButtonDefaults.MinHeight + 4.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    ) { Text(stringResource(R.string.delete_multiple)) }
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        containerColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .containerColor(scrollBehavior.state.collapsedFraction).value,
                        contentColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .titleContentColor(scrollBehavior.state.collapsedFraction).value
                    ) {
                        Button(
                            onClick = { scope.launch { state.bottomSheetState.collapse() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { Text(stringResource(id = R.string.cancel)) }

                        Button(
                            onClick = { showPopup = true },
                            enabled = itemsToDelete.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { Text(stringResource(id = R.string.remove)) }
                    }
                }
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = it,
                    modifier = Modifier.padding(5.dp)
                ) {
                    items(listOfItems, key = { i -> i.hashCode().toString() }) { i ->
                        i?.let { d ->
                            DeleteItemView(
                                item = d,
                                selectedForDeletion = d in itemsToDelete,
                                onClick = { item -> if (item in itemsToDelete) itemsToDelete.remove(item) else itemsToDelete.add(item) },
                                customSingleRemoveDialog = customSingleRemoveDialog,
                                deleteTitle = deleteTitle,
                                onRemove = onRemove,
                                itemUi = itemUi
                            )
                        }
                    }
                }
            }
        }
    ) { mainView(it, listOfItems) }
}

@ExperimentalMaterialApi
@Composable
private fun <T : Any> DeleteItemView(
    item: T,
    selectedForDeletion: Boolean,
    deleteTitle: @Composable (T) -> String = { stringResource(R.string.remove) },
    onClick: (T) -> Unit,
    customSingleRemoveDialog: (T) -> Boolean,
    onRemove: (T) -> Unit,
    itemUi: @Composable (T) -> Unit
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

    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                if (customSingleRemoveDialog(item)) {
                    showPopup = true
                }
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
            val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

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
        }
    ) {
        Surface(
            tonalElevation = 5.dp,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick(item) },
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(
                animateDpAsState(targetValue = if (selectedForDeletion) 5.dp else 1.dp).value,
                animateColorAsState(if (selectedForDeletion) Color(0xfff44336) else Color.Transparent).value
            )
        ) { itemUi(item) }
    }

}