package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.navigation3.runtime.NavKey

/** Only Android posts the morning notification; elsewhere the settings hide that group. */
expect val supportsMorningNotification: Boolean

/** The AI Helper's models screen, or null where there is no AI Helper (iOS). */
expect val aiHelperModelsRoute: NavKey?

/** The AI Helper's provider settings screen, or null where there is no AI Helper (iOS). */
expect val aiHelperSettingsRoute: NavKey?
