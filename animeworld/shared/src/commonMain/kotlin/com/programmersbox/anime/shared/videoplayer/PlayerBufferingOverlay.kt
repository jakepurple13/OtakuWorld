package com.programmersbox.anime.shared.videoplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice

/** Percent of [targetMs] buffered ahead of the playhead, clamped 0..100. */
fun bufferPercent(bufferedMs: Long, positionMs: Long, targetMs: Long = 5_000): Int =
    ((bufferedMs - positionMs) * 100 / targetMs).coerceIn(0, 100).toInt()

/**
 * Buffering ring with "Buffering n%". While [showPoster] (before the first frame) it sits on the
 * blurred, darkened cover; later rebuffers show only the ring and text over the video.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerBufferingOverlay(
    visible: Boolean,
    percent: Int,
    posterUrl: String?,
    showPoster: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible && showPoster,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                posterUrl?.let {
                    ImageLoaderChoice(
                        imageUrl = it,
                        name = "",
                        placeHolder = { ColorPainter(Color.Black) },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp)
                            .alpha(0.35f)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularWavyProgressIndicator(
                    progress = { percent / 100f },
                    color = Color.White,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    "Buffering $percent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}
