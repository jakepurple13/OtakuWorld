package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DateFormatItem
import com.programmersbox.kmpuiviews.utils.HeatMapWrapper
import com.programmersbox.kmpuiviews.utils.KmpHeat
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
                scrollBehavior = scrollBehavior,
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
            item(
                contentType = "profile",
                key = "profile",
            ) {
                ProfileStripCard(
                    profileUrl = profileUrl.orEmpty(),
                    appConfig = appConfig,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item(
                contentType = "hero",
                key = "hero",
            ) {
                HeroChipsRow(
                    favorites = state.totalFavorites,
                    chapters = state.chapters,
                    timeSpent = state.timeSpentDoing,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            if (state.heatMaps.isNotEmpty()) {
                item(
                    contentType = "heatMap",
                    key = "heatMap",
                ) {
                    var heatItem by remember { mutableStateOf<KmpHeat<Int>?>(null) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth(),
                    ) {
                        SectionHeader("🕐 Activity")
                        CategoryGroup {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .animateContentSize()
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    HeatMapWrapper(
                                        data = state.heatMaps,
                                        onHeatClick = { heatItem = it },
                                    )
                                    heatItem?.let {
                                        Text(
                                            "Read/Watched ${it.data} on ${DateFormatItem.format(it.date)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(
                contentType = "collection",
                key = "collection",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                ) {
                    SectionHeader("⭐ Collection")
                    CategoryGroup {
                        if (appConfig.buildType == BuildType.Full) {
                            item {
                                AccountInfoItem(
                                    title = "Cloud Favorites",
                                    description = "Synced to cloud",
                                    amount = state.cloudFavorites,
                                )
                            }
                        }
                        item {
                            AccountInfoItem(
                                title = "Local Favorites",
                                description = "Stored on device",
                                amount = state.localFavorites,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Notifications",
                                description = "Pending update notifications",
                                amount = state.notifications,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Incognito Sources",
                                description = "Sources browsed privately",
                                amount = state.incognitoSources,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Bookmarks",
                                description = "Chapters or Episodes bookmarked",
                                amount = state.bookmarkCount,
                            )
                        }
                    }
                }
            }

            item(
                contentType = "readingList",
                key = "readingList",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                ) {
                    SectionHeader("🔍 Discovery")
                    CategoryGroup {
                        item {
                            AccountInfoItem(
                                title = "Sources",
                                description = "Installed extensions",
                                amount = state.sourceCount,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Search History",
                                description = "Recent searches",
                                amount = state.history,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Global Search History",
                                description = "Cross-source searches",
                                amount = state.globalSearchHistory,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Saved Recommendations",
                                description = "Suggested titles saved",
                                amount = state.savedRecommendations,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Lists",
                                description = "${state.itemsInLists} items total",
                                amount = state.lists,
                            )
                        }
                    }
                }
            }

            item(
                contentType = "system",
                key = "system",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                ) {
                    SectionHeader("⚙️ System")
                    CategoryGroup {
                        item {
                            AccountInfoItem(
                                title = "Blur Hash Cache",
                                description = "Speeds up image loading",
                                amount = state.blurHashes,
                            )
                        }
                        if (appConfig.buildType != BuildType.NoFirebase) {
                            item {
                                AccountInfoItem(
                                    title = "Translation Models",
                                    description = "Downloaded language models",
                                    amount = state.translationModels,
                                )
                            }
                        }
                        item {
                            AccountInfoItem(
                                title = "Logged Exceptions",
                                description = "Errors captured by the app",
                                amount = state.exceptionCount,
                                valueColor = if (state.exceptionCount > 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    Color.Unspecified,
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
    valueColor: Color = Color.Unspecified,
) = ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(description) },
    trailingContent = {
        Text(
            text = animateIntAsState(amount, label = "accountInfoItemAmount").value.toString(),
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.primary else valueColor,
        )
    },
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

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun ProfileStripCard(
    profileUrl: String,
    appConfig: AppConfig,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(primaryColor, tertiaryColor)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ImageLoaderChoice(
                    profileUrl,
                    name = "",
                    placeHolder = { rememberVectorPainter(Icons.Default.AccountCircle) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f), CircleShape),
                )
                Column {
                    Text(
                        text = appConfig.userName ?: appConfig.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "${appConfig.appName} member",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroChipsRow(
    favorites: Int,
    chapters: Int,
    timeSpent: String,
    modifier: Modifier = Modifier,
) {
    val animatedFavorites by animateIntAsState(favorites, label = "heroFavorites")
    val animatedChapters by animateIntAsState(chapters, label = "heroChapters")
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        maxItemsInEachRow = 2,
    ) {
        HeroStatChip(
            label = "Favorites",
            value = animatedFavorites.toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        HeroStatChip(
            label = "Chapters",
            value = animatedChapters.toString(),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        HeroStatChip(
            label = "Time",
            value = timeSpent,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}