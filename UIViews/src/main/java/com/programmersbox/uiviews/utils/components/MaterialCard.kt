package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@ExperimentalMaterialApi
@Composable
fun MaterialCard(
    modifier: Modifier = Modifier,
    headerOnTop: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    media: (@Composable ColumnScope.() -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        border = border,
        elevation = elevation
    ) {
        Column {
            if (headerOnTop) header?.invoke(this)
            media?.invoke(this)
            if (!headerOnTop) header?.invoke(this)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                supportingText?.let {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        ProvideTextStyle(MaterialTheme.typography.body2) {
                            it.invoke()
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { actions?.invoke(this) }
        }
    }
}