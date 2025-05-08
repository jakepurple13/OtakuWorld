package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    hasCloudBackup: Boolean,
    profileUrl: String?,
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
                        modifier = Modifier.size(40.dp),
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
            item {
                AccountInfoItem(
                    "Total Favorites",
                    state.totalFavorites
                )
            }

            if (hasCloudBackup) {
                item {
                    AccountInfoItem(
                        "Cloud Favorites",
                        state.cloudFavorites
                    )
                }
            }

            item {
                AccountInfoItem(
                    "Local Favorites",
                    state.localFavorites
                )
            }

            item {
                AccountInfoItem(
                    "Notifications",
                    state.notifications
                )
            }

            item {
                AccountInfoItem(
                    "Incognito Sources",
                    state.incognitoSources
                )
            }

            item {
                AccountInfoItem(
                    "History",
                    state.history
                )
            }

            item {
                AccountInfoItem(
                    "Lists",
                    state.lists
                )
            }
        }
    }
}

@Composable
private fun AccountInfoItem(
    title: String,
    amount: Int,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(title) },
            trailingContent = { Text(animateIntAsState(amount).value.toString()) },
        )
    }
}