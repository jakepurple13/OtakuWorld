package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.LocalPageRefreshHost
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.details_refresh
import otakuworld.kmpuiviews.generated.resources.details_refreshing
import otakuworld.kmpuiviews.generated.resources.details_retry
import otakuworld.kmpuiviews.generated.resources.details_retrying

/**
 * The Refresh row at the top of the Details overflow menu, with its divider. Not shown in the desktop tab shell, whose
 * toolbar has its own Refresh button.
 */
@Composable
internal fun RefreshMenuItem(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    dismiss: () -> Unit,
) {
    if (LocalPageRefreshHost.current != null) return
    DropdownMenuItem(
        onClick = {
            dismiss()
            onRefresh()
        },
        enabled = !isRefreshing,
        text = { Text(stringResource(if (isRefreshing) Res.string.details_refreshing else Res.string.details_refresh)) },
        leadingIcon = {
            if (isRefreshing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, null)
            }
        },
    )
    HorizontalDivider()
}

/** The progress line under the Details top bar while a refresh runs. The desktop toolbar shows its own. */
@Composable
internal fun DetailsRefreshProgress(isRefreshing: Boolean) {
    AnimatedVisibility(
        visible = isRefreshing && LocalPageRefreshHost.current == null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

/**
 * Flashes the primary container color behind this element for 900 ms when [key] changes, so a refresh shows what it
 * changed. Never flashes on first composition.
 */
@Composable
internal fun Modifier.highlightOnChange(key: Any?): Modifier {
    val color = MaterialTheme.colorScheme.primaryContainer
    val alpha = remember { Animatable(0f) }
    val shown = remember { arrayOf(key) }
    LaunchedEffect(key) {
        if (shown[0] == key) return@LaunchedEffect
        shown[0] = key
        alpha.snapTo(1f)
        alpha.animateTo(0f, tween(900))
    }
    return drawBehind {
        if (alpha.value > 0f) drawRoundRect(color.copy(alpha = alpha.value), cornerRadius = CornerRadius(4.dp.toPx()))
    }
}

/** Retry on the Details error card. The desktop toolbar's Refresh does this there, so it shows on mobile only. */
@Composable
internal fun DetailErrorRetry(isRefreshing: Boolean, onRetry: () -> Unit) {
    if (LocalPageRefreshHost.current != null) return
    TextButton(onClick = onRetry, enabled = !isRefreshing) {
        Text(stringResource(if (isRefreshing) Res.string.details_retrying else Res.string.details_retry))
    }
}
