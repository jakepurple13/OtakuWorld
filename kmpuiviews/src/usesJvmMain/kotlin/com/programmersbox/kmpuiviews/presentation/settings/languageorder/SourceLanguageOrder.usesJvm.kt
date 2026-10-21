package com.programmersbox.kmpuiviews.presentation.settings.languageorder

import java.util.Locale

actual fun languageDisplayName(code: String): String =
    Locale.forLanguageTag(code).getDisplayName(Locale.getDefault()).ifBlank { code }
