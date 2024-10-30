package com.programmersbox.uiviews.utils.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTipWrapper(
    info: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tooltipState: TooltipState = rememberTooltipState(),
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { RichTooltip { info() } },
        state = tooltipState,
        modifier = modifier,
        content = content
    )
}