package com.programmersbox.kmpuiviews.presentation.settings.exceptions

/** True where Share opens a system share sheet and Open website can use the in-app WebView (Android). */
expect val hasShareSheet: Boolean

/** The report's "Device:" line, e.g. "Android 16 · Pixel 8" or "Mac OS X 26.1 · aarch64". */
expect fun deviceDescription(): String
