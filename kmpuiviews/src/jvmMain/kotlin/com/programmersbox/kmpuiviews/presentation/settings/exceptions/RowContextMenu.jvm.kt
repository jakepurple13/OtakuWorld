package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.exc_copy_report
import otakuworld.kmpuiviews.generated.resources.exc_delete
import otakuworld.kmpuiviews.generated.resources.exc_keys_hint_mac
import otakuworld.kmpuiviews.generated.resources.exc_keys_hint_other
import otakuworld.kmpuiviews.generated.resources.exc_open_website
import otakuworld.kmpuiviews.generated.resources.exc_save_report

@Composable
actual fun RowContextMenu(
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onOpenWebsite: (() -> Unit)?,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val copy = stringResource(Res.string.exc_copy_report)
    val save = stringResource(Res.string.exc_save_report)
    val website = stringResource(Res.string.exc_open_website)
    val delete = stringResource(Res.string.exc_delete)
    ContextMenuArea(
        items = {
            listOfNotNull(
                ContextMenuItem(copy, onCopy),
                ContextMenuItem(save, onSave),
                onOpenWebsite?.let { ContextMenuItem(website, it) },
                ContextMenuItem(delete, onDelete),
            )
        },
        content = content,
    )
}

private val isMac = System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)

@Composable
actual fun keyboardHint(): String? =
    stringResource(if (isMac) Res.string.exc_keys_hint_mac else Res.string.exc_keys_hint_other)
