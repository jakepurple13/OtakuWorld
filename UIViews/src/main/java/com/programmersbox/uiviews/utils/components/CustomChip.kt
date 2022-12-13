package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CustomChip(
    category: String,
    modifier: Modifier = Modifier,
    textColor: Color = androidx.compose.material.MaterialTheme.colors.onSurface,
    backgroundColor: Color = androidx.compose.material.MaterialTheme.colors.surface
) {
    androidx.compose.material.Surface(
        modifier = Modifier.then(modifier),
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row {
            androidx.compose.material.Text(
                text = category,
                style = androidx.compose.material.MaterialTheme.typography.body2,
                color = textColor,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterVertically),
                textAlign = TextAlign.Center
            )
        }
    }
}
