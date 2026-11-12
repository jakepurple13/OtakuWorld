package com.programmersbox.novel.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.presentation.components.ai.AiCard
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_chapter_recap
import otakuworld.kmpuiviews.generated.resources.ai_previously_in

private const val RECAP_EXPAND_MS = 400

/**
 * A collapsed "Previously, in ch. N" bar above the chapter text. Nothing generates until it is
 * first expanded; collapsing again keeps any running generation going.
 */
@Composable
internal fun ChapterRecapBar(
    viewModel: ReadViewModel,
    modifier: Modifier = Modifier,
) {
    val enabled by viewModel.recapEnabled.collectAsState(initial = true)
    val available by viewModel.recaps.available.collectAsState()
    val current = viewModel.currentChapterModel
    val hasPrevious = viewModel.previousChapterModel != null

    AnimatedVisibility(
        visible = enabled && available && hasPrevious && current != null,
        enter = expandVertically(tween(RECAP_EXPAND_MS)) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        if (current != null) {
            ChapterRecapContent(
                chapterUrl = current.url,
                previousNumber = (viewModel.list.size - (viewModel.currentChapter + 1)).toString(),
                state = viewModel.recaps.stateFor(current.url).collectAsState().value,
                onRequest = viewModel::requestRecap,
            )
        }
    }
}

@Composable
private fun ChapterRecapContent(
    chapterUrl: String,
    previousNumber: String,
    state: AiTextState,
    onRequest: () -> Unit,
) {
    var expanded by rememberSaveable(chapterUrl) { mutableStateOf(false) }
    // Covers the first tap and a restored expanded state; request is a no-op unless Idle or Error
    LaunchedEffect(chapterUrl, expanded) { if (expanded) onRequest() }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(RECAP_EXPAND_MS),
        label = "recapChevron",
    )

    Column(modifier = Modifier.padding(4.dp)) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Spark(spinning = state is AiTextState.Loading || state is AiTextState.Streaming)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.ai_previously_in, previousNumber),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevron),
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(RECAP_EXPAND_MS)) + fadeIn(),
            exit = shrinkVertically(tween(RECAP_EXPAND_MS)) + fadeOut(),
        ) {
            AiCard(
                label = stringResource(Res.string.ai_chapter_recap),
                state = state,
                onRetry = onRequest,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
