package com.programmersbox.kmpuiviews.widget.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.favoritesdatabase.SourceCount

@Composable
fun TextOnlyWidgetContentPreview(
    state: WidgetDataState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clip(MaterialTheme.shapes.extraLarge)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED ITEMS",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = state.totalCount.toString(),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Render the Dynamic 2x2 Grid
        val sources = state.topSources
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(sources.chunked(2)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    SourceItemSafe(source = sources.getOrNull(0), modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    SourceItemSafe(source = sources.getOrNull(1), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// A safe wrapper that renders an empty space if the source doesn't exist
// (e.g., if the user only has 1 or 3 sources total)
@Composable
private fun SourceItemSafe(source: SourceCount?, modifier: Modifier) {
    if (source == null) {
        Spacer(modifier = modifier)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp)
        ) {
            Text(
                text = source.source,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                maxLines = 1, // Prevents long source names from breaking the layout
                // 1. Move defaultWeight() here so the text truncates instead of expanding
                modifier = Modifier.weight(1f)
            )

            // 2. Change the spacer to a fixed width so there is always a small gap
            // between a long name and the count.
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = source.count.toString(),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                ),
            )
        }
    }
}