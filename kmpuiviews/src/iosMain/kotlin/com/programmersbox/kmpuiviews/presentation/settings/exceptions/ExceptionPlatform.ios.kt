package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import com.programmersbox.kmpuiviews.platform

actual val hasShareSheet: Boolean = false

actual fun deviceDescription(): String = platform()
