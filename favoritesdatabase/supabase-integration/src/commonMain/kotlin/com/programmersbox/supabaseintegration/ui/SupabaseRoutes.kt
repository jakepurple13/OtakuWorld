package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.programmersbox.supabaseintegration.Res
import com.programmersbox.supabaseintegration.di.SupabaseActions
import com.programmersbox.supabaseintegration.supabase_logo_icon
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Serializable
data object SupabaseConfigRoute : NavKey

@Serializable
data object AuthRoute : NavKey

@Serializable
data object SyncStatusRoute : NavKey

@Serializable
data object BackupRestoreRoute : NavKey

@Serializable
data object BackupPreferencesRoute : NavKey

@Serializable
data object SupabaseRoutes : NavKey

fun EntryProviderScope<NavKey>.supabaseRoutes(
    hideComposable: @Composable () -> Unit,
) {
    entry<SupabaseConfigRoute> {
        hideComposable()
        SupabaseConfigScreen()
    }
    entry<AuthRoute> {
        hideComposable()
        AuthScreen()
    }
    entry<SyncStatusRoute> {
        hideComposable()
        SyncStatusScreen()
    }
    entry<BackupRestoreRoute> {
        hideComposable()
        BackupRestoreScreen(
            getLocalDbPath = { "" }
        )
    }
    entry<BackupPreferencesRoute> {
        hideComposable()
        BackupPreferencesScreen()
    }
    entry<SupabaseRoutes> {
        val actions = koinInject<SupabaseActions>()
        SupabaseSettingsScreen(
            onNavigate = actions.onNavigate
        )
    }
}

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

@Composable
fun SupabaseIcon() {
    Image(
        painterResource(Res.drawable.supabase_logo_icon),
        null,
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
    )
}