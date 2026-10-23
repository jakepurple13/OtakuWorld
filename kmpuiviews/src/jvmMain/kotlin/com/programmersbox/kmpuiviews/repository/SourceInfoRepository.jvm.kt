package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.platform.Clipboard
import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpmodels.KmpSourceInformation

actual class SourceInfoRepository(
    private val stores: List<ApkExtensionStore> = emptyList(),
) {
    actual fun versionName(kmpSourceInformation: KmpSourceInformation): String =
        stores.firstNotNullOfOrNull { it.versionName(kmpSourceInformation.packageName) }
            ?: kmpSourceInformation.name

    actual fun uninstall(kmpSourceInformation: KmpSourceInformation) {
    }

    actual suspend fun copyUrl(clipboard: Clipboard, url: String) {
    }
}