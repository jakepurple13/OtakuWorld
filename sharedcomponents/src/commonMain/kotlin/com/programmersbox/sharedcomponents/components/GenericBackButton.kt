package com.programmersbox.sharedcomponents.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.programmersbox.showcase.annotations.ShowcaseComponent

@Composable
fun GenericBackButton() {
    val navEvent = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher

    val navInput = remember { DirectNavigationEventInput() }

    DisposableEffect(Unit) {
        navEvent?.addInput(navInput)
        onDispose { navEvent?.removeInput(navInput) }
    }

    IconButton(
        onClick = { navInput.backCompleted() }
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
}

@ShowcaseComponent(
    name = "Generic Back Button",
    description = "A simple back button that is located in shared components.",
    group = "Buttons",
)
@Composable
fun GenericBackButtonSample() {
    GenericBackButton()
}