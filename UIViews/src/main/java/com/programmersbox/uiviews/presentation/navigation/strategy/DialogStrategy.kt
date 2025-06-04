package com.programmersbox.uiviews.presentation.navigation.strategy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy

class DialogScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val firstEntry: NavEntry<T>,
    val secondEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(firstEntry, secondEntry)
    override val content: @Composable (() -> Unit) = {
        Box(modifier = Modifier.fillMaxSize()) {
            firstEntry.content.invoke(firstEntry.key)
            secondEntry.content.invoke(secondEntry.key)
        }
    }

    companion object {
        internal const val DIALOG_KEY = "Dialog"

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * in a two-pane layout.
         */
        fun dialog() = mapOf(DIALOG_KEY to true)
    }
}

// --- TwoPaneSceneStrategy ---
/**
 * A [SceneStrategy] that activates a [TwoPaneScene] if the window is wide enough
 * and the top two back stack entries declare support for two-pane display.
 */
class DialogStrategy<T : Any> : SceneStrategy<T> {
    @Composable
    override fun calculateScene(
        entries: List<NavEntry<T>>,
        onBack: (Int) -> Unit,
    ): Scene<T>? {
        return if (
            entries.size >= 2 &&
            entries.any { it.metadata.containsKey(DialogScene.DIALOG_KEY) }
        ) {
            DialogScene(
                key = "Dialog",
                previousEntries = entries.dropLast(1),
                firstEntry = entries.last { !it.metadata.containsKey(DialogScene.DIALOG_KEY) },
                secondEntry = entries.last { it.metadata.containsKey(DialogScene.DIALOG_KEY) }
            )
        } else {
            null
        }
    }
}