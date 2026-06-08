package com.programmersbox.koogintegration.screens.chatscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.koogintegration.AppDimension

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LLMCallMessageItem(data: LlmCallData) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val formattedMessages = remember(data.messageHistory) { data.messageHistory.map(::formatLlmCallMessage) }
    val formattedTools = remember(data.availableTools) { data.availableTools.map(::formatAvailableTool) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val toolNameTextStyle = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxContentWidth = maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        val toolNameCellWidth = remember(formattedMessages, maxContentWidth, density, toolNameTextStyle) {
            calculateToolNameCellWidth(formattedMessages, textMeasurer, toolNameTextStyle, density, maxContentWidth)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(AppDimension.messageTitleColumnWidth),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Request\nto LLM",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = AppDimension.spacingExtraSmall)
                )
            }
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            Column(
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .clip(RoundedCornerShape(AppDimension.radiusMedium))
                    .border(1.dp, borderColor, RoundedCornerShape(AppDimension.radiusMedium))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(AppDimension.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingExtraSmall)
            ) {
                if (formattedMessages.isNotEmpty()) {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                    formattedMessages.forEach { message ->
                        LlmCallMessageRow(
                            message = message,
                            borderColor = borderColor,
                            toolNameCellWidth = toolNameCellWidth
                        )
                    }
                }

                if (formattedTools.isNotEmpty()) {
                    if (formattedMessages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(AppDimension.spacingExtraSmall))
                    }
                    Text(
                        text = "Available tools",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall),
                        verticalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
                    ) {
                        formattedTools.forEach { tool ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(AppDimension.radiusSmall))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    .border(1.dp, borderColor, RoundedCornerShape(AppDimension.radiusSmall))
                                    .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tool,
                                    color = MaterialTheme.colorScheme.outline,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateToolNameCellWidth(
    formattedMessages: List<FormattedLlmCallMessage>,
    textMeasurer: TextMeasurer,
    toolNameTextStyle: TextStyle,
    density: Density,
    maxBubbleWidth: Dp,
): Dp {
    val toolNames = formattedMessages.mapNotNull { it.toolName }
    if (toolNames.isEmpty()) {
        return 110.dp
    }
    val maxMeasuredWidth = toolNames.maxOf { toolName ->
        textMeasurer.measure(
            text = toolName,
            style = toolNameTextStyle
        ).size.width
    }
    return with(density) {
        (maxMeasuredWidth.toDp() + AppDimension.spacingSmall * 2).coerceAtMost(maxBubbleWidth * 0.3f)
    }
}

@Composable
private fun LlmCallMessageRow(
    message: FormattedLlmCallMessage,
    borderColor: Color,
    toolNameCellWidth: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(AppDimension.radiusSmall))
            .border(1.dp, borderColor, RoundedCornerShape(AppDimension.radiusSmall))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LlmCallCell(
            text = message.type,
            modifier = Modifier.width(92.dp),
            centered = true,
            monospace = true,
            bold = true,
        )
        LlmCallCellDivider(borderColor)
        message.toolName?.let {
            LlmCallCell(
                text = it,
                modifier = Modifier.width(toolNameCellWidth),
                monospace = true,
                bold = true,
            )
            LlmCallCellDivider(borderColor)
        }
        LlmCallCell(
            text = message.content,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RowScope.LlmCallCell(
    text: String,
    modifier: Modifier,
    centered: Boolean = false,
    monospace: Boolean = false,
    bold: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = AppDimension.spacingSmall, vertical = 8.dp),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = if (monospace) FontFamily.Monospace else null,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LlmCallCellDivider(borderColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(borderColor.copy(alpha = 0.7f))
    )
}

private fun formatLlmCallMessage(message: LlmCallHistoryItem): FormattedLlmCallMessage {
    return when (message) {
        is LlmCallHistoryItem.System -> FormattedLlmCallMessage(
            type = "System",
            content = message.text.firstLineWithEllipsis()
        )

        is LlmCallHistoryItem.User -> FormattedLlmCallMessage(
            type = "User",
            content = message.text
        )

        is LlmCallHistoryItem.Assistant -> FormattedLlmCallMessage(
            type = "Assistant",
            content = message.text
        )

        is LlmCallHistoryItem.Reasoning -> FormattedLlmCallMessage(
            type = "Reasoning",
            content = message.text
        )

        is LlmCallHistoryItem.ToolCall -> FormattedLlmCallMessage(
            type = "ToolCall",
            toolName = message.toolName,
            content = message.text
        )

        is LlmCallHistoryItem.ToolResult -> FormattedLlmCallMessage(
            type = "Result",
            toolName = message.toolName,
            content = message.text
        )
    }
}

private fun formatAvailableTool(tool: LlmCallToolData): String {
    val parameters = tool.requiredParameters + tool.optionalParameters
    return if (parameters.isEmpty()) tool.name else "${tool.name}(${parameters.joinToString(", ")})"
}

private fun String.firstLineWithEllipsis(): String {
    val firstLine = lineSequence().firstOrNull()?.trim().orEmpty()
    return if (contains('\n') && firstLine.isNotEmpty()) "$firstLine ..." else firstLine
}

private data class FormattedLlmCallMessage(
    val type: String,
    val toolName: String? = null,
    val content: String,
)
