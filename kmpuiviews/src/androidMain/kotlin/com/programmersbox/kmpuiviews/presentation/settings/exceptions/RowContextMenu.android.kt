package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.runtime.Composable

@Composable
actual fun RowContextMenu(
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onOpenWebsite: (() -> Unit)?,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) = content()

@Composable
actual fun keyboardHint(): String? = null
