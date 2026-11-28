package com.programmersbox.kmpuiviews.presentation.settings.exceptions

actual val hasShareSheet: Boolean = false

actual fun deviceDescription(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.version")} · ${System.getProperty("os.arch")}"
