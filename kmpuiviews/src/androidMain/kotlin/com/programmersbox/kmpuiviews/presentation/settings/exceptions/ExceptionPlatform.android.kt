package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import android.os.Build

actual val hasShareSheet: Boolean = true

actual fun deviceDescription(): String = "Android ${Build.VERSION.RELEASE} · ${Build.MODEL}"
