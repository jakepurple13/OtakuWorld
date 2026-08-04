package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpSourceInformation
import io.github.vinceglb.filekit.PlatformFile

actual class ExtensionShareHandler {
    actual suspend fun shareExtensions(
        platformFile: PlatformFile,
        extensions: List<KmpSourceInformation>,
    ) {
    }
}