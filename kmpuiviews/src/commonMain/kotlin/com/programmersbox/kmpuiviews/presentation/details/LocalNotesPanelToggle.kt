package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Toggles the desktop tab's Notes side panel. Null everywhere else (Android never provides it), and
 * then Details opens the note sheet as usual.
 */
val LocalNotesPanelToggle = staticCompositionLocalOf<(() -> Unit)?> { null }
