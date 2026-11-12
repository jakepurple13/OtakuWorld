package com.programmersbox.kmpuiviews.presentation.dailyoverview.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.presentation.components.ai.AiShimmer
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import com.programmersbox.kmpuiviews.presentation.components.ai.StreamedText
import com.programmersbox.kmpuiviews.presentation.components.ai.generatingBorder
import com.programmersbox.kmpuiviews.presentation.dailyoverview.SummaryState
import com.programmersbox.showcase.annotations.ShowcaseComponent
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_privacy_on_device
import otakuworld.kmpuiviews.generated.resources.do_privacy_sent
import otakuworld.kmpuiviews.generated.resources.do_regenerate
import otakuworld.kmpuiviews.generated.resources.do_set_up_ai
import otakuworld.kmpuiviews.generated.resources.do_summary

private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun SummaryCard(
    summary: SummaryState,
    needsSetup: Boolean,
    onRegenerate: () -> Unit,
    onSetUp: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = stringResource(Res.string.do_regenerate),
) {
    val generating = summary is SummaryState.Loading || summary is SummaryState.Streaming
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .generatingBorder(generating, CardShape)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val template = summary is SummaryState.Done && summary.engine == SummaryEngineId.Template
                if (!template) Spark(spinning = generating)
                Text(
                    text = when (summary) {
                        SummaryState.Loading -> stringResource(Res.string.do_summary)
                        is SummaryState.Streaming -> summary.label
                        is SummaryState.Done -> summary.label
                    }.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.55.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Keyed on loading so Streaming -> Done keeps the same text; the outgoing body keeps
            // its own state while it fades.
            AnimatedContent(
                targetState = summary,
                contentKey = { it is SummaryState.Loading },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "summaryBody",
            ) { body ->
                if (body is SummaryState.Loading) {
                    AiShimmer()
                } else {
                    Column {
                        StreamedText(
                            text = when (body) {
                                is SummaryState.Streaming -> body.text
                                is SummaryState.Done -> body.text
                                SummaryState.Loading -> ""
                            }
                        )
                        if (needsSetup && body is SummaryState.Done) {
                            AssistChip(
                                onClick = onSetUp,
                                label = { Text(stringResource(Res.string.do_set_up_ai)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                border = null,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = when (summary) {
                        SummaryState.Loading -> ""
                        is SummaryState.Streaming -> privacyLine(summary.onDevice, summary.destination ?: summary.label)
                        is SummaryState.Done -> privacyLine(summary.onDevice, summary.destination ?: summary.label)
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onRegenerate,
                    enabled = !generating,
                    label = { Text(actionLabel, fontSize = 11.sp) },
                )
            }
        }
    }
}

@Composable
private fun privacyLine(onDevice: Boolean, destination: String): String =
    if (onDevice) stringResource(Res.string.do_privacy_on_device)
    else stringResource(Res.string.do_privacy_sent, destination)

@ShowcaseComponent(
    name = "Daily Overview Summary Card",
    description = "Summary card states: loading, streaming, done, and template needing setup.",
    group = "Daily Overview"
)
@Composable
fun SummaryCardSample() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            summary = SummaryState.Loading,
            needsSetup = false,
            onRegenerate = {},
            onSetUp = {},
        )
        SummaryCard(
            summary = SummaryState.Streaming(
                text = "You finished Frieren last night with a 13-chapter run",
                label = "Gemini Nano",
                onDevice = true,
            ),
            needsSetup = false,
            onRegenerate = {},
            onSetUp = {},
        )
        SummaryCard(
            summary = SummaryState.Done(
                text = "You read 23 chapters, mostly after 7 PM, and caught up on Frieren. One Piece and Kagurabachi updated overnight.",
                label = "Google · via Koog",
                onDevice = false,
                engine = SummaryEngineId.Provider,
                destination = "Google",
            ),
            needsSetup = false,
            onRegenerate = {},
            onSetUp = {},
        )
        SummaryCard(
            summary = SummaryState.Done(
                text = "You read 23 chapters across 4 titles. 11 new chapters arrived.",
                label = "Yesterday at a glance",
                onDevice = true,
                engine = SummaryEngineId.Template,
            ),
            needsSetup = true,
            onRegenerate = {},
            onSetUp = {},
        )
    }
}
