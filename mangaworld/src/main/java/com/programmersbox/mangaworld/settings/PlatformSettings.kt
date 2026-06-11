package com.programmersbox.mangaworld.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object PlatformSettings : NavKey

class AndroidSettingsViewModel(
    private val mangaSettings: MangaNewSettingsHandling,
) : ViewModel() {

    val downloadPath = mangaSettings.downloadPath.asFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setDownloadPath(uri: String) {
        viewModelScope.launch { mangaSettings.downloadPath.set(uri) }
    }

    fun resetDownloadPath() {
        viewModelScope.launch { mangaSettings.downloadPath.set("") }
    }
}
