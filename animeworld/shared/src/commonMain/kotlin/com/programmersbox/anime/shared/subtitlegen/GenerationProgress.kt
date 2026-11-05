package com.programmersbox.anime.shared.subtitlegen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One-line status under a library row while subtitles are generated for it. */
fun generationStatusText(status: GenerationStatus): String = when (status) {
    is GenerationStatus.Queued -> "Queued · starts after ${status.afterTitle}"
    GenerationStatus.ExtractingAudio -> "Extracting audio…"
    is GenerationStatus.DownloadingModel -> "Downloading ${status.modelName} · ${percent(status.fraction)}"
    is GenerationStatus.Transcribing -> buildString {
        val sameLanguage = status.to.code == "auto" || status.to == status.from
        val fromAuto = status.from.code == "auto"
        append(
            when {
                sameLanguage -> if (fromAuto) "Transcribing" else "Transcribing ${status.from.name}"
                fromAuto -> "Auto-detect → ${status.to.name}"
                else -> "${status.from.name} → ${status.to.name}"
            }
        )
        append(" · ").append(percent(status.fraction))
        status.remainingMs?.let { append(" · ").append(remainingText(it)) }
    }

    is GenerationStatus.Failed -> "Couldn't generate subtitles: ${status.message}"
}

private fun percent(fraction: Float): String = "${(fraction.coerceIn(0f, 1f) * 100).toInt()}%"

/** "about M min left" with minutes rounded up, or "less than a minute" under one minute. */
fun remainingText(remainingMs: Long): String =
    if (remainingMs < 60_000) "less than a minute" else "about ${(remainingMs + 59_999) / 60_000} min left"

@Composable
fun GenerationProgress(
    status: GenerationStatus,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        val target = when (status) {
            is GenerationStatus.DownloadingModel -> status.fraction
            is GenerationStatus.Transcribing -> status.fraction
            else -> 0f
        }
        val fraction by animateFloatAsState(
            targetValue = target,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "generationProgress"
        )
        when (status) {
            is GenerationStatus.Failed -> Unit
            GenerationStatus.ExtractingAudio -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is GenerationStatus.Queued -> LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
            else -> LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        }

        // Keyed on the kind of status so the text slides in on each new step, not on every percent tick.
        AnimatedContent(
            targetState = status,
            contentKey = { it::class },
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
            label = "generationStatus"
        ) { current ->
            if (current is GenerationStatus.Failed) {
                Column {
                    Text(
                        generationStatusText(current),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onRetry) { Text("Retry") }
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
            } else {
                Text(
                    generationStatusText(current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
