package com.programmersbox.kmpuiviews.repository

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.programmersbox.kmpmodels.KmpSourceInformation

actual class SourceInfoRepository(
    private val context: Context,
) {
    actual fun versionName(kmpSourceInformation: KmpSourceInformation): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    kmpSourceInformation.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                context.packageManager.getPackageInfo(kmpSourceInformation.packageName, 0)
            }
        }
            .getOrNull()
            ?.versionName
            .orEmpty()
    }

    actual fun uninstall(kmpSourceInformation: KmpSourceInformation) {
        val uri = Uri.fromParts("package", kmpSourceInformation.packageName, null)
        val uninstall = Intent(Intent.ACTION_DELETE, uri)
        uninstall.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(uninstall)
    }

    actual suspend fun copyUrl(clipboard: Clipboard, url: String) {
        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Url", url)))
    }
}