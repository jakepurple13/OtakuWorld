package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImageFlushListItem(
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: (@Composable () -> Unit)? = null,
    headlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(modifier) {
        Box(Modifier.align(Alignment.CenterVertically)) { leadingContent() }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 4.dp)
        ) {
            overlineContent?.let { ProvideTextStyle(value = MaterialTheme.typography.labelMedium, content = it) }
            headlineContent?.let { ProvideTextStyle(value = MaterialTheme.typography.titleSmall, content = it) }
            supportingContent?.let { ProvideTextStyle(value = MaterialTheme.typography.bodyMedium, content = it) }
        }

        trailingContent?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(horizontal = 2.dp)
            ) { it.invoke() }
        }
    }
}