package com.programmersbox.manga.shared.downloads

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.utils.PermissionRequest

@Composable
actual fun PermissionRequester(content: @Composable (() -> Unit)) {
    PermissionRequest(
        if (Build.VERSION.SDK_INT >= 33)
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        else listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    ) { content() }
}