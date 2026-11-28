package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionKind
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.exc_browse
import otakuworld.kmpuiviews.generated.resources.exc_browse_desc
import otakuworld.kmpuiviews.generated.resources.exc_check_update
import otakuworld.kmpuiviews.generated.resources.exc_check_update_desc
import otakuworld.kmpuiviews.generated.resources.exc_open_website
import otakuworld.kmpuiviews.generated.resources.exc_open_website_desc_android
import otakuworld.kmpuiviews.generated.resources.exc_open_website_desc_desktop
import otakuworld.kmpuiviews.generated.resources.exc_source_settings
import otakuworld.kmpuiviews.generated.resources.exc_source_settings_desc
import otakuworld.kmpuiviews.generated.resources.exc_suggested

enum class SourceAction { OpenWebsite, CheckUpdate, Browse, Settings }

/** The menu item marked "Suggested" for an error kind. App errors have no source, so none. */
fun suggestedAction(kind: ExceptionKind): SourceAction? = when (kind) {
    ExceptionKind.Blocked, ExceptionKind.Parsing, ExceptionKind.Network -> SourceAction.OpenWebsite
    ExceptionKind.Source -> SourceAction.CheckUpdate
    ExceptionKind.App -> null
}

/** Menu items for [row], suggested first. Without an installed source only the update check makes sense. */
fun availableActions(row: ExceptionRow): List<SourceAction> {
    if (row.parsed.kind == ExceptionKind.App) return emptyList()
    if (row.source == null) return listOf(SourceAction.CheckUpdate)
    val suggested = suggestedAction(row.parsed.kind)
    return SourceAction.entries.sortedByDescending { it == suggested }
}

@Composable
fun SourceActionsMenu(
    row: ExceptionRow,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (SourceAction) -> Unit,
) {
    val suggested = suggestedAction(row.parsed.kind)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        availableActions(row).forEach { action ->
            val (title, desc) = when (action) {
                SourceAction.OpenWebsite -> Res.string.exc_open_website to
                    if (hasShareSheet) Res.string.exc_open_website_desc_android else Res.string.exc_open_website_desc_desktop
                SourceAction.CheckUpdate -> Res.string.exc_check_update to Res.string.exc_check_update_desc
                SourceAction.Browse -> Res.string.exc_browse to Res.string.exc_browse_desc
                SourceAction.Settings -> Res.string.exc_source_settings to Res.string.exc_source_settings_desc
            }
            DropdownMenuItem(
                text = {
                    Column(Modifier.widthIn(max = 260.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(title))
                            if (action == suggested) {
                                Text(
                                    stringResource(Res.string.exc_suggested),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        Text(
                            stringResource(desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = {
                    onDismiss()
                    onAction(action)
                },
            )
        }
    }
}
