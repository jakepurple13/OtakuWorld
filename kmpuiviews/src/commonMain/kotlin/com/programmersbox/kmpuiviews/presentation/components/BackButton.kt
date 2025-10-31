package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
fun BackButton() {
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