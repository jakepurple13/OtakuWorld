package com.programmersbox.uiviews.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp


@Composable
fun MaterialCard(
    modifier: Modifier = Modifier,
    headerOnTop: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    header: (@Composable ColumnScope.() -> Unit)? = null,
    media: (@Composable ColumnScope.() -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        border = border,
        elevation = elevation
    ) {
        Column {
            if (headerOnTop) header?.invoke(this)
            media?.invoke(this)
            if (!headerOnTop) header?.invoke(this)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                supportingText?.let {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        it.invoke()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { actions?.invoke(this) }
        }
    }
}