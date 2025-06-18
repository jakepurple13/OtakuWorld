package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.platform.Clipboard
import com.programmersbox.kmpmodels.KmpSourceInformation

actual class SourceInfoRepository {
    actual fun versionName(kmpSourceInformation: KmpSourceInformation): String = kmpSourceInformation.name

    actual fun uninstall(kmpSourceInformation: KmpSourceInformation) {
    }

    actual suspend fun copyUrl(clipboard: Clipboard, url: String) {
    }
}