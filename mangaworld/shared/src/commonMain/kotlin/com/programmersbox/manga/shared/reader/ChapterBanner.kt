package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** How long the chapter banner stays up after the last [ChapterBannerState.show]. */
internal const val CHAPTER_BANNER_MILLIS = 2_000L

@Stable
internal class ChapterBannerState {
    var chapterName: String? by mutableStateOf(null)
        private set

    internal var token by mutableIntStateOf(0)
        private set

    internal var visible by mutableStateOf(false)

    fun show(name: String) {
        chapterName = name
        visible = true
        token++
    }
}

/** Pill that drops from the top when the chapter changes, then lifts away after [CHAPTER_BANNER_MILLIS]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChapterBanner(
    state: ChapterBannerState,
    pageCount: Int?,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.token) {
        if (state.token == 0) return@LaunchedEffect
        delay(CHAPTER_BANNER_MILLIS)
        state.visible = false
    }

    AnimatedVisibility(
        visible = state.visible,
        enter = slideInVertically { -it } +
                scaleIn(spring(dampingRatio = 0.6f)) +
                fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = slideOutVertically { -it } +
                scaleOut(targetScale = .8f) +
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(state.chapterName.orEmpty(), style = MaterialTheme.typography.labelLarge)
                if (pageCount != null) {
                    Text(
                        " · $pageCount pages",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
