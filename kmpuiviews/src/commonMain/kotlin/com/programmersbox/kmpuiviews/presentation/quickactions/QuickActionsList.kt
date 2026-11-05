package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.qa_background_tasks
import otakuworld.kmpuiviews.generated.resources.qa_background_tasks_sub
import otakuworld.kmpuiviews.generated.resources.qa_check_updates
import otakuworld.kmpuiviews.generated.resources.qa_checking
import otakuworld.kmpuiviews.generated.resources.qa_checking_indeterminate
import otakuworld.kmpuiviews.generated.resources.qa_last_checked
import otakuworld.kmpuiviews.generated.resources.qa_never_checked
import otakuworld.kmpuiviews.generated.resources.qa_run
import otakuworld.kmpuiviews.generated.resources.qa_scan_qr
import otakuworld.kmpuiviews.generated.resources.qa_scan_qr_sub
import otakuworld.kmpuiviews.generated.resources.qa_scheduled
import otakuworld.kmpuiviews.generated.resources.quick_actions

private val FirstShape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 6.dp)
private val MiddleShape = RoundedCornerShape(6.dp)
private val LastShape = RoundedCornerShape(6.dp, 6.dp, 18.dp, 18.dp)

@Composable
fun QuickActionsList(
    state: QuickActionsUiState,
    onAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
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
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            QuickActionRow(
                shape = FirstShape,
                icon = Icons.Default.Refresh,
                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                iconContent = MaterialTheme.colorScheme.onPrimaryContainer,
                title = stringResource(Res.string.qa_check_updates),
                supporting = checkSupportingText(state),
                onClick = { onAction(QuickAction.CheckForUpdates) },
                trailing = { RunChip() },
            )
            QuickActionRow(
                shape = MiddleShape,
                icon = Icons.Default.QrCodeScanner,
                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                iconContent = MaterialTheme.colorScheme.onTertiaryContainer,
                title = stringResource(Res.string.qa_scan_qr),
                supporting = stringResource(Res.string.qa_scan_qr_sub),
                onClick = { onAction(QuickAction.ScanQrCode) },
                trailing = { Chevron() },
            )
            QuickActionRow(
                shape = LastShape,
                icon = Icons.Default.Schedule,
                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                iconContent = MaterialTheme.colorScheme.onSecondaryContainer,
                title = stringResource(Res.string.qa_background_tasks),
                supporting = state.scheduledCount
                    ?.let { stringResource(Res.string.qa_scheduled, it) }
                    ?: stringResource(Res.string.qa_background_tasks_sub),
                onClick = { onAction(QuickAction.BackgroundTasks) },
                trailing = { Chevron() },
            )
        }
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
    return when (val last = state.lastChecked) {
        LastChecked.Never -> stringResource(Res.string.qa_never_checked)
        is LastChecked.Ago -> stringResource(Res.string.qa_last_checked, agoText(last.minutes))
    }
}

private fun agoText(minutes: Long): String = when {
    minutes < 60 -> "${minutes}m ago"
    minutes < 60 * 24 -> "${minutes / 60}h ago"
    else -> "${minutes / (60 * 24)}d ago"
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
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
        state = QuickActionsUiState(ManualCheckState.Idle, LastChecked.Ago(12), 4),
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
        state = QuickActionsUiState(ManualCheckState.Running(3, 12), LastChecked.Ago(12), 4),
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
        state = QuickActionsUiState(ManualCheckState.Idle, LastChecked.Never, null),
        onAction = {},
    )
}
