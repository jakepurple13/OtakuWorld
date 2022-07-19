package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

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

//taken from https://github.com/RoudyK/AnimatedLazyColumn due to https://issuetracker.google.com/issues/150812265
internal enum class AnimatedItemState {
    INITIAL, INSERTED, REMOVED, IDLE, ALL_REMOVED
}

internal data class AnimatedItemWA<T>(
    val value: AnimatedLazyListItem<T>,
    val state: AnimatedItemState
)

internal class AnimatedLazyListViewModel<T>(
    private val scope: CoroutineScope,
    private val animationDuration: Int,
    private val reverseLayout: Boolean
) {

    val items = MutableStateFlow<List<AnimatedItemWA<T>>>(emptyList())

    private var previousList = emptyList<AnimatedLazyListItem<T>>()

    data class ItemsUpdate<T>(
        val insertedPositions: List<Int>,
        val removedPositions: List<Int>,
        val movedPositions: List<Pair<Int, Int>>,
        val changedPositions: List<Int>,
        val previousList: List<AnimatedLazyListItem<T>>,
        val currentList: List<AnimatedLazyListItem<T>>
    )

    private var job: Job? = null
    private val mutex = Mutex()
    private val itemsUpdateFlow = MutableSharedFlow<ItemsUpdate<T>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    init {
        itemsUpdateFlow
            .onEach { (
                          insertedPositions,
                          removedPositions,
                          movedPositions,
                          changedPositions,
                          currentPreviousList,
                          currentList
                      ) ->
                if (insertedPositions.isEmpty() && removedPositions.isEmpty() && movedPositions.isEmpty() && changedPositions.isEmpty()) {
                    return@onEach
                }
                mutex.withLock {
                    if (items.value.isEmpty()) {
                        items.emit(currentList.fastMap { AnimatedItemWA(value = it, state = AnimatedItemState.INITIAL) })
                        delay(animationDuration.toLong())
                        items.emit(items.value.fastMap { it.copy(state = AnimatedItemState.IDLE) })
                        previousList = currentList
                        return@onEach
                    }
                    val intermediateList = mutableListOf<AnimatedItemWA<T>>()
                    val allRemoved = currentList.isEmpty() && removedPositions.isNotEmpty()
                    intermediateList.addAll(currentList.mapIndexed { index, item ->
                        AnimatedItemWA(
                            value = item,
                            state = when {
                                insertedPositions.contains(index) || movedPositions.find { it.second == index } != null -> AnimatedItemState.INSERTED
                                else -> AnimatedItemState.IDLE
                            }
                        )
                    })
                    removedPositions.fastForEach {
                        val index = max(
                            0,
                            if (it > intermediateList.size) intermediateList.size - 1 else it
                        )
                        intermediateList.add(
                            index,
                            AnimatedItemWA(
                                value = currentPreviousList[index],
                                state = if (allRemoved) {
                                    AnimatedItemState.ALL_REMOVED
                                } else {
                                    AnimatedItemState.REMOVED
                                }
                            )
                        )
                    }
                    if (!allRemoved) {
                        movedPositions.fastForEach {
                            val item = currentPreviousList[it.first]
                            intermediateList.add(
                                if (it.first > intermediateList.size) {
                                    intermediateList.size
                                } else {
                                    it.first + if (reverseLayout) 1 else -1
                                }.coerceIn(0, intermediateList.size),
                                AnimatedItemWA(
                                    value = item.copy(key = "${item.key}-temp"),
                                    state = AnimatedItemState.REMOVED
                                )
                            )
                        }
                    }
                    items.emit(intermediateList.distinctBy { it.value.key })
                    delay(animationDuration.toLong())
                    items.emit(currentList.fastMap {
                        AnimatedItemWA(
                            value = it,
                            state = AnimatedItemState.IDLE
                        )
                    })
                    previousList = currentList
                }
            }
            .launchIn(scope)
    }

    fun updateList(currentList: List<AnimatedLazyListItem<T>>) {
        job?.cancel()
        job = scope.launch {
            mutex.withLock {
                val insertedPositions = mutableListOf<Int>()
                val removedPositions = mutableListOf<Int>()
                val changedPositions = mutableListOf<Int>()
                val movedPositions = mutableListOf<Pair<Int, Int>>()
                val diffResult = DiffUtil.calculateDiff(ItemsCallback(previousList, currentList))
                diffResult.dispatchUpdatesTo(
                    ListCallback(
                        insertedPositions,
                        removedPositions,
                        movedPositions,
                        changedPositions
                    )
                )
                itemsUpdateFlow.emit(
                    ItemsUpdate(
                        insertedPositions = insertedPositions,
                        removedPositions = removedPositions,
                        movedPositions = movedPositions,
                        previousList = previousList,
                        changedPositions = changedPositions,
                        currentList = currentList
                    )
                )
            }
        }
    }

    inner class ItemsCallback(
        private var previousList: List<AnimatedLazyListItem<T>>,
        private var newList: List<AnimatedLazyListItem<T>>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = previousList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return previousList[oldItemPosition].key == newList[newItemPosition].key
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return previousList[oldItemPosition].value == newList[newItemPosition].value
        }
    }

    inner class ListCallback(
        private val insertedPositions: MutableList<Int>,
        private val removedPositions: MutableList<Int>,
        private val movedPositions: MutableList<Pair<Int, Int>>,
        private val changedPositions: MutableList<Int>,
    ) : ListUpdateCallback {

        override fun onInserted(position: Int, count: Int) {
            for (i in 0 until count) {
                insertedPositions.add(position + i)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            for (i in 0 until count) {
                removedPositions.add(position + i)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (fromPosition == 0 && toPosition == 1 && reverseLayout) {
                movedPositions.add(toPosition to fromPosition)
            } else {
                movedPositions.add(fromPosition to toPosition)
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            changedPositions.add(position)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun <T> animatedLazyListScope(
    currentItems: List<AnimatedItemWA<T>>,
    initialEnter: EnterTransition,
    enter: EnterTransition,
    exit: ExitTransition,
    finalExit: ExitTransition,
    isVertical: Boolean,
    spacing: Dp
): LazyListScope.() -> Unit = {
    itemsIndexed(currentItems, key = { _, k -> k.value.key }) { index, item ->
        val transitionState = remember("${item.value.key}-$index") {
            MutableTransitionState(
                when (item.state) {
                    AnimatedItemState.INITIAL -> false
                    AnimatedItemState.INSERTED -> false
                    AnimatedItemState.REMOVED -> true
                    AnimatedItemState.IDLE -> true
                    AnimatedItemState.ALL_REMOVED -> true
                }
            )
        }
        transitionState.targetState = when (item.state) {
            AnimatedItemState.INITIAL -> true
            AnimatedItemState.INSERTED -> true
            AnimatedItemState.REMOVED -> false
            AnimatedItemState.IDLE -> true
            AnimatedItemState.ALL_REMOVED -> false
        }
        AnimatedVisibility(
            visibleState = transitionState,
            enter = when (item.state) {
                AnimatedItemState.INITIAL -> initialEnter
                else -> enter
            },
            exit = when (item.state) {
                AnimatedItemState.ALL_REMOVED -> finalExit
                else -> exit
            }
        ) {
            Box(
                modifier = Modifier
                    .let {
                        if (isVertical) {
                            it.padding(bottom = spacing)
                        } else {
                            it.padding(end = spacing)
                        }
                    }
                    .animateItemPlacement()
            ) {
                item.value.composable()
            }
        }
    }
}

data class AnimatedLazyListItem<out T>(
    val key: String,
    val value: T? = null,
    val composable: @Composable () -> Unit
)

@Composable
fun <T> AnimatedLazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    items: List<AnimatedLazyListItem<T>>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = if (!reverseLayout) Arrangement.Start else Arrangement.End,
    animationDuration: Int = 400,
    initialEnter: EnterTransition = fadeIn(),
    enter: EnterTransition = fadeIn(
        animationSpec = tween(delayMillis = animationDuration / 3),
    ) + expandHorizontally(
        animationSpec = tween(durationMillis = animationDuration),
        expandFrom = if (reverseLayout) Alignment.End else Alignment.Start
    ),
    exit: ExitTransition = fadeOut() + shrinkHorizontally(
        animationSpec = tween(durationMillis = animationDuration),
        shrinkTowards = if (reverseLayout) Alignment.Start else Alignment.End
    ),
    finalExit: ExitTransition = exit
) {
    val scope = rememberCoroutineScope { Dispatchers.Main }
    val viewModel = remember { AnimatedLazyListViewModel<T>(scope, animationDuration, reverseLayout) }
    viewModel.updateList(items)
    val currentItems by viewModel.items.collectAsState(emptyList())

    LazyRow(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        content = animatedLazyListScope(
            currentItems = currentItems,
            initialEnter = initialEnter,
            enter = enter,
            exit = exit,
            finalExit = finalExit,
            isVertical = false,
            spacing = horizontalArrangement.spacing
        )
    )
}

@Composable
fun <T> AnimatedLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    items: List<AnimatedLazyListItem<T>>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    animationDuration: Int = 400,
    initialEnter: EnterTransition = fadeIn(),
    enter: EnterTransition = fadeIn(
        animationSpec = tween(delayMillis = animationDuration / 3),
    ) + expandVertically(
        animationSpec = tween(durationMillis = animationDuration),
        expandFrom = if (reverseLayout) Alignment.Top else Alignment.Bottom
    ),
    exit: ExitTransition = fadeOut() + shrinkVertically(
        animationSpec = tween(durationMillis = animationDuration),
        shrinkTowards = if (reverseLayout) Alignment.Bottom else Alignment.Top
    ),
    finalExit: ExitTransition = exit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember { AnimatedLazyListViewModel<T>(scope, animationDuration, reverseLayout) }
    viewModel.updateList(items)
    val currentItems by viewModel.items.collectAsState(emptyList())

    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        content = animatedLazyListScope(
            currentItems = currentItems,
            initialEnter = initialEnter,
            enter = enter,
            exit = exit,
            finalExit = finalExit,
            isVertical = true,
            spacing = verticalArrangement.spacing
        )
    )
}