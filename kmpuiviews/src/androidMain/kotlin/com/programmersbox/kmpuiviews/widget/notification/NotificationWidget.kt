package com.programmersbox.kmpuiviews.widget.notification

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceCount
import com.programmersbox.kmpuiviews.widget.WidgetTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationWidget : GlanceAppWidget(), KoinComponent {
    private val itemDao by inject<ItemDao>()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by getWidgetStateFlow(itemDao).collectAsState(WidgetDataState())

            WidgetTheme {
                TextOnlyWidgetContent(
                    state = state
                )
            }
        }
    }
}

fun getWidgetStateFlow(
    itemDao: ItemDao,
): Flow<WidgetDataState> {
    return combine(
        itemDao.getTotalCountFlow(),
        itemDao.getTopSourcesFlow()
    ) { total, sources ->
        WidgetDataState(totalCount = total, topSources = sources)
    }
}

data class WidgetDataState(
    val totalCount: Int = 0,
    val topSources: List<SourceCount> = emptyList(),
)

@Composable
private fun TextOnlyWidgetContent(state: WidgetDataState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .appWidgetBackground()
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED ITEMS",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = state.totalCount.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Render the Dynamic 2x2 Grid
        val sources = state.topSources
        LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
            items(sources.chunked(2)) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    SourceItemSafe(source = sources.getOrNull(0), modifier = GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    SourceItemSafe(source = sources.getOrNull(1), modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

// A safe wrapper that renders an empty space if the source doesn't exist
// (e.g., if the user only has 1 or 3 sources total)
@Composable
private fun SourceItemSafe(source: SourceCount?, modifier: GlanceModifier) {
    if (source == null) {
        Spacer(modifier = modifier)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(GlanceTheme.colors.primaryContainer)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            Text(
                text = source.source,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                ),
                maxLines = 1, // Prevents long source names from breaking the layout
                // 1. Move defaultWeight() here so the text truncates instead of expanding
                modifier = GlanceModifier.defaultWeight()
            )

            // 2. Change the spacer to a fixed width so there is always a small gap
            // between a long name and the count.
            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = source.count.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                ),
            )
        }
    }
}