package com.programmersbox.kmpuiviews.utils.composables.icons

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Custom Icons",
    description = "Custom icons.",
    group = "Icons"
)
@Composable
fun IconSample() {
    FlowRow(
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Discord, null)
        Icon(Icons.Github, null)
        Icon(Icons.Default.CatchingPokemon, null)
    }
}