package com.programmersbox.koogintegration.screens.chatscreen

import androidx.compose.runtime.Stable
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.koogintegration.agentresponse.GeneratedCustomListResponse

// Define UI Events for the agent demo screen
sealed interface ChatUiEvents {
    data class UpdateInputText(val text: String) : ChatUiEvents
    data object ToggleDebugEnabled : ChatUiEvents
    data class ToggleDebugOption(val option: DebugOption) : ChatUiEvents
    data object SendMessage : ChatUiEvents
    data object RestartChat : ChatUiEvents
    data object ShowMermaidGraph : ChatUiEvents
    data class SaveGeneratedList(
        val generatedCustomListResponse: GeneratedCustomListResponse,
        val chosenName: String,
        val useBiometrics: Boolean,
        val itemsToSave: List<CustomListInfo>,
    ) : ChatUiEvents
}

@Stable
data class KoogNavigation(
    val onSearchClick: (String) -> Unit,
    val onKoogSettingsClick: () -> Unit,
    val onBack: () -> Unit,
    val onListClick: () -> Unit,
)