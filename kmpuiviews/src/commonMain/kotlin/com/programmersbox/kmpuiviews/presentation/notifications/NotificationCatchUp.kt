package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.GradientImage
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.SourceNotInstalledModal
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.all_caught_up
import otakuworld.kmpuiviews.generated.resources.back_to_list
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.catch_up_delete_body
import otakuworld.kmpuiviews.generated.resources.catch_up_delete_title
import otakuworld.kmpuiviews.generated.resources.catch_up_dont_ask_again
import otakuworld.kmpuiviews.generated.resources.catch_up_handled
import otakuworld.kmpuiviews.generated.resources.catch_up_left
import otakuworld.kmpuiviews.generated.resources.catch_up_progress
import otakuworld.kmpuiviews.generated.resources.catch_up_swipe_hint_delete
import otakuworld.kmpuiviews.generated.resources.catch_up_swipe_hint_skip
import otakuworld.kmpuiviews.generated.resources.delete
import otakuworld.kmpuiviews.generated.resources.deleted_notification
import otakuworld.kmpuiviews.generated.resources.deleted_notification_count
import otakuworld.kmpuiviews.generated.resources.read
import otakuworld.kmpuiviews.generated.resources.remind
import otakuworld.kmpuiviews.generated.resources.skip
import otakuworld.kmpuiviews.generated.resources.undo
import kotlin.math.abs

@Composable
fun CatchUpScreen(
    vm: CatchUpViewModel = koinViewModel(),
    navController: NavigationActions = LocalNavActions.current,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
) {
    var catchUpSwipeDeletes by koinInject<DataStoreHandling>()
        .catchUpSwipeDeletes
        .asState()

    val state = vm.catchUp

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }
    val notificationScreenInterface: NotificationScreenInterface = koinInject()

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val biometricOpen = rememberBiometricOpening()

    var showNotificationItem by remember { mutableStateOf<NotificationItem?>(null) }

    SourceNotInstalledModal(
        showItem = showNotificationItem?.notiTitle,
        onShowItemDismiss = { showNotificationItem = null },
        source = showNotificationItem?.source,
        url = showNotificationItem?.url
    )

    val toSource: (String) -> KmpApiService? = { s -> sourceRepository.toSourceByApiServiceName(s)?.apiService }

    val onError: (NotificationItem) -> Unit = {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                "Something went wrong. Source might not be installed",
                duration = SnackbarDuration.Long,
                actionLabel = "More Options",
                withDismissAction = true
            )
            showNotificationItem = when (result) {
                SnackbarResult.Dismissed -> null
                SnackbarResult.ActionPerformed -> it
            }
        }
    }

    val openItem: (NotificationItem) -> Unit = { item ->
        scope.launch {
            biometricOpen.openIfNotIncognito(item.url, item.notiTitle) {
                toSource(item.source)
                    ?.let { source ->
                        flow {
                            Cached.cache[item.url]?.let {
                                emit(
                                    it
                                        .toDbModel()
                                        .toItemModel(source)
                                )
                            } ?: emitAll(source.getSourceByUrlFlow(item.url))
                        }
                    }
                    ?.dispatchIo()
                    ?.onStart { showLoadingDialog = true }
                    ?.onEach {
                        showLoadingDialog = false
                        navController.details(it)
                    }
                    ?.launchIn(scope) ?: onError(item)
            }
        }
    }

    val deleteWithUndo: (List<NotificationItem>) -> Unit = { toDelete ->
        if (toDelete.isNotEmpty()) {
            val batch = vm.deleteWithUndo(toDelete)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                var undone = false
                try {
                    val message = toDelete.singleOrNull()
                        ?.let { getString(Res.string.deleted_notification, it.notiTitle) }
                        ?: getString(Res.string.deleted_notification_count, toDelete.size)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = getString(Res.string.undo),
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    )
                    undone = result == SnackbarResult.ActionPerformed
                    if (undone) vm.undoDeletion(batch)
                } finally {
                    // Also runs when the screen leaves composition mid-snackbar.
                    if (!undone) vm.commitDeletion(batch)
                }
            }
        }
    }

    CatchUpDeck(
        remaining = vm.catchUpRemaining,
        index = state?.index ?: 0,
        total = state?.urls?.size ?: 0,
        title = vm.filter.catchUpTitle(),
        colorFilter = colorFilter,
        snackbarHostState = snackbarHostState,
        notificationScreenInterface = notificationScreenInterface,
        onClose = { navController.popBackStack() },
        swipeDeletes = catchUpSwipeDeletes,
        onSwipeDeletesChange = { catchUpSwipeDeletes = it },
        confirmDeletes = state?.confirmDeletes ?: true,
        onStopConfirmingDeletes = vm::stopConfirmingCatchUpDeletes,
        onSkip = { vm.advanceCatchUp() },
        onDelete = { item ->
            deleteWithUndo(listOf(item))
            vm.advanceCatchUp()
        },
        onRead = { item ->
            vm.advanceCatchUp()
            openItem(item)
        },
        onReminded = { vm.advanceCatchUp() },
    )
}

/**
 * One update at a time. Swipe left to skip or delete (per [swipeDeletes]), up or tap to read, right to remind.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CatchUpDeck(
    remaining: List<NotificationItem>,
    index: Int,
    total: Int,
    title: String,
    colorFilter: ColorFilter?,
    snackbarHostState: SnackbarHostState,
    notificationScreenInterface: NotificationScreenInterface,
    swipeDeletes: Boolean,
    onSwipeDeletesChange: (Boolean) -> Unit,
    confirmDeletes: Boolean,
    onStopConfirmingDeletes: () -> Unit,
    onClose: () -> Unit,
    onSkip: (NotificationItem) -> Unit,
    onDelete: (NotificationItem) -> Unit,
    onRead: (NotificationItem) -> Unit,
    onReminded: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val top = remaining.firstOrNull()
    val offsetX = remember(top?.url) { Animatable(0f) }
    val offsetY = remember(top?.url) { Animatable(0f) }
    val containerSize = LocalWindowInfo.current.containerSize
    val flingDistance = containerSize.width * 1.5f
    val flingDistanceY = containerSize.height * 1f
    val threshold = flingDistance / 6
    val flingSpec = remember { tween<Float>(durationMillis = 200, easing = FastOutLinearInEasing) }
    var pendingDelete by remember { mutableStateOf<NotificationItem?>(null) }

    val snapBack: () -> Unit = {
        scope.launch { offsetX.animateTo(0f) }
        scope.launch { offsetY.animateTo(0f) }
    }

    // Swipe left and the left button both land here. Skip mode never deletes.
    val onLeft: (NotificationItem) -> Unit = { item ->
        scope.launch {
            when {
                !swipeDeletes -> {
                    offsetX.animateTo(-flingDistance, flingSpec)
                    onSkip(item)
                }

                confirmDeletes -> {
                    snapBack()
                    pendingDelete = item
                }

                else -> {
                    offsetX.animateTo(-flingDistance, flingSpec)
                    onDelete(item)
                }
            }
        }
    }

    // Swipe up, a tap on the card, and the Read button all land here.
    val onReadTop: (NotificationItem) -> Unit = { item ->
        scope.launch {
            offsetY.animateTo(-flingDistanceY, flingSpec)
            onRead(item)
        }
    }

    pendingDelete?.let { item ->
        var dontAskAgain by remember(item.url) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text(stringResource(Res.string.catch_up_delete_title, item.notiTitle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.catch_up_delete_body))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = dontAskAgain,
                                onValueChange = { dontAskAgain = it },
                                role = Role.Checkbox
                            )
                    ) {
                        Checkbox(checked = dontAskAgain, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.catch_up_dont_ask_again))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        if (dontAskAgain) onStopConfirmingDeletes()
                        scope.launch {
                            offsetX.animateTo(-flingDistance, flingSpec)
                            onDelete(item)
                        }
                    }
                ) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    HideNavBarWhileOnScreen()

    NotifyAt(
        items = listOfNotNull(top),
        notificationScreenInterface = notificationScreenInterface,
        onScheduled = {
            scope.launch {
                offsetX.animateTo(flingDistance, flingSpec)
                onReminded()
            }
        },
    ) { showRemindPicker ->
        // Swipe right and the Remind button both land here. The card waits until a time is picked.
        val onRemind: () -> Unit = {
            snapBack()
            showRemindPicker()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, stringResource(Res.string.back_to_list))
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.padding(LocalNavHostPadding.current)
                )
            },
        ) { p ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(p)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(Res.string.catch_up_progress, (index + 1).coerceAtMost(total), total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(Res.string.catch_up_left, remaining.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (total == 0) 1f else index.toFloat() / total },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (top != null) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            SegmentedButton(
                                selected = !swipeDeletes,
                                onClick = { onSwipeDeletesChange(false) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = !swipeDeletes) {
                                        Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                                    }
                                }
                            ) { Text(stringResource(Res.string.skip)) }
                            SegmentedButton(
                                selected = swipeDeletes,
                                onClick = { onSwipeDeletesChange(true) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = swipeDeletes) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                                    }
                                }
                            ) { Text(stringResource(Res.string.delete)) }
                        }
                        Text(
                            stringResource(if (swipeDeletes) Res.string.catch_up_swipe_hint_delete else Res.string.catch_up_swipe_hint_skip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    if (top == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(Res.string.all_caught_up), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                stringResource(Res.string.catch_up_handled, total),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onClose) { Text(stringResource(Res.string.back_to_list)) }
                        }
                    } else {
                        // Only recomposes when the direction under the finger changes, not every frame.
                        val dragDirection by remember(top.url) {
                            derivedStateOf { swipeDirection(offsetX.value, offsetY.value, threshold) }
                        }
                        // A 4th, fully transparent card waits at the back so it can fade in as the stack moves up.
                        remaining
                            .take(4)
                            .withIndex()
                            .reversed()
                            .forEach { (depth, item) ->
                                // Keyed so each card keeps its animation state as it moves up the stack.
                                key(item.url) {
                                    val isTop = depth == 0
                                    val animatedDepth by animateFloatAsState(
                                        targetValue = depth.toFloat(),
                                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                        label = "cardDepth"
                                    )
                                    val cardAlpha by animateFloatAsState(
                                        targetValue = when (depth) {
                                            0 -> 1f
                                            1 -> .7f
                                            2 -> .4f
                                            else -> 0f
                                        },
                                        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                        label = "cardAlpha"
                                    )

                                    CatchUpCard(
                                        item = item,
                                        onClick = { onReadTop(item) },
                                        colorFilter = colorFilter,
                                        actionLabel = if (isTop) {
                                            when (dragDirection) {
                                                DeckSwipe.Left -> stringResource(if (swipeDeletes) Res.string.delete else Res.string.skip)
                                                DeckSwipe.Right -> stringResource(Res.string.remind)
                                                DeckSwipe.Up -> stringResource(Res.string.read)
                                                null -> null
                                            }
                                        } else {
                                            null
                                        },
                                        actionLabelColor = when (dragDirection) {
                                            DeckSwipe.Left -> if (swipeDeletes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                            DeckSwipe.Right -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = 1f - animatedDepth * .06f
                                                scaleY = 1f - animatedDepth * .06f
                                                translationY = -animatedDepth * 14.dp.toPx()
                                                alpha = cardAlpha
                                                if (isTop) {
                                                    translationX = offsetX.value
                                                    translationY += offsetY.value
                                                    rotationZ = offsetX.value / 60f
                                                }
                                            }
                                            .then(
                                                if (isTop) {
                                                    Modifier.pointerInput(item.url) {
                                                        detectDragGestures(
                                                            onDragEnd = {
                                                                when (swipeDirection(offsetX.value, offsetY.value, threshold)) {
                                                                    DeckSwipe.Left -> onLeft(item)
                                                                    DeckSwipe.Right -> onRemind()
                                                                    DeckSwipe.Up -> onReadTop(item)
                                                                    null -> snapBack()
                                                                }
                                                            },
                                                            onDragCancel = snapBack,
                                                        ) { change, dragAmount ->
                                                            change.consume()
                                                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                                                            scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                                                        }
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    )
                                }
                            }
                    }
                }

                if (top != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Crossfade(swipeDeletes) { target ->
                            val leftLabel = stringResource(if (target) Res.string.delete else Res.string.skip)
                            DeckAction(label = leftLabel) {
                                FilledTonalIconButton(
                                    onClick = { onLeft(top) },
                                    colors = if (target) {
                                        IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    } else {
                                        IconButtonDefaults.filledTonalIconButtonColors()
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) { Icon(if (target) Icons.Default.Delete else Icons.Default.SkipNext, leftLabel) }
                            }
                        }

                        DeckAction(label = stringResource(Res.string.read)) {
                            FilledIconButton(
                                onClick = { onReadTop(top) },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    stringResource(Res.string.read),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        DeckAction(label = stringResource(Res.string.remind)) {
                            FilledTonalIconButton(
                                onClick = onRemind,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                                modifier = Modifier.size(56.dp)
                            ) { Icon(Icons.Default.Schedule, stringResource(Res.string.remind)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckAction(
    label: String,
    button: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(80.dp)
            .padding(vertical = 16.dp)
    ) {
        button()
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CatchUpCard(
    item: NotificationItem,
    onClick: () -> Unit,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionLabelColor: Color = Color.White,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onClick,
        modifier = modifier.aspectRatio(2f / 3f, matchHeightConstraintsFirst = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GradientImage(
                model = item.imageUrl.orEmpty(),
                placeholder = painterLogo(),
                error = painterLogo(),
                contentDescription = item.notiTitle,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )

            if (actionLabel != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = .65f),
                    contentColor = actionLabelColor,
                    border = BorderStroke(2.dp, actionLabelColor),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                ) {
                    Text(
                        actionLabel.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .85f))))
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)
            ) {
                Text(
                    item.notiTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.White.copy(alpha = .15f),
                    contentColor = Color.White,
                ) {
                    Text(
                        item.source,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private enum class DeckSwipe { Left, Right, Up }

/** The larger axis wins once it passes [threshold]. Dragging down does nothing. */
private fun swipeDirection(dx: Float, dy: Float, threshold: Float): DeckSwipe? = when {
    abs(dx) >= abs(dy) && dx < -threshold -> DeckSwipe.Left
    abs(dx) >= abs(dy) && dx > threshold -> DeckSwipe.Right
    abs(dy) > abs(dx) && dy < -threshold -> DeckSwipe.Up
    else -> null
}
