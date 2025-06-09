package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.platform.Clipboard
import com.programmersbox.kmpmodels.KmpSourceInformation

expect class SourceInfoRepository {
    fun versionName(kmpSourceInformation: KmpSourceInformation): String
    fun uninstall(kmpSourceInformation: KmpSourceInformation)
    suspend fun copyUrl(clipboard: Clipboard, url: String)
}