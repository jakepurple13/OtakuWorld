package com.programmersbox.showcase.samples

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Primary Button",
    description = "Standard filled Material 3 button.",
    group = "Buttons",
)
@Composable
fun PrimaryButtonSample() {
    Button(onClick = {}) {
        Text("Primary Button")
    }
}

@ShowcaseComponent(
    name = "Text Button",
    description = "Low-emphasis text-only Material 3 button.",
    group = "Buttons",
)
@Composable
fun TextButtonSample() {
    TextButton(onClick = {}) {
        Text("Text Button")
    }
}
