package com.programmersbox.uiviews

import kotlinx.coroutines.flow.MutableStateFlow

class ChangingSettingsRepository {
    val showNavBar = MutableStateFlow(true)
}