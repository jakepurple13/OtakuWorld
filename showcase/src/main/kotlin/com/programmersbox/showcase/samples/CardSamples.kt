package com.programmersbox.showcase.samples

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Simple Card",
    description = "A basic Material 3 card with text content.",
    group = "Cards",
)
@Composable
fun SimpleCardSample() {
    Card {
        Text(
            text = "Card content",
            modifier = Modifier.padding(16.dp),
        )
    }
}
