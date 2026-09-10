package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard2
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomListRotationScreen(
    viewModel: CustomListRotationViewModel = koinViewModel(),
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val physicalOrientation by rememberPhysicalDeviceOrientation()
    val currentList = lists.getOrNull(viewModel.currentIndex)

    Scaffold { _ ->
        SensorRotatedLayout(physicalOrientation = physicalOrientation) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                DateBatteryCard()

                AnimatedContent(
                    targetState = currentList,
                    modifier = Modifier.weight(1f)
                ) { list ->
                    if (list != null) {
                        CustomListGrid(list = list)
                    } else {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No custom lists yet", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomListGrid(list: CustomList) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    list.item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(list.list) { info ->
                M3CoverCard2(
                    imageUrl = info.imageUrl,
                    name = info.title,
                    placeHolder = { rememberVectorPainter(Icons.Default.Settings) },
                )
            }
        }
    }
}
