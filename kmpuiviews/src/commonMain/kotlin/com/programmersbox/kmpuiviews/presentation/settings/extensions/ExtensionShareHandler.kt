package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpSourceInformation
import io.github.vinceglb.filekit.PlatformFile

expect class ExtensionShareHandler {
    suspend fun shareExtensions(platformFile: PlatformFile, extensions: List<KmpSourceInformation>)
}