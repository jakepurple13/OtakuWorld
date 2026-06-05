package com.programmersbox.koogintegration.screens.chatscreen

// Define UI Events for the agent demo screen
sealed interface ChatUiEvents {
    data class UpdateInputText(val text: String) : ChatUiEvents
    data object ToggleDebugEnabled : ChatUiEvents
    data class ToggleDebugOption(val option: DebugOption) : ChatUiEvents
    data object SendMessage : ChatUiEvents
    data object RestartChat : ChatUiEvents
    data object ShowMermaidGraph : ChatUiEvents
}
