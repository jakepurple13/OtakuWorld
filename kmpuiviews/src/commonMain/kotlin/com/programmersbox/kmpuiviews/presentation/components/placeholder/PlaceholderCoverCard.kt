package com.programmersbox.kmpuiviews.presentation.components.placeholder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.utils.ComposableUtils

@Composable
fun M3PlaceHolderCoverCard(placeHolder: Painter, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(
            ComposableUtils.IMAGE_WIDTH,
            ComposableUtils.IMAGE_HEIGHT
        ),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box {
            Image(
                painter = placeHolder,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .m3placeholder(
                        true,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "",
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .m3placeholder(
                            true,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}