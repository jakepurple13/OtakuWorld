package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.GradientImage
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.all_caught_up
import otakuworld.kmpuiviews.generated.resources.back_to_list
import otakuworld.kmpuiviews.generated.resources.catch_up_handled
import otakuworld.kmpuiviews.generated.resources.catch_up_left
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.catch_up_delete_body
import otakuworld.kmpuiviews.generated.resources.catch_up_delete_title
import otakuworld.kmpuiviews.generated.resources.catch_up_dont_ask_again
import otakuworld.kmpuiviews.generated.resources.catch_up_progress
import otakuworld.kmpuiviews.generated.resources.delete
import otakuworld.kmpuiviews.generated.resources.read
import otakuworld.kmpuiviews.generated.resources.remind
import otakuworld.kmpuiviews.generated.resources.skip
import otakuworld.kmpuiviews.generated.resources.swipe_left_to_delete
import otakuworld.kmpuiviews.generated.resources.swipe_left_to_skip

/**
 * One update at a time: swipe or tap to skip or delete (per [swipeDeletes]), read (open details), or remind later.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val flingDistance = LocalWindowInfo.current.containerSize.width * 1.5f
    val threshold = flingDistance / 6
    var pendingDelete by remember { mutableStateOf<NotificationItem?>(null) }

    // Swipe left and the left button both land here. Skip mode never deletes.
    val onLeft: (NotificationItem) -> Unit = { item ->
        scope.launch {
            when {
                !swipeDeletes -> {
                    offsetX.animateTo(-flingDistance)
                    onSkip(item)
                }

                confirmDeletes -> {
                    offsetX.animateTo(0f)
                    pendingDelete = item
                }

                else -> {
                    offsetX.animateTo(-flingDistance)
                    onDelete(item)
                }
            }
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
                            offsetX.animateTo(-flingDistance)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                LinearWavyProgressIndicator(
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
                        stringResource(if (swipeDeletes) Res.string.swipe_left_to_delete else Res.string.swipe_left_to_skip),
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
                    remaining
                        .take(3)
                        .withIndex()
                        .reversed()
                        .forEach { (depth, item) ->
                            val isTop = depth == 0
                            CatchUpCard(
                                item = item,
                                colorFilter = colorFilter,
                                modifier = Modifier
                                    .graphicsLayer {
                                        if (isTop) {
                                            translationX = offsetX.value
                                            rotationZ = offsetX.value / 60f
                                        } else {
                                            scaleX = 1f - depth * .06f
                                            scaleY = 1f - depth * .06f
                                            translationY = -depth * 14.dp.toPx()
                                            alpha = 1f - depth * .3f
                                        }
                                    }
                                    .then(
                                        if (isTop) {
                                            Modifier.pointerInput(item.url) {
                                                detectHorizontalDragGestures(
                                                    onDragEnd = {
                                                        scope.launch {
                                                            when {
                                                                //Left
                                                                offsetX.value > threshold -> {
                                                                    offsetX.animateTo(flingDistance)
                                                                    onRead(item)
                                                                }

                                                                //Right
                                                                offsetX.value < -threshold -> onLeft(item)

                                                                else -> offsetX.animateTo(0f)
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                                                ) { change, dragAmount ->
                                                    change.consume()
                                                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
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

            if (top != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val leftLabel = stringResource(if (swipeDeletes) Res.string.delete else Res.string.skip)
                    DeckAction(label = leftLabel) {
                        FilledTonalIconButton(
                            onClick = { onLeft(top) },
                            colors = if (swipeDeletes) {
                                IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            } else {
                                IconButtonDefaults.filledTonalIconButtonColors()
                            },
                            modifier = Modifier.size(56.dp)
                        ) { Icon(if (swipeDeletes) Icons.Default.Delete else Icons.Default.SkipNext, leftLabel) }
                    }

                    DeckAction(label = stringResource(Res.string.read)) {
                        FilledIconButton(
                            onClick = {
                                scope.launch {
                                    offsetX.animateTo(flingDistance)
                                    onRead(top)
                                }
                            },
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
                        NotifyAt(
                            items = listOf(top),
                            notificationScreenInterface = notificationScreenInterface,
                            onScheduled = onReminded,
                        ) { showDatePicker ->
                            FilledTonalIconButton(
                                onClick = showDatePicker,
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
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
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
