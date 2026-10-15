package com.programmersbox.manga.shared.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChapterTransitionItem(
    fromChapterName: String?,
    toChapterName: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when {
                fromChapterName == null -> Text(
                    "No more chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Finished",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            fromChapterName,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider()

            when {
                toChapterName == null -> Text(
                    "No more chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Next",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            toChapterName,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChapterPage(
    chapterLink: () -> String,
    isDownloaded: Boolean,
    headers: Map<String, String>,
    contentScale: ContentScale,
    imageLoaderType: ImageLoaderType,
    colorFilter: ColorFilter? = null,
) {
    if (imageLoaderType == ImageLoaderType.Panpf) {
        Panpf(
            painter = chapterLink(),
            headers = headers,
            modifier = Modifier.fillMaxWidth(),
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .requiredHeightIn(min = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            ZoomableImage(
                painter = chapterLink(),
                isDownloaded = isDownloaded,
                headers = headers,
                contentScale = contentScale,
                imageLoaderType = imageLoaderType,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    painter: String,
    isDownloaded: Boolean,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
    imageLoaderType: ImageLoaderType,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            /*.clickable(
                indication = null,
                onClick = onClick,
                interactionSource = null
            )*/
    ) {
        val scope = rememberCoroutineScope()
        var showTheThing by remember { mutableStateOf(true) }

        imageLoaderType.Composed(
            painter = painter,
            colorFilter = colorFilter,
            onRefresh = {
                scope.launch {
                    showTheThing = false
                    delay(1000)
                    showTheThing = true
                }
            },
            contentScale = contentScale,
            headers = headers,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private fun clampOffset(centerPoint: Offset, offset: Offset, scale: Float): Offset {
    val maxPosition = centerPoint * (scale - 1)

    return offset.copy(
        x = offset.x.coerceIn(-maxPosition.x, maxPosition.x),
        y = offset.y.coerceIn(-maxPosition.y, maxPosition.y)
    )
}
