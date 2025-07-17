package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.fleeys.heatmap.HeatMap
import com.fleeys.heatmap.model.Heat
import com.fleeys.heatmap.style.DaysLabelColor
import com.fleeys.heatmap.style.DaysLabelStyle
import com.fleeys.heatmap.style.HeatColor
import com.fleeys.heatmap.style.HeatMapStyle
import com.fleeys.heatmap.style.HeatStyle
import com.fleeys.heatmap.style.LabelStyle
import com.fleeys.heatmap.style.MonthsLabelColor
import com.fleeys.heatmap.style.MonthsLabelStyle
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DateFormatItem
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    profileUrl: String?,
    appConfig: AppConfig = koinInject(),
    viewModel: AccountInfoViewModel = koinViewModel(),
) {
    val state = viewModel.accountInfo
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Info") },
                navigationIcon = { BackButton() },
                actions = {
                    ImageLoaderChoice(
                        profileUrl.orEmpty(),
                        name = "",
                        placeHolder = { rememberVectorPainter(Icons.Default.AccountCircle) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(40.dp)
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { p ->
        LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            if(state.heatMaps.isNotEmpty()) {
                item {
                    var heatItem by remember { mutableStateOf<Heat<Int>?>(null) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .animateItem()
                            .animateContentSize()
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text("Heat Map")
                        HeatMap(
                            data = state.heatMaps,
                            onHeatClick = { heatItem = it },
                            style = HeatMapStyle().copy(
                                heatStyle = HeatStyle().copy(
                                    heatColor = HeatColor().copy(
                                        activeLowestColor = Color(0xff212f57),
                                        activeHighestColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    heatShape = CircleShape,
                                ),
                                labelStyle = LabelStyle().copy(
                                    daysLabelStyle = DaysLabelStyle(
                                        color = DaysLabelColor(
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ),
                                    monthsLabelStyle = MonthsLabelStyle(
                                        color = MonthsLabelColor(
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                )
                            ),
                        )
                        heatItem?.let {
                            Text("Read/Watched ${it.data} on ${DateFormatItem.format(it.date)}")
                        }
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Total Favorites",
                            description = "The amount of total favorites",
                            amount = state.totalFavorites
                        )
                    }

                    if (appConfig.buildType == BuildType.Full) {
                        item {
                            AccountInfoItem(
                                title = "Cloud Favorites",
                                description = "The amount of favorites in the cloud",
                                amount = state.cloudFavorites
                            )
                        }
                    }

                    item {
                        AccountInfoItem(
                            title = "Local Favorites",
                            description = "The amount of favorites in the local database",
                            amount = state.localFavorites
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Local Chapters",
                            description = "The amount of chapters in the local database",
                            amount = state.chapters
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Time Spent Doing",
                            description = "The amount of time spent doing things",
                            amount = state.timeSpentDoing
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Source Count",
                            description = "The amount of sources",
                            amount = state.sourceCount
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Notifications",
                            description = "The amount of notifications saved",
                            amount = state.notifications
                        )
                    }

                    item {
                        AccountInfoItem(
                            title = "Incognito Sources",
                            description = "The amount of sources you have in incognito mode",
                            amount = state.incognitoSources
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "History",
                            description = "The amount of history items",
                            amount = state.history
                        )
                    }

                    item {
                        AccountInfoItem(
                            title = "Global Search History",
                            description = "The amount of global search history items",
                            amount = state.globalSearchHistory
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Lists",
                            description = "The amount of lists",
                            amount = state.lists
                        )
                    }

                    item {
                        AccountInfoItem(
                            title = "Items in Lists",
                            description = "The total amount of items in lists",
                            amount = state.itemsInLists
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Saved Recommendations",
                            description = "The amount of saved recommendations",
                            amount = state.savedRecommendations
                        )
                    }
                }
            }

            item {
                CategoryGroup(
                    modifier = Modifier.animateItem()
                ) {
                    item {
                        AccountInfoItem(
                            title = "Blur Hashes",
                            description = "Used to speed up loading images",
                            amount = state.blurHashes
                        )
                    }
                }
            }

            if (appConfig.buildType != BuildType.NoFirebase) {
                item {
                    CategoryGroup(
                        modifier = Modifier.animateItem()
                    ) {
                        item {
                            AccountInfoItem(
                                title = "Translation Models",
                                description = "The amount of translation models",
                                amount = state.translationModels
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoItem(
    title: String,
    description: String,
    amount: Int,
    modifier: Modifier = Modifier,
) = ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(description) },
    trailingContent = { Text(animateIntAsState(amount).value.toString()) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
)

@Composable
private fun AccountInfoItem(
    title: String,
    description: String,
    amount: String,
    modifier: Modifier = Modifier,
) = ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(description) },
    trailingContent = { Text(amount) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
)