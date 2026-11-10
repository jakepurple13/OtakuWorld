package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.qa_ago_days
import otakuworld.kmpuiviews.generated.resources.qa_ago_hours
import otakuworld.kmpuiviews.generated.resources.qa_ago_minutes
import otakuworld.kmpuiviews.generated.resources.qa_background_tasks
import otakuworld.kmpuiviews.generated.resources.qa_background_tasks_sub
import otakuworld.kmpuiviews.generated.resources.qa_check_updates
import otakuworld.kmpuiviews.generated.resources.qa_checking
import otakuworld.kmpuiviews.generated.resources.qa_checking_indeterminate
import otakuworld.kmpuiviews.generated.resources.qa_global_search
import otakuworld.kmpuiviews.generated.resources.qa_global_search_sub
import otakuworld.kmpuiviews.generated.resources.qa_group_go_to
import otakuworld.kmpuiviews.generated.resources.qa_group_tools
import otakuworld.kmpuiviews.generated.resources.qa_last_checked
import otakuworld.kmpuiviews.generated.resources.qa_lists
import otakuworld.kmpuiviews.generated.resources.qa_lists_count
import otakuworld.kmpuiviews.generated.resources.qa_lists_sub
import otakuworld.kmpuiviews.generated.resources.qa_never_checked
import otakuworld.kmpuiviews.generated.resources.qa_run
import otakuworld.kmpuiviews.generated.resources.qa_saved_count
import otakuworld.kmpuiviews.generated.resources.qa_saved_notifications
import otakuworld.kmpuiviews.generated.resources.qa_saved_notifications_sub
import otakuworld.kmpuiviews.generated.resources.qa_scan_qr
import otakuworld.kmpuiviews.generated.resources.qa_scan_qr_sub
import otakuworld.kmpuiviews.generated.resources.qa_scheduled
import otakuworld.kmpuiviews.generated.resources.qa_update_errors
import otakuworld.kmpuiviews.generated.resources.qa_update_errors_sub
import otakuworld.kmpuiviews.generated.resources.quick_actions
import kotlin.time.Clock

private val FirstShape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 6.dp)
private val MiddleShape = RoundedCornerShape(6.dp)
private val SingleShape = RoundedCornerShape(18.dp)
private val LastShape = RoundedCornerShape(6.dp, 6.dp, 18.dp, 18.dp)

@Composable
fun QuickActionsList(
    state: QuickActionsUiState,
    onAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
    /** Attached to the first row so keyboard-opened cards can focus it. */
    firstRowFocusRequester: FocusRequester? = null,
    actions: List<QuickAction> = QuickAction.entries,
    showGroupLabels: Boolean = true,
    /** Row drawn with a primary inside border, for keyboard/desktop selection. */
    selected: QuickAction? = null,
    /**
     * When set, only the rows scroll (the header stays put) and take at most the height left
     * after the header. Needs a bounded height, so leave it null inside an unbounded parent.
     */
    rowsScrollState: ScrollState? = null,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = QuickAction.entries.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val groups = QuickActionGroup.entries.map { group -> group to actions.filter { it.group == group } }
            .filter { it.second.isNotEmpty() }
        val rowsModifier = if (rowsScrollState != null) {
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rowsScrollState)
        } else {
            Modifier
        }
        Column(rowsModifier) {
            groups.forEachIndexed { groupIndex, (group, items) ->
                if (showGroupLabels) {
                    Text(
                        text = stringResource(
                            when (group) {
                                QuickActionGroup.Tools -> Res.string.qa_group_tools
                                QuickActionGroup.GoTo -> Res.string.qa_group_go_to
                            }
                        ).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (groupIndex == 0) 0.dp else 10.dp,
                            bottom = 4.dp,
                        )
                    )
                } else if (groupIndex > 0) {
                    Spacer(Modifier.height(10.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items.forEachIndexed { index, action ->
                        key(action) {
                            val spec = action.rowSpec(state)
                            val bringIntoView = remember { BringIntoViewRequester() }
                            // Arrow keys move the selection; keep the selected row inside the scrolled rows
                            if (action == selected) LaunchedEffect(selected) { bringIntoView.bringIntoView() }
                            val rowModifier = Modifier
                                .bringIntoViewRequester(bringIntoView)
                                .then(
                                    if (groupIndex == 0 && index == 0 && firstRowFocusRequester != null) {
                                        Modifier.focusRequester(firstRowFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                            QuickActionRow(
                                shape = when {
                                    items.size == 1 -> SingleShape
                                    index == 0 -> FirstShape
                                    index == items.lastIndex -> LastShape
                                    else -> MiddleShape
                                },
                                icon = spec.icon,
                                iconContainer = spec.container,
                                iconContent = spec.content,
                                title = spec.title,
                                supporting = spec.supporting,
                                onClick = { onAction(action) },
                                trailing = {
                                    if (spec.run) RunChip()
                                    else {
                                        val keycap = if (quickActionsDesktopExtras) quickActionShortcutLabel(action) else null
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (keycap != null) {
                                                KeyCap(keycap)
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Chevron()
                                        }
                                    }
                                },
                                selected = action == selected,
                                modifier = rowModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal data class RowSpec(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
    val title: String,
    val supporting: String,
    val run: Boolean,
)

@Composable
internal fun QuickAction.rowSpec(state: QuickActionsUiState): RowSpec {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        QuickAction.CheckForUpdates -> RowSpec(
            Icons.Default.Refresh, colors.primaryContainer, colors.onPrimaryContainer,
            stringResource(Res.string.qa_check_updates), checkSupportingText(state), run = true,
        )

        QuickAction.ScanQrCode -> RowSpec(
            Icons.Default.QrCodeScanner, colors.tertiaryContainer, colors.onTertiaryContainer,
            stringResource(Res.string.qa_scan_qr), stringResource(Res.string.qa_scan_qr_sub), run = false,
        )

        QuickAction.GlobalSearch -> RowSpec(
            Icons.Default.Search, colors.primaryFixed, colors.onPrimaryFixed,
            stringResource(Res.string.qa_global_search), stringResource(Res.string.qa_global_search_sub), run = false,
        )

        QuickAction.Lists -> RowSpec(
            Icons.AutoMirrored.Filled.List, colors.tertiaryFixed, colors.onTertiaryFixed,
            stringResource(Res.string.qa_lists),
            state.listCount?.let { stringResource(Res.string.qa_lists_count, it) }
                ?: stringResource(Res.string.qa_lists_sub),
            run = false,
        )

        QuickAction.SavedNotifications -> RowSpec(
            Icons.Default.Notifications, colors.secondaryFixed, colors.onSecondaryFixed,
            stringResource(Res.string.qa_saved_notifications),
            state.savedNotificationCount?.let { stringResource(Res.string.qa_saved_count, it) }
                ?: stringResource(Res.string.qa_saved_notifications_sub),
            run = false,
        )

        QuickAction.BackgroundTasks -> RowSpec(
            Icons.Default.Schedule, colors.secondaryContainer, colors.onSecondaryContainer,
            stringResource(Res.string.qa_background_tasks),
            state.scheduledCount?.let { stringResource(Res.string.qa_scheduled, it) }
                ?: stringResource(Res.string.qa_background_tasks_sub),
            run = false,
        )

        QuickAction.UpdateErrors -> RowSpec(
            Icons.Default.ErrorOutline, colors.errorContainer, colors.onErrorContainer,
            stringResource(Res.string.qa_update_errors), stringResource(Res.string.qa_update_errors_sub), run = false,
        )
    }
}

@Composable
private fun checkSupportingText(state: QuickActionsUiState): String {
    val check = state.checkState
    if (check is ManualCheckState.Running) {
        val checked = check.checked
        val total = check.total
        return if (checked != null && total != null) {
            stringResource(Res.string.qa_checking, checked, total)
        } else {
            stringResource(Res.string.qa_checking_indeterminate)
        }
    }
    // Ticks once a minute so "Last checked" keeps counting while the card stays open
    val now by produceState(Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            delay(60_000)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    return when (val last = lastCheckedLabel(state.lastCheckedAt, now)) {
        LastChecked.Never -> stringResource(Res.string.qa_never_checked)
        is LastChecked.Ago -> stringResource(Res.string.qa_last_checked, agoText(last.minutes))
    }
}

@Composable
private fun agoText(minutes: Long): String = when {
    minutes < 60 -> stringResource(Res.string.qa_ago_minutes, minutes)
    minutes < 60 * 24 -> stringResource(Res.string.qa_ago_hours, minutes / 60)
    else -> stringResource(Res.string.qa_ago_days, minutes / (60 * 24))
}

@Composable
private fun QuickActionRow(
    shape: Shape,
    icon: ImageVector,
    iconContainer: Color,
    iconContent: Color,
    title: String,
    supporting: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(iconContainer)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconContent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            headlineContent = { Text(title) },
            supportingContent = { Text(supporting) },
            trailingContent = trailing,
        )
    }
}

@Composable
private fun RunChip() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = stringResource(Res.string.qa_run),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun KeyCap(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@ShowcaseComponent(
    name = "Quick Actions List",
    description = "Segmented quick-actions list, idle state.",
    group = "Quick Actions"
)
@Composable
fun QuickActionsListIdleSample() {
    QuickActionsList(
        state = QuickActionsUiState(ManualCheckState.Idle, minutesAgo(12), 4),
        onAction = {},
    )
}

@ShowcaseComponent(
    name = "Quick Actions List (Running)",
    description = "Segmented quick-actions list while a check is running.",
    group = "Quick Actions"
)
@Composable
fun QuickActionsListRunningSample() {
    QuickActionsList(
        state = QuickActionsUiState(ManualCheckState.Running(3, 12), minutesAgo(12), 4),
        onAction = {},
    )
}

@ShowcaseComponent(
    name = "Quick Actions List (Never checked)",
    description = "Segmented quick-actions list before any check, no scheduled count.",
    group = "Quick Actions"
)
@Composable
fun QuickActionsListNeverCheckedSample() {
    QuickActionsList(
        state = QuickActionsUiState(ManualCheckState.Idle, null, null),
        onAction = {},
    )
}

private fun minutesAgo(minutes: Long): Long = Clock.System.now().toEpochMilliseconds() - minutes * 60_000L
