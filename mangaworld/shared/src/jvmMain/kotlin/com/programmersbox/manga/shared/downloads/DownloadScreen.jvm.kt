package com.programmersbox.manga.shared.downloads

import androidx.compose.runtime.Composable

@Composable
actual fun PermissionRequester(content: @Composable (() -> Unit)) {
    content()
}