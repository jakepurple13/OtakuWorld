package com.programmersbox.kmpuiviews.settings

import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsScreenDisplayNames
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenDisplayNamesTest {

    @Test
    fun breadcrumbTextJoinsWithArrow() {
        val result = SettingsScreenDisplayNames.breadcrumbText(
            listOf(Screen.Settings.Appearance, Screen.ThemeSettings)
        )
        assertEquals("Appearance > Theme", result)
    }

    @Test
    fun breadcrumbTextSingleEntry() {
        val result = SettingsScreenDisplayNames.breadcrumbText(
            listOf(Screen.Settings.Appearance)
        )
        assertEquals("Appearance", result)
    }

    @Test
    fun breadcrumbTextEmpty() {
        assertEquals("", SettingsScreenDisplayNames.breadcrumbText(emptyList()))
    }

    @Test
    fun displayNameForKnownScreens() {
        assertEquals("Theme", SettingsScreenDisplayNames.displayNameFor(Screen.ThemeSettings))
        assertEquals("Notifications", SettingsScreenDisplayNames.displayNameFor(Screen.NotificationsSettings))
        assertEquals("Backup & Restore", SettingsScreenDisplayNames.displayNameFor(Screen.MoreSettings))
    }
}
