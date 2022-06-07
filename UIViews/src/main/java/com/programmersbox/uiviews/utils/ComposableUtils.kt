package com.programmersbox.uiviews.utils

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.ChipColors
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.window.layout.WindowMetricsCalculator
import coil.Coil
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.uiviews.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

@Composable
fun StaggeredVerticalGrid(
    modifier: Modifier = Modifier,
    maxColumnWidth: Dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeableXY: MutableMap<Placeable, Pair<Int, Int>> = mutableMapOf()
        check(constraints.hasBoundedWidth) { "Unbounded width not supported" }
        val columns = ceil(constraints.maxWidth / maxColumnWidth.toPx()).toInt()
        val columnWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(maxWidth = columnWidth)
        val colHeights = IntArray(columns) { 0 } // track each column's height
        val placeables = measurables.map { measurable ->
            val column = shortestColumn(colHeights)
            val placeable = measurable.measure(itemConstraints)
            placeableXY[placeable] = Pair(columnWidth * column, colHeights[column])
            colHeights[column] += placeable.height
            placeable
        }

        val height = colHeights.maxOrNull()
            ?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            placeables.forEach { placeable ->
                placeable.place(
                    x = placeableXY.getValue(placeable).first,
                    y = placeableXY.getValue(placeable).second
                )
            }
        }
    }
}

@Composable
fun StaggeredVerticalGrid(
    modifier: Modifier = Modifier,
    columns: Int,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeableXY: MutableMap<Placeable, Pair<Int, Int>> = mutableMapOf()
        check(constraints.hasBoundedWidth) { "Unbounded width not supported" }
        val columnWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(maxWidth = columnWidth)
        val colHeights = IntArray(columns) { 0 } // track each column's height
        val placeables = measurables.map { measurable ->
            val column = shortestColumn(colHeights)
            val placeable = measurable.measure(itemConstraints)
            placeableXY[placeable] = Pair(columnWidth * column, colHeights[column])
            colHeights[column] += placeable.height
            placeable
        }

        val height = colHeights.maxOrNull()
            ?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            placeables.forEach { placeable ->
                placeable.place(
                    x = placeableXY.getValue(placeable).first,
                    y = placeableXY.getValue(placeable).second
                )
            }
        }
    }
}

private fun shortestColumn(colHeights: IntArray): Int {
    var minHeight = Int.MAX_VALUE
    var column = 0
    colHeights.forEachIndexed { index, height ->
        if (height < minHeight) {
            minHeight = height
            column = index
        }
    }
    return column
}

fun Int.toComposeColor() = Color(this)

@Composable
fun CustomChip2(
    category: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colors.onSurface,
    backgroundColor: Color = MaterialTheme.colors.surface
) {
    Surface(
        modifier = Modifier.then(modifier),
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row {
            Text(
                text = category,
                style = MaterialTheme.typography.body2,
                color = textColor,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterVertically),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CustomChip(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    border: BorderStroke? = null,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    leadingIcon: @Composable (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.labelColor(enabled)
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor(enabled).value,
        contentColor = contentColor.copy(1.0f),
        border = border,
        tonalElevation = 8.dp
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = M3MaterialTheme.typography.bodyMedium
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minHeight = ChipDefaults.MinHeight
                        )
                        .padding(
                            start = if (leadingIcon == null) {
                                12.dp
                            } else 0.dp,
                            end = 12.dp,
                        ),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Spacer(Modifier.width(4.dp))
                        val leadingIconContentColor by colors.leadingIconContentColor(enabled)
                        CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides leadingIconContentColor,
                            LocalContentAlpha provides leadingIconContentColor.alpha,
                            content = leadingIcon
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    content()
                }
            }
        }
    }
}

@ExperimentalAnimationApi
/**
 * @param state Use [updateAnimatedItemsState].
 */
inline fun <T> LazyListScope.animatedItems(
    state: List<AnimatedItem<T>>,
    enterTransition: EnterTransition = expandVertically(),
    exitTransition: ExitTransition = shrinkVertically(),
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = animatedItemsIndexed(state, enterTransition, exitTransition, key) { _, item -> itemContent(item) }

@ExperimentalAnimationApi
/**
 * @param state Use [updateAnimatedItemsState].
 */
inline fun <T> LazyListScope.animatedItemsIndexed(
    state: List<AnimatedItem<T>>,
    enterTransition: EnterTransition = expandVertically(),
    exitTransition: ExitTransition = shrinkVertically(),
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    items(
        state.size,
        if (key != null) { keyIndex: Int -> key(state[keyIndex].item) } else null
    ) { index ->
        val item = state[index]
        val visibility = item.visibility

        key(key?.invoke(item.item)) {
            AnimatedVisibility(
                visibleState = visibility,
                enter = enterTransition,
                exit = exitTransition
            ) { itemContent(index, item.item) }
        }
    }
}

@Composable
fun <T> updateAnimatedItemsState(
    newList: List<T>
): State<List<AnimatedItem<T>>> {
    val state = remember { mutableStateOf(emptyList<AnimatedItem<T>>()) }
    LaunchedEffect(newList) {
        if (state.value == newList) {
            return@LaunchedEffect
        }
        val oldList = state.value.toList()

        val diffCb = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].item == newList[newItemPosition]

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].item == newList[newItemPosition]

        }
        val diffResult = calculateDiff(false, diffCb)
        val compositeList = oldList.toMutableList()

        diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                for (i in 0 until count) {
                    val newItem = AnimatedItem(visibility = MutableTransitionState(false), newList[position + i])
                    newItem.visibility.targetState = true
                    compositeList.add(position + i, newItem)
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                for (i in 0 until count) {
                    compositeList[position + i].visibility.targetState = false
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                // not detecting moves.
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                // irrelevant with compose.
            }
        })
        if (state.value != compositeList) {
            state.value = compositeList
        }
        val initialAnimation = Animatable(1.0f)
        initialAnimation.animateTo(0f)
        state.value = state.value.filter { it.visibility.targetState }
    }

    return state
}

data class AnimatedItem<T>(
    val visibility: MutableTransitionState<Boolean>,
    val item: T,
) {
    override fun hashCode(): Int = item?.hashCode() ?: 0
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AnimatedItem<*>
        if (item != other.item) return false
        return true
    }
}

suspend fun calculateDiff(
    detectMoves: Boolean = true,
    diffCb: DiffUtil.Callback
): DiffUtil.DiffResult = withContext(Dispatchers.Unconfined) {
    DiffUtil.calculateDiff(diffCb, detectMoves)
}

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
    topAppBarScrollState: TopAppBarScrollState = rememberTopAppBarScrollState(),
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
        backgroundColor = M3MaterialTheme.colorScheme.background,
        contentColor = m3ContentColorFor(M3MaterialTheme.colorScheme.background),
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

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { androidx.compose.material3.Text(multipleTitle) },
                    text = {
                        androidx.compose.material3.Text(
                            context.resources.getQuantityString(
                                R.plurals.areYouSureRemove,
                                itemsToDelete.size,
                                itemsToDelete.size
                            )
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                onDismiss()
                                scope.launch { state.bottomSheetState.collapse() }
                                onMultipleRemove(itemsToDelete)
                            }
                        ) { androidx.compose.material3.Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text(stringResource(R.string.no)) } }
                )

            }

            val topAppBarScrollState = rememberTopAppBarScrollState()
            val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

            androidx.compose.material3.Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    androidx.compose.material3.Button(
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
                    ) { androidx.compose.material3.Text(stringResource(R.string.delete_multiple)) }
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        containerColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .containerColor(scrollFraction = scrollBehavior.scrollFraction).value,
                        contentColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .titleContentColor(scrollFraction = scrollBehavior.scrollFraction).value
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { scope.launch { state.bottomSheetState.collapse() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { androidx.compose.material3.Text(stringResource(id = R.string.cancel)) }

                        androidx.compose.material3.Button(
                            onClick = { showPopup = true },
                            enabled = itemsToDelete.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { androidx.compose.material3.Text(stringResource(id = R.string.remove)) }
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { androidx.compose.material3.Text(deleteTitle(item)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDismiss()
                        onRemove(item)
                    }
                ) { androidx.compose.material3.Text(stringResource(R.string.yes)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text(stringResource(R.string.no)) } }
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
                androidx.compose.material3.Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.scale(scale)
                )
            }
        }
    ) {
        androidx.compose.material3.OutlinedCard(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (item in deleteItemList) deleteItemList.remove(item) else deleteItemList.add(item) },
            border = BorderStroke(
                animateDpAsState(targetValue = if (item in deleteItemList) 5.dp else 1.dp).value,
                animateColorAsState(if (item in deleteItemList) Color(0xfff44336) else M3MaterialTheme.colorScheme.outline).value
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
        backgroundColor = M3MaterialTheme.colorScheme.background,
        contentColor = m3ContentColorFor(M3MaterialTheme.colorScheme.background),
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

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { androidx.compose.material3.Text(multipleTitle) },
                    text = {
                        androidx.compose.material3.Text(
                            context.resources.getQuantityString(
                                R.plurals.areYouSureRemove,
                                itemsToDelete.size,
                                itemsToDelete.size
                            )
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                onDismiss()
                                scope.launch { state.bottomSheetState.collapse() }
                                onMultipleRemove(itemsToDelete)
                            }
                        ) { androidx.compose.material3.Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text(stringResource(R.string.no)) } }
                )

            }

            val topAppBarScrollState = rememberTopAppBarScrollState()
            val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

            androidx.compose.material3.Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    androidx.compose.material3.Button(
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
                    ) { androidx.compose.material3.Text(stringResource(R.string.delete_multiple)) }
                },
                bottomBar = {
                    BottomAppBar(
                        contentPadding = PaddingValues(0.dp),
                        containerColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .containerColor(scrollFraction = scrollBehavior.scrollFraction).value,
                        contentColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .titleContentColor(scrollFraction = scrollBehavior.scrollFraction).value
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { scope.launch { state.bottomSheetState.collapse() } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { androidx.compose.material3.Text(stringResource(id = R.string.cancel)) }

                        androidx.compose.material3.Button(
                            onClick = { showPopup = true },
                            enabled = itemsToDelete.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp)
                        ) { androidx.compose.material3.Text(stringResource(id = R.string.remove)) }
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { androidx.compose.material3.Text(deleteTitle(item)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDismiss()
                        onRemove(item)
                    }
                ) { androidx.compose.material3.Text(stringResource(R.string.yes)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text(stringResource(R.string.no)) } }
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
                androidx.compose.material3.Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.scale(scale)
                )
            }
        }
    ) {
        androidx.compose.material3.Surface(
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

interface AutoCompleteEntity {
    fun filter(query: String): Boolean
}

private typealias ItemSelected<T> = (T) -> Unit

interface AutoCompleteScope<T : AutoCompleteEntity> : AutoCompleteDesignScope {
    var isSearching: Boolean
    fun filter(query: String)
    fun onItemSelected(block: ItemSelected<T> = {})
}

interface AutoCompleteDesignScope {
    var boxWidthPercentage: Float
    var shouldWrapContentHeight: Boolean
    var boxMaxHeight: Dp
    var boxBorderStroke: BorderStroke
    var boxShape: Shape
}

class AutoCompleteState<T : AutoCompleteEntity>(private val startItems: List<T>) : AutoCompleteScope<T> {
    private var onItemSelectedBlock: ItemSelected<T>? = null

    fun selectItem(item: T) {
        onItemSelectedBlock?.invoke(item)
    }

    private var filteredItems by mutableStateOf(startItems)
    override var isSearching by mutableStateOf(false)
    override var boxWidthPercentage by mutableStateOf(.9f)
    override var shouldWrapContentHeight by mutableStateOf(false)
    override var boxMaxHeight: Dp by mutableStateOf(androidx.compose.material3.TextFieldDefaults.MinHeight * 3)
    override var boxBorderStroke by mutableStateOf(BorderStroke(2.dp, Color.Black))
    override var boxShape: Shape by mutableStateOf(RoundedCornerShape(8.dp))

    override fun filter(query: String) {
        if (isSearching) filteredItems = startItems.filter { entity -> entity.filter(query) }
    }

    override fun onItemSelected(block: ItemSelected<T>) {
        onItemSelectedBlock = block
    }
}

interface ValueAutoCompleteEntity<T> : AutoCompleteEntity {
    val value: T
}

typealias CustomFilter<T> = (T, String) -> Boolean

fun <T> List<T>.asAutoCompleteEntities(filter: CustomFilter<T>): List<ValueAutoCompleteEntity<T>> {
    return fastMap {
        object : ValueAutoCompleteEntity<T> {
            override val value: T = it
            override fun filter(query: String): Boolean = filter(value, query)
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun <T : AutoCompleteEntity> AutoCompleteBox(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    trailingIcon: (@Composable (T) -> Unit)? = null,
    leadingIcon: (@Composable (T) -> Unit)? = null,
    content: @Composable AutoCompleteScope<T>.() -> Unit
) {
    val autoCompleteState = remember { AutoCompleteState(startItems = items) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        autoCompleteState.content()

        androidx.compose.material3.DropdownMenu(
            expanded = autoCompleteState.isSearching && items.isNotEmpty(),
            onDismissRequest = { },
            modifier = Modifier.autoComplete(autoCompleteState),
            properties = PopupProperties(focusable = false)
        ) {
            items.fastForEachIndexed { i, item ->
                androidx.compose.material3.DropdownMenuItem(
                    onClick = { autoCompleteState.selectItem(item) },
                    text = { itemContent(item) },
                    trailingIcon = trailingIcon?.let { { it.invoke(item) } },
                    leadingIcon = leadingIcon?.let { { it.invoke(item) } }
                )
                if (i < items.size - 1) MenuDefaults.Divider()
            }
        }
    }
}

private fun Modifier.autoComplete(
    autoCompleteItemScope: AutoCompleteDesignScope
): Modifier = composed {
    val baseModifier = if (autoCompleteItemScope.shouldWrapContentHeight)
        wrapContentHeight()
    else
        heightIn(0.dp, autoCompleteItemScope.boxMaxHeight)

    baseModifier
        .fillMaxWidth(autoCompleteItemScope.boxWidthPercentage)
        .border(
            border = autoCompleteItemScope.boxBorderStroke,
            shape = autoCompleteItemScope.boxShape
        )
}

private class SwipeToDismissBackground @ExperimentalMaterialApi constructor(
    val background: @Composable (DismissState) -> Unit
)

@ExperimentalMaterialApi
private val DEFAULT_SWIPE_TO_DISMISS_BACKGROUND
    get() = SwipeToDismissBackground { dismissState ->
        val direction = dismissState.dismissDirection ?: return@SwipeToDismissBackground
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

@ExperimentalMaterialApi
@Composable
fun CustomSwipeToDelete(
    modifier: Modifier = Modifier,
    dismissState: DismissState,
    dismissThresholds: (DismissDirection) -> androidx.compose.material.ThresholdConfig = { androidx.compose.material.FractionalThreshold(0.5f) },
    dismissDirections: Set<DismissDirection> = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
    backgroundInfo: @Composable (DismissState) -> Unit = DEFAULT_SWIPE_TO_DISMISS_BACKGROUND.background,
    content: @Composable () -> Unit
) {
    SwipeToDismiss(
        modifier = modifier,
        state = dismissState,
        directions = dismissDirections,
        dismissThresholds = dismissThresholds,
        background = { backgroundInfo(dismissState) }
    ) { content() }
}

@ExperimentalPermissionsApi
@Composable
fun PermissionRequest(permissionsList: List<String>, content: @Composable () -> Unit) {
    val storagePermissions = rememberMultiplePermissionsState(permissionsList)
    val context = LocalContext.current
    SideEffect { storagePermissions.launchMultiplePermissionRequest() }
    if (storagePermissions.allPermissionsGranted) {
        content()
    } else {
        if (storagePermissions.permissions.fastAll { it.status.shouldShowRationale }) {
            NeedsPermissions { storagePermissions.launchMultiplePermissionRequest() }
        } else {
            NeedsPermissions {
                context.startActivity(
                    Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsPermissions(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(5.dp)
        ) {
            Column(modifier = Modifier) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.please_enable_permissions),
                    style = M3MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                androidx.compose.material3.Text(
                    text = stringResource(R.string.need_permissions_to_work),
                    style = M3MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 4.dp)
                )

                androidx.compose.material3.Button(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 5.dp)
                ) { androidx.compose.material3.Text(text = stringResource(R.string.enable)) }
            }
        }
    }
}

@Composable
fun <T : Any> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<T> = rememberSaveable(
    saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )
) { elements.toList().toMutableStateList() }

@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    buffer: Int = 2,
    onLoadMore: () -> Unit
) {
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .drop(1)
            .distinctUntilChanged()
            .collect { onLoadMore() }
    }
}

@ExperimentalFoundationApi
@Composable
fun InfiniteListHandler(
    listState: LazyGridState,
    buffer: Int = 2,
    onLoadMore: () -> Unit
) {
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .drop(1)
            .distinctUntilChanged()
            .collect { onLoadMore() }
    }
}

class ListBottomSheetItemModel(
    val primaryText: String,
    val overlineText: String? = null,
    val secondaryText: String? = null,
    val icon: ImageVector? = null
)

class ListBottomSheet<T>(
    private val title: String,
    private val list: List<T>,
    private val onClick: (T) -> Unit,
    private val itemContent: (T) -> ListBottomSheetItemModel
) : BottomSheetDialogFragment() {
    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                MdcTheme {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        stickyHeader {
                            TopAppBar(
                                title = { Text(title) },
                                navigationIcon = { IconButton(onClick = { dismiss() }) { Icon(Icons.Default.Close, null) } },
                                actions = { if (list.isNotEmpty()) Text("(${list.size})") }
                            )
                        }

                        itemsIndexed(list) { index, it ->
                            val c = itemContent(it)
                            ListItem(
                                modifier = Modifier.clickable {
                                    dismiss()
                                    onClick(it)
                                },
                                icon = c.icon?.let { i -> { Icon(i, null) } },
                                text = { Text(c.primaryText) },
                                secondaryText = c.secondaryText?.let { i -> { Text(i) } },
                                overlineText = c.overlineText?.let { i -> { Text(i) } }
                            )
                            if (index < list.size - 1) androidx.compose.material.Divider()
                        }
                    }
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState)
        .apply {
            setOnShowListener {
                val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                val bottomSheet = BottomSheetBehavior.from(sheet)
                bottomSheet.skipCollapsed = true
                bottomSheet.isHideable = false
            }
        }
}

class GroupButtonModel<T>(val item: T, val iconContent: @Composable () -> Unit)

@Composable
fun <T> GroupButton(
    modifier: Modifier = Modifier,
    selected: T,
    options: List<GroupButtonModel<T>>,
    selectedColor: Color = M3MaterialTheme.colorScheme.inversePrimary,
    unselectedColor: Color = M3MaterialTheme.colorScheme.surface,
    onClick: (T) -> Unit
) {
    Row(modifier) {
        val smallShape = RoundedCornerShape(20.0.dp)
        val noCorner = CornerSize(0.dp)

        options.fastForEachIndexed { i, option ->
            OutlinedButton(
                modifier = Modifier,
                onClick = { onClick(option.item) },
                shape = smallShape.copy(
                    topStart = if (i == 0) smallShape.topStart else noCorner,
                    topEnd = if (i == options.size - 1) smallShape.topEnd else noCorner,
                    bottomStart = if (i == 0) smallShape.bottomStart else noCorner,
                    bottomEnd = if (i == options.size - 1) smallShape.bottomEnd else noCorner
                ),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = animateColorAsState(if (selected == option.item) selectedColor else unselectedColor).value
                )
            ) { option.iconContent() }
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun MaterialCard(
    modifier: Modifier = Modifier,
    headerOnTop: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    media: (@Composable ColumnScope.() -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        border = border,
        elevation = elevation
    ) {
        Column {
            if (headerOnTop) header?.invoke(this)
            media?.invoke(this)
            if (!headerOnTop) header?.invoke(this)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                supportingText?.let {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        ProvideTextStyle(MaterialTheme.typography.body2) {
                            it.invoke()
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { actions?.invoke(this) }
        }
    }
}

class CoordinatorModel(
    val height: Dp,
    val show: Boolean = true,
    val content: @Composable BoxScope.(Float, CoordinatorModel) -> Unit
) {
    var heightPx by Delegates.notNull<Float>()
    val offsetHeightPx = mutableStateOf(0f)

    @Composable
    internal fun Setup() {
        heightPx = with(LocalDensity.current) { height.roundToPx().toFloat() }
    }

    @Composable
    fun Content(scope: BoxScope) = scope.content(offsetHeightPx.value, this)
}

fun Modifier.coordinatorOffset(x: Int = 0, y: Int = 0) = offset { IntOffset(x = x, y = y) }

@Composable
fun Coordinator(
    topBar: CoordinatorModel? = null,
    bottomBar: CoordinatorModel? = null,
    vararg otherCoords: CoordinatorModel,
    content: @Composable BoxScope.() -> Unit
) = Coordinator(topBar, bottomBar, otherCoords.toList(), content)

@Composable
fun Coordinator(
    topBar: CoordinatorModel? = null,
    bottomBar: CoordinatorModel? = null,
    otherCoords: List<CoordinatorModel>,
    content: @Composable BoxScope.() -> Unit
) {
    topBar?.Setup()
    bottomBar?.Setup()
    otherCoords.fastForEach { it.Setup() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                topBar?.let {
                    val topBarOffset = it.offsetHeightPx.value + delta
                    it.offsetHeightPx.value = topBarOffset.coerceIn(-it.heightPx, 0f)
                }

                bottomBar?.let {
                    val bottomBarOffset = it.offsetHeightPx.value + delta
                    it.offsetHeightPx.value = bottomBarOffset.coerceIn(-it.heightPx, 0f)
                }

                otherCoords.fastForEach { c ->
                    c.let {
                        val offset = it.offsetHeightPx.value + delta
                        it.offsetHeightPx.value = offset.coerceIn(-it.heightPx, 0f)
                    }
                }

                return Offset.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()
        otherCoords.filter(CoordinatorModel::show).fastForEach { it.Content(this) }
        topBar?.let { if (it.show) it.Content(this) }
        bottomBar?.let { if (it.show) it.Content(this) }
    }
}

@Composable
fun BannerBox(
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    bannerEnter: EnterTransition = slideInVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ) { -it },
    bannerExit: ExitTransition = slideOutVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ) { -it },
    banner: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        content()
        AnimatedVisibility(
            visible = showBanner,
            enter = bannerEnter,
            exit = bannerExit,
        ) { banner() }
    }
}

@Composable
fun BannerBox2(
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    bannerSize: Dp,
    banner: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        content()
        val topBarHeightPx = with(LocalDensity.current) { bannerSize.roundToPx().toFloat() }
        val aniOffset = remember { Animatable(-topBarHeightPx * 2f) }
        LaunchedEffect(key1 = showBanner) { aniOffset.animateTo(if (showBanner) 0f else (-topBarHeightPx * 2f)) }
        Box(modifier = Modifier.offset { IntOffset(x = 0, y = aniOffset.value.roundToInt()) }) { banner() }
    }
}

val currentColorScheme: ColorScheme
    @Composable
    get() {
        val darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )
            else -> lightColorScheme(
                primary = Color(0xff2196F3),
                secondary = Color(0xff90CAF9)
            )
        }
    }

/**
 * Opinionated set of viewport breakpoints
 *     - Compact: Most phones in portrait mode
 *     - Medium: Most foldables and tablets in portrait mode
 *     - Expanded: Most tablets in landscape mode
 *
 * More info: https://material.io/archive/guidelines/layout/responsive-ui.html
 */
enum class WindowSize { Compact, Medium, Expanded }

/**
 * Remembers the [WindowSize] class for the window corresponding to the current window metrics.
 */
@Composable
fun Activity.rememberWindowSizeClass(): WindowSize {
    // Get the size (in pixels) of the window
    val windowSize = rememberWindowSize()

    // Convert the window size to [Dp]
    val windowDpSize = with(LocalDensity.current) {
        windowSize.toDpSize()
    }

    // Calculate the window size class
    return getWindowSizeClass(windowDpSize)
}

/**
 * Remembers the [Size] in pixels of the window corresponding to the current window metrics.
 */
@Composable
private fun Activity.rememberWindowSize(): Size {
    val configuration = LocalConfiguration.current
    // WindowMetricsCalculator implicitly depends on the configuration through the activity,
    // so re-calculate it upon changes.
    val windowMetrics = remember(configuration) { WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this) }
    return windowMetrics.bounds.toComposeRect().size
}

/**
 * Partitions a [DpSize] into a enumerated [WindowSize] class.
 */
fun getWindowSizeClass(windowDpSize: DpSize): WindowSize = when {
    windowDpSize.width < 0.dp -> throw IllegalArgumentException("Dp value cannot be negative")
    windowDpSize.width < 600.dp -> WindowSize.Compact
    windowDpSize.width < 840.dp -> WindowSize.Medium
    else -> WindowSize.Expanded
}

fun Color.contrastAgainst(background: Color): Float {
    val fg = if (alpha < 1f) compositeOver(background) else this

    val fgLuminance = fg.luminance() + 0.05f
    val bgLuminance = background.luminance() + 0.05f

    return kotlin.math.max(fgLuminance, bgLuminance) / kotlin.math.min(fgLuminance, bgLuminance)
}

@Composable
fun rememberDominantColorState(
    context: Context = LocalContext.current,
    defaultColor: Color = M3MaterialTheme.colorScheme.primary,
    defaultOnColor: Color = M3MaterialTheme.colorScheme.onPrimary,
    cacheSize: Int = 12,
    isColorValid: (Color) -> Boolean = { true }
): DominantColorState = remember {
    DominantColorState(context, defaultColor, defaultOnColor, cacheSize, isColorValid)
}

@Composable
fun rememberDynamicColorState(
    context: Context = LocalContext.current,
    defaultColorScheme: ColorScheme = M3MaterialTheme.colorScheme,
    cacheSize: Int = 12
): DynamicColorState = remember {
    DynamicColorState(context, defaultColorScheme, cacheSize)
}

//TODO: Try converting DetailsFragment to use this

/*
val surfaceColor = M3MaterialTheme.colorScheme.surface
val dominantColor = rememberDominantColorState { color ->
    // We want a color which has sufficient contrast against the surface color
    color.contrastAgainst(surfaceColor) >= 3f
}

DynamicThemePrimaryColorsFromImage(dominantColor) {
    LaunchedEffect(item.imageUrl) {
        if (item.imageUrl != null) {
            dominantColor.updateColorsFromImageUrl(item.imageUrl)
        } else {
            dominantColor.reset()
        }
    }
    HistoryItem(item, scope)
}
 */

/**
 * A composable which allows dynamic theming of the [androidx.compose.material.Colors.primary]
 * color from an image.
 */
@Composable
fun DynamicThemePrimaryColorsFromImage(
    dominantColorState: DominantColorState = rememberDominantColorState(),
    content: @Composable () -> Unit
) {
    val colors = M3MaterialTheme.colorScheme.copy(
        primary = animateColorAsState(
            dominantColorState.color,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onPrimary = animateColorAsState(
            dominantColorState.onColor,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        surface = animateColorAsState(
            dominantColorState.color,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onSurface = animateColorAsState(
            dominantColorState.onColor,
            spring(stiffness = Spring.StiffnessLow)
        ).value
    )
    M3MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun FullDynamicThemePrimaryColorsFromImage(
    dominantColorState: DynamicColorState = rememberDynamicColorState(),
    content: @Composable () -> Unit
) {
    val colors = M3MaterialTheme.colorScheme.copy(
        primary = animateColorAsState(
            dominantColorState.colorScheme.primary,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onPrimary = animateColorAsState(
            dominantColorState.colorScheme.onPrimary,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        surface = animateColorAsState(
            dominantColorState.colorScheme.surface,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onSurface = animateColorAsState(
            dominantColorState.colorScheme.onSurface,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        background = animateColorAsState(
            dominantColorState.colorScheme.background,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onBackground = animateColorAsState(
            dominantColorState.colorScheme.onBackground,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
    )
    M3MaterialTheme(colorScheme = colors, content = content)
}

/**
 * A class which stores and caches the result of any calculated dominant colors
 * from images.
 *
 * @param context Android context
 * @param defaultColor The default color, which will be used if [calculateDominantColor] fails to
 * calculate a dominant color
 * @param defaultOnColor The default foreground 'on color' for [defaultColor].
 * @param cacheSize The size of the [LruCache] used to store recent results. Pass `0` to
 * disable the cache.
 * @param isColorValid A lambda which allows filtering of the calculated image colors.
 */
@Stable
class DominantColorState(
    private val context: Context,
    private val defaultColor: Color,
    private val defaultOnColor: Color,
    cacheSize: Int = 12,
    private val isColorValid: (Color) -> Boolean = { true }
) {
    var color by mutableStateOf(defaultColor)
        private set
    var onColor by mutableStateOf(defaultOnColor)
        private set

    private val cache = when {
        cacheSize > 0 -> LruCache<String, DominantColors>(cacheSize)
        else -> null
    }

    suspend fun updateColorsFromImageUrl(url: String) {
        val result = calculateDominantColor(url)
        color = result?.color ?: defaultColor
        onColor = result?.onColor ?: defaultOnColor
    }

    private suspend fun calculateDominantColor(url: String): DominantColors? {
        val cached = cache?.get(url)
        if (cached != null) {
            // If we already have the result cached, return early now...
            return cached
        }

        // Otherwise we calculate the swatches in the image, and return the first valid color
        return calculateSwatchesInImage(context, url)
            // First we want to sort the list by the color's population
            .sortedByDescending { swatch -> swatch.population }
            // Then we want to find the first valid color
            .firstOrNull { swatch -> isColorValid(Color(swatch.rgb)) }
            // If we found a valid swatch, wrap it in a [DominantColors]
            ?.let { swatch ->
                DominantColors(
                    color = Color(swatch.rgb),
                    onColor = Color(swatch.bodyTextColor).copy(alpha = 1f)
                )
            }
            // Cache the resulting [DominantColors]
            ?.also { result -> cache?.put(url, result) }
    }

    /**
     * Reset the color values to [defaultColor].
     */
    fun reset() {
        color = defaultColor
        onColor = defaultColor
    }
}

@Stable
class DynamicColorState(
    private val context: Context,
    private val defaultColorScheme: ColorScheme,
    cacheSize: Int = 12
) {
    var colorScheme by mutableStateOf(defaultColorScheme)
        private set

    private val cache = when {
        cacheSize > 0 -> LruCache<String, Palette>(cacheSize)
        else -> null
    }

    suspend fun updateColorsFromImageUrl(url: String) {
        val result = calculateDominantColor(url)
        colorScheme = result?.let {
            defaultColorScheme.copy(
                primary = it.dominantSwatch?.rgb?.let { it1 -> Color(it1) } ?: defaultColorScheme.primary,
                onPrimary = it.dominantSwatch?.let { it1 -> Color(it1.bodyTextColor) } ?: defaultColorScheme.onPrimary,
                surface = it.vibrantSwatch?.let { it1 -> Color(it1.rgb) } ?: defaultColorScheme.surface,
                onSurface = it.vibrantSwatch?.let { it1 -> Color(it1.bodyTextColor) } ?: defaultColorScheme.onSurface,
                background = it.mutedSwatch?.let { it1 -> Color(it1.rgb) } ?: defaultColorScheme.background,
                onBackground = it.mutedSwatch?.let { it1 -> Color(it1.bodyTextColor) } ?: defaultColorScheme.onBackground,
            )
        } ?: defaultColorScheme
    }

    private suspend fun calculateDominantColor(url: String): Palette? {
        val cached = cache?.get(url)
        if (cached != null) {
            // If we already have the result cached, return early now...
            return cached
        }

        // Otherwise we calculate the swatches in the image, and return the first valid color
        return calculateAllSwatchesInImage(context, url)
            // First we want to sort the list by the color's population
            //.sortedByDescending { swatch -> swatch.population }
            // Then we want to find the first valid color
            //.firstOrNull { swatch -> isColorValid(Color(swatch.rgb)) }
            // If we found a valid swatch, wrap it in a [DominantColors]
            /*?.let { swatch ->
                DominantColors(
                    color = Color(swatch.rgb),
                    onColor = Color(swatch.bodyTextColor).copy(alpha = 1f)
                )
            }*/
            // Cache the resulting [DominantColors]
            ?.also { result -> cache?.put(url, result) }
    }

    /**
     * Reset the color values to [defaultColor].
     */
    fun reset() {
        colorScheme = defaultColorScheme
    }
}

@Immutable
private data class DominantColors(val color: Color, val onColor: Color)

/**
 * Fetches the given [imageUrl] with [Coil], then uses [Palette] to calculate the dominant color.
 */
private suspend fun calculateSwatchesInImage(
    context: Context,
    imageUrl: String
): List<Palette.Swatch> {
    val r = ImageRequest.Builder(context)
        .data(imageUrl)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(128).scale(Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()

    val bitmap = when (val result = r.context.imageLoader.execute(r)) {
        is SuccessResult -> result.drawable.toBitmap()
        else -> null
    }

    return bitmap?.let {
        withContext(Dispatchers.Default) {
            val palette = Palette.Builder(bitmap)
                // Disable any bitmap resizing in Palette. We've already loaded an appropriately
                // sized bitmap through Coil
                .resizeBitmapArea(0)
                // Clear any built-in filters. We want the unfiltered dominant color
                .clearFilters()
                // We reduce the maximum color count down to 8
                .maximumColorCount(8)
                .generate()
            //TODO: Maybe change this to return the palette and choose the kind of swatch afterwards
            // dominant
            // vibrant
            // muted
            palette.vibrantSwatch?.let { listOfNotNull(it) } ?: palette.swatches
        }
    } ?: emptyList()
}

private suspend fun calculateAllSwatchesInImage(
    context: Context,
    imageUrl: String
): Palette? {
    val r = ImageRequest.Builder(context)
        .data(imageUrl)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(128).scale(Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()

    val bitmap = when (val result = r.context.imageLoader.execute(r)) {
        is SuccessResult -> result.drawable.toBitmap()
        else -> null
    }

    return bitmap?.let {
        withContext(Dispatchers.Default) {
            val palette = Palette.Builder(bitmap)
                // Disable any bitmap resizing in Palette. We've already loaded an appropriately
                // sized bitmap through Coil
                .resizeBitmapArea(0)
                // Clear any built-in filters. We want the unfiltered dominant color
                .clearFilters()
                // We reduce the maximum color count down to 8
                .maximumColorCount(8)
                .generate()
            // dominant
            // vibrant
            // muted
            palette
        }
    }
}

@Composable
fun adaptiveGridCell(): GridCells = CustomAdaptive(ComposableUtils.IMAGE_WIDTH)

class CustomAdaptive(private val minSize: Dp) : GridCells {
    init {
        require(minSize > 0.dp)
    }

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int
    ): List<Int> {
        val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1) + 1
        return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
    }

    override fun hashCode(): Int {
        return minSize.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is CustomAdaptive && minSize == other.minSize
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) {
        slotSize + if (it < remainingPixels) 1 else 0
    }
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiver(defaultValue: T, intentFilter: IntentFilter, tick: (context: Context, intent: Intent) -> T): State<T> {
    val item: MutableState<T> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents.
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiverNullable(defaultValue: T?, intentFilter: IntentFilter, tick: (context: Context, intent: Intent) -> T?): State<T?> {
    val item: MutableState<T?> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}


class AirBarController(
    progress: Double = 0.0,
    progressCoordinates: Float = 0f,
    internal val isHorizontal: Boolean = false,
    animateProgress: Boolean = true
) {

    var animateProgress by mutableStateOf(animateProgress)
    internal var internalProgress by mutableStateOf(progress)
    internal var progressCoordinates by mutableStateOf(progressCoordinates)

    var progress: Double
        get() = internalProgress
        set(value) {
            progressCoordinates = reverseCalculateValues(value).toFloat()
            internalProgress = value
        }

    internal var bottomY = 0f
    internal var rightX = 0f

    private fun reverseCalculateValues(realPercentage: Double): Double {
        val p = if (isHorizontal)
            realPercentage * rightX / 100
        else
            bottomY - realPercentage * bottomY / 100

        return String.format("%.2f", p).toDouble()
    }

}

@Composable
fun rememberAirBarController(
    progress: Double = 0.0,
    progressCoordinates: Float = 0f,
    isHorizontal: Boolean = false,
    animateProgress: Boolean = true
) = remember { AirBarController(progress, progressCoordinates, isHorizontal, animateProgress) }

@ExperimentalComposeUiApi
@Composable
fun AirBar(
    modifier: Modifier = Modifier,
    controller: AirBarController = rememberAirBarController(),
    fillColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    fillColorGradient: List<Color>? = null,
    backgroundColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.background,
    cornerRadius: CornerRadius = CornerRadius(x = 40.dp.value, y = 40.dp.value),
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    icon: (@Composable () -> Unit)? = null,
    valueChanged: (Double) -> Unit
) {

    fun calculateValues(touchY: Float, touchX: Float): Double {
        val rawPercentage = if (controller.isHorizontal) {
            String.format("%.2f", (touchX.toDouble() / controller.rightX.toDouble()) * 100).toDouble()
        } else {
            String.format("%.2f", 100 - ((touchY.toDouble() / controller.bottomY.toDouble()) * 100)).toDouble()
        }

        val percentage = if (rawPercentage < 0) 0.0 else if (rawPercentage > 100) 100.0 else rawPercentage

        return String.format("%.2f", ((percentage / 100) * (maxValue - minValue) + minValue)).toDouble()
    }

    val vertical = if (controller.isHorizontal) 0f else controller.progressCoordinates
    val horizontal = if (controller.isHorizontal) controller.progressCoordinates else controller.rightX

    val bottomToTop = if (controller.animateProgress) animateFloatAsState(vertical).value else vertical
    val startToEnd = if (controller.animateProgress) animateFloatAsState(horizontal).value else horizontal

    Box(modifier = modifier, contentAlignment = if (controller.isHorizontal) Alignment.CenterStart else Alignment.BottomCenter) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (!controller.isHorizontal) {
                        when {
                            event.y in 0.0..controller.bottomY.toDouble() -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.y > 100 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.y < 0 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            else -> false
                        }
                    } else {
                        when {
                            event.x in 0.0..controller.rightX.toDouble() -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.x > 100 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.x < 0 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            else -> false
                        }
                    }
                }
        ) {
            controller.bottomY = size.height
            controller.rightX = size.width
            if (controller.internalProgress > 0.0) controller.progress = controller.internalProgress

            val path = Path()
            path.addRoundRect(
                roundRect = RoundRect(
                    0F,
                    0F,
                    size.width,
                    size.height,
                    cornerRadius
                )
            )
            drawContext.canvas.drawPath(path, Paint().apply {
                color = backgroundColor
                isAntiAlias = true
            })
            drawContext.canvas.clipPath(path = path, ClipOp.Intersect)
            drawContext.canvas.drawRect(
                0F,
                bottomToTop,
                startToEnd,
                size.height,
                Paint().apply {
                    color = fillColor
                    isAntiAlias = true
                    if (!fillColorGradient.isNullOrEmpty() && fillColorGradient.size > 1) {
                        shader = LinearGradientShader(
                            from = Offset(0f, 0f),
                            to = Offset(size.width, size.height),
                            colors = fillColorGradient
                        )
                    }
                })
        }

        icon?.let { Box(modifier = if (!controller.isHorizontal) Modifier.padding(bottom = 15.dp) else Modifier.padding(start = 15.dp)) { it() } }
    }
}

@ExperimentalComposeUiApi
@Composable
fun AirBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    fillColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    fillColorGradient: List<Color>? = null,
    backgroundColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.background,
    cornerRadius: CornerRadius = CornerRadius(x = 40.dp.value, y = 40.dp.value),
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    icon: (@Composable () -> Unit)? = null,
    valueChanged: (Float) -> Unit
) {

    var bottomY = 0f
    var rightX = 0f

    fun reverseCalculateValues(realPercentage: Float): Float {
        val p = if (isHorizontal)
            realPercentage * rightX / 100
        else
            bottomY - realPercentage * bottomY / 100

        return String.format("%.2f", p).toFloat()
    }

    fun calculateValues(touchY: Float, touchX: Float): Float {
        val rawPercentage = if (isHorizontal) {
            String.format("%.2f", (touchX.toDouble() / rightX.toDouble()) * 100).toDouble()
        } else {
            String.format("%.2f", 100 - ((touchY.toDouble() / bottomY.toDouble()) * 100)).toDouble()
        }

        val percentage = if (rawPercentage < 0) 0.0 else if (rawPercentage > 100) 100.0 else rawPercentage

        return String.format("%.2f", ((percentage / 100) * (maxValue - minValue) + minValue)).toFloat()
    }

    Box(modifier = modifier, contentAlignment = if (isHorizontal) Alignment.CenterStart else Alignment.BottomCenter) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (!isHorizontal) {
                        when {
                            event.y in 0.0..bottomY.toDouble() -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.y > 100 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.y < 0 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            else -> false
                        }
                    } else {
                        when {
                            event.x in 0.0..rightX.toDouble() -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.x > 100 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            event.x < 0 -> {
                                valueChanged(calculateValues(event.y, event.x))
                                true
                            }
                            else -> false
                        }
                    }
                }
        ) {
            bottomY = size.height
            rightX = size.width

            val path = Path()
            path.addRoundRect(
                roundRect = RoundRect(
                    0F,
                    0F,
                    size.width,
                    size.height,
                    cornerRadius
                )
            )
            drawContext.canvas.drawPath(path, Paint().apply {
                color = backgroundColor
                isAntiAlias = true
            })
            drawContext.canvas.clipPath(path = path, ClipOp.Intersect)
            drawContext.canvas.drawRect(
                0F,
                if (isHorizontal) 0f else reverseCalculateValues(progress),
                if (isHorizontal) reverseCalculateValues(progress) else rightX,
                size.height,
                Paint().apply {
                    color = fillColor
                    isAntiAlias = true
                    if (!fillColorGradient.isNullOrEmpty() && fillColorGradient.size > 1) {
                        shader = LinearGradientShader(
                            from = Offset(0f, 0f),
                            to = Offset(size.width, size.height),
                            colors = fillColorGradient
                        )
                    }
                })
        }

        icon?.let { Box(modifier = if (!isHorizontal) Modifier.padding(bottom = 15.dp) else Modifier.padding(start = 15.dp)) { it() } }
    }
}