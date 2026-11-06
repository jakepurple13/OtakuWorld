package com.programmersbox.kmpuiviews.notifier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import com.materialkolor.material3.rememberDynamicColorScheme
import com.programmersbox.sharedcomponents.notifier.Banner
import com.programmersbox.sharedcomponents.notifier.BannerKind
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.banner_dismiss

private val SuccessSeed = Color(0xFF3C9A5F)

@Composable
private fun bannerColors(kind: BannerKind): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (kind) {
        BannerKind.Info -> scheme.inverseSurface to scheme.inverseOnSurface
        BannerKind.Error -> scheme.errorContainer to scheme.onErrorContainer
        BannerKind.Progress -> scheme.primaryContainer to scheme.onPrimaryContainer
        BannerKind.Success -> {
            val isDark = scheme.surface.luminance() < 0.5f
            val primary = scheme.primary
            val success: ColorScheme = rememberDynamicColorScheme(
                seedColor = remember(primary) { SuccessSeed.harmonize(primary) },
                isDark = isDark,
            )
            success.primaryContainer to success.onPrimaryContainer
        }
    }
}

private fun Banner.defaultIcon(): ImageVector = icon ?: when (kind) {
    BannerKind.Success, BannerKind.Progress -> Icons.Default.Check
    BannerKind.Info -> Icons.Default.Info
    BannerKind.Error -> Icons.Default.ErrorOutline
}

@Composable
fun BannerCard(
    banner: Banner,
    contentAlpha: Float,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
) {
    val (container, content) = bannerColors(banner.kind)
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = if (banner.kind == BannerKind.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(20.dp),
        color = container,
        contentColor = content,
        shadowElevation = 6.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = contentAlpha }
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(content.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (banner.kind == BannerKind.Progress && banner.icon == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = content,
                        )
                    } else {
                        Icon(banner.defaultIcon(), contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        banner.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    banner.subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                banner.action?.let {
                    TextButton(onClick = onAction) { Text(it, color = content) }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.banner_dismiss),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            BannerProgressBar(banner, content, paused, Modifier.graphicsLayer { alpha = contentAlpha })
        }
    }
}

@Composable
private fun BannerProgressBar(banner: Banner, color: Color, paused: Boolean, modifier: Modifier) {
    val trackColor = color.copy(alpha = 0.15f)
    val barModifier = modifier
        .fillMaxWidth()
        .height(3.dp)
    val duration = banner.durationMillis
    when {
        banner.kind == BannerKind.Progress -> {
            val fraction = banner.fraction
            if (fraction == null) {
                LinearProgressIndicator(modifier = barModifier, color = color, trackColor = trackColor)
            } else {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = barModifier,
                    color = color,
                    trackColor = trackColor,
                )
            }
        }

        duration != null -> {
            val remaining = remember(banner) { Animatable(1f) }
            // Freezes while paused and resumes from where it stopped.
            LaunchedEffect(banner, paused) {
                if (!paused) {
                    remaining.animateTo(0f, tween((duration * remaining.value).toInt(), easing = LinearEasing))
                }
            }
            LinearProgressIndicator(
                progress = { remaining.value },
                modifier = barModifier,
                color = color,
                trackColor = trackColor,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}
