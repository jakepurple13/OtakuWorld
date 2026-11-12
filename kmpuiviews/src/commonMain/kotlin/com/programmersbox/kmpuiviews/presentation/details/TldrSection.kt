package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.presentation.components.ai.AiCard
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import com.programmersbox.kmpuiviews.presentation.components.ai.StreamedText
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_translated
import otakuworld.kmpuiviews.generated.resources.ai_translating
import otakuworld.kmpuiviews.generated.resources.ai_tldr

/** What the Details screen needs to show the TL;DR chip and card. */
class TldrUi(
    val available: Boolean,
    val state: AiTextState,
    val translating: Boolean,
    val translated: Boolean,
    val onShow: () -> Unit,
    val onHide: () -> Unit,
) {
    /** The chip or the card is on screen. */
    val visible: Boolean get() = available || cardShown

    internal val cardShown: Boolean get() = state !is AiTextState.Idle || translating
}

/** A "TL;DR" chip that expands into an [AiCard] with three streamed bullets. */
@Composable
fun TldrSection(ui: TldrUi, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = ui.cardShown,
        transitionSpec = {
            if (targetState) {
                (expandVertically(tween(400)) + fadeIn()) togetherWith (fadeOut() + shrinkVertically())
            } else {
                (expandVertically() + fadeIn()) togetherWith (fadeOut() + shrinkVertically(tween(400)))
            }
        },
        modifier = modifier,
        label = "tldr",
    ) { card ->
        if (card) {
            AiCard(
                label = if (ui.translating) stringResource(Res.string.ai_translating) else stringResource(Res.string.ai_tldr),
                state = ui.state,
                onRetry = ui.onShow,
                headerAction = {
                    IconButton(onClick = ui.onHide) { Icon(Icons.Default.Close, null) }
                },
                footerPrefix = if (ui.translated) stringResource(Res.string.ai_translated) else null,
                body = { TldrBullets(it, streaming = ui.state is AiTextState.Streaming) },
            )
        } else if (ui.available) {
            AssistChip(
                onClick = ui.onShow,
                leadingIcon = { Spark(spinning = false) },
                label = { Text(stringResource(Res.string.ai_tldr)) },
            )
        } else {
            Spacer(Modifier)
        }
    }
}

@Composable
private fun TldrBullets(text: String, streaming: Boolean) {
    // While streaming the last line may be a partial marker like "1", so only complete lines show
    val complete = if (streaming) text.substringBeforeLast('\n', missingDelimiterValue = "") else text
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AiTextPrompts.parseBullets(complete).forEach { bullet ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StreamedText(
                    text = bullet,
                    minHeight = 0.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
