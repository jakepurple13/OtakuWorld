package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

class GroupButtonModel<T>(val item: T, val iconContent: @Composable () -> Unit)

@Composable
fun <T> GroupButton(
    selected: T,
    options: List<GroupButtonModel<T>>,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.inversePrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (T) -> Unit
) {
    Row(modifier) {
        val smallShape = RoundedCornerShape(20.0.dp)
        val noCorner = CornerSize(0.dp)

        options.fastForEachIndexed { i, option ->
            OutlinedButton(
                modifier = Modifier,
                onClick = { onClick(option.item) },
                shape = smallShape.copy(
                    topStart = if (i == 0) smallShape.topStart else noCorner,
                    topEnd = if (i == options.size - 1) smallShape.topEnd else noCorner,
                    bottomStart = if (i == 0) smallShape.bottomStart else noCorner,
                    bottomEnd = if (i == options.size - 1) smallShape.bottomEnd else noCorner
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = animateColorAsState(if (selected == option.item) selectedColor else unselectedColor).value
                )
            ) { option.iconContent() }
        }
    }
}