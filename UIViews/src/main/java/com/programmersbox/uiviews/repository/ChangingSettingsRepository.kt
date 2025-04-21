package com.programmersbox.uiviews.repository

import kotlinx.coroutines.flow.MutableStateFlow

class ChangingSettingsRepository {
    val showNavBar = MutableStateFlow(true)
    val showInsets = MutableStateFlow(true)
}