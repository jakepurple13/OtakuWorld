package com.programmersbox.anime.shared.cast

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import org.koin.compose.koinInject

@Composable
fun CastButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val controller = koinInject<CastController>()
    val state by controller.state.collectAsStateWithLifecycle()
    if (state.connection == CastConnection.Unavailable || state.connection == CastConnection.NoDevices) return
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            if (state.connection == CastConnection.Connected) Icons.Default.CastConnected else Icons.Default.Cast,
            contentDescription = "Cast",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDevicePickerSheet(onDismiss: () -> Unit, onConnected: () -> Unit) {
    val context = LocalContext.current
    val controller = koinInject<CastController>()
    val state by controller.state.collectAsStateWithLifecycle()
    val currentOnConnected by rememberUpdatedState(onConnected)

    LaunchedEffect(state.connection) {
        if (state.connection == CastConnection.Connected) currentOnConnected()
    }

    val router = remember(context) { MediaRouter.getInstance(context) }
    val selector = remember(context) {
        runCatching { CastContext.getSharedInstance(context).mergedSelector }.getOrNull()
    }
    var routes by remember { mutableStateOf(emptyList<MediaRouter.RouteInfo>()) }

    DisposableEffect(router, selector) {
        if (selector == null) return@DisposableEffect onDispose { }
        fun refresh() {
            routes = router.routes.filter { !it.isDefault && it.matchesSelector(selector) && it.isEnabled }
        }

        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        }
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        refresh()
        onDispose { router.removeCallback(callback) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Cast to", style = MaterialTheme.typography.titleLarge)
            Text(
                "Same Wi-Fi as this phone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.connection == CastConnection.Connected) {
                Text(
                    "Connected to ${state.deviceName ?: "device"}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedButton(onClick = { controller.stop(); onDismiss() }) { Text("Stop casting") }
            } else {
                val connectingName = state.deviceName.takeIf { state.connection == CastConnection.Connecting }
                routes.forEach { route ->
                    DeviceRow(
                        route = route,
                        selected = route.isSelected || route.name == connectingName,
                        onClick = { router.selectRoute(route) },
                    )
                }
                if (routes.isEmpty()) {
                    Text(
                        "No TVs found on this Wi-Fi",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) { Text("Wi-Fi settings") }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        "Looking for more devices…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(route: MediaRouter.RouteInfo, selected: Boolean, onClick: () -> Unit) {
    val isTv = route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_TV
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Icon(if (isTv) Icons.Default.Tv else Icons.Default.Cast, contentDescription = null)
            Column {
                Text(route.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (isTv) "Google TV" else "Chromecast",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
