package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard2
import com.programmersbox.kmpuiviews.utils.DateTimeFormatScreensaverItem
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryFeedScreen(
    viewModel: HistoryFeedViewModel = koinViewModel(),
) {
    val recentlyViewed by viewModel.recentlyViewed.collectAsStateWithLifecycle()
    val physicalOrientation by rememberPhysicalDeviceOrientation()

    Scaffold { padding ->
        SensorRotatedLayout(
            physicalOrientation = physicalOrientation,
            modifier = Modifier.padding(padding)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                DateBatteryCard()

                HistoryFeedList(
                    list = recentlyViewed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HistoryFeedList(
    list: List<RecentModel>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    SlowScroll(
        listState = listState,
        animateScrollToItem = { listState.animateScrollToItem(it) }
    )

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    "Recently Viewed",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (list.isEmpty()) {
                item { Text("Nothing viewed yet") }
            }

            items(list) { recent -> HistoryFeedRow(recent) }
        }
    }
}

@Composable
private fun HistoryFeedRow(recent: RecentModel) {
    val dateTimeFormatHandler: DateTimeFormatHandler = koinInject()
    val is24Time = dateTimeFormatHandler.is24Time()
    val dateFormat = remember(is24Time) {
        DateTimeFormatScreensaverItem(is24Time)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        M3CoverCard2(
            imageUrl = recent.imageUrl,
            name = recent.title,
            placeHolder = { rememberVectorPainter(Icons.Default.Settings) },
        )

        Column {
            Text(
                recent.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                dateFormat.format(recent.timestamp.toLocalDateTime()),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
