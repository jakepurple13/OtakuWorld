package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StatsDashboardScreen(
    viewModel: StatsDashboardViewModel = koinViewModel(),
) {
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

                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text("Stats", style = MaterialTheme.typography.headlineSmall)
                        StatRow("Time Spent", viewModel.activity)
                        StatRow("Favorites", viewModel.favoritesCount.toString())
                        StatRow("Recently Viewed", viewModel.recentHistoryCount.toString())
                        StatRow("Custom Lists", viewModel.customListsCount.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
