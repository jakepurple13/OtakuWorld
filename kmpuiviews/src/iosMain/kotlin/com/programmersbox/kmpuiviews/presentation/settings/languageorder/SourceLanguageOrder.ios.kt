package com.programmersbox.kmpuiviews.presentation.settings.languageorder

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.currentLocale

actual fun languageDisplayName(code: String): String =
    NSLocale.currentLocale.displayNameForKey(NSLocaleIdentifier, code) ?: code
