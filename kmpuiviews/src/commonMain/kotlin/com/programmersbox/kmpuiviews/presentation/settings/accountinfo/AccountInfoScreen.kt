package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
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
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
    ) { p ->
        LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            accountInfoItem(
                title = "Total Favorites",
                description = "The amount of total favorites",
                amount = state.totalFavorites
            )

            if (appConfig.buildType == BuildType.Full) {
                accountInfoItem(
                    title = "Cloud Favorites",
                    description = "The amount of favorites in the cloud",
                    amount = state.cloudFavorites
                )
            }

            accountInfoItem(
                title = "Local Favorites",
                description = "The amount of favorites in the local database",
                amount = state.localFavorites
            )

            accountInfoItem(
                title = "Local Chapters",
                description = "The amount of chapters in the local database",
                amount = state.chapters
            )

            accountInfoItem(
                title = "Source Count",
                description = "The amount of sources",
                amount = state.sourceCount
            )

            accountInfoItem(
                title = "Notifications",
                description = "The amount of notifications saved",
                amount = state.notifications
            )

            accountInfoItem(
                title = "Incognito Sources",
                description = "The amount of sources you have in incognito mode",
                amount = state.incognitoSources
            )

            accountInfoItem(
                title = "History",
                description = "The amount of history items",
                amount = state.history
            )

            accountInfoItem(
                title = "Global Search History",
                description = "The amount of global search history items",
                amount = state.globalSearchHistory
            )

            accountInfoItem(
                title = "Lists",
                description = "The amount of lists",
                amount = state.lists
            )

            accountInfoItem(
                title = "Items in Lists",
                description = "The total amount of items in lists",
                amount = state.itemsInLists
            )

            accountInfoItem(
                title = "Blur Hashes",
                description = "Used to speed up loading images",
                amount = state.blurHashes
            )

            if (appConfig.buildType != BuildType.NoFirebase) {
                accountInfoItem(
                    title = "Translation Models",
                    description = "The amount of translation models",
                    amount = state.translationModels
                )
            }
        }
    }
}

private fun LazyListScope.accountInfoItem(
    title: String,
    description: String,
    amount: Int,
    modifier: Modifier = Modifier,
) = item {
    OutlinedCard(
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(description) },
            trailingContent = { Text(animateIntAsState(amount).value.toString()) },
        )
    }
}