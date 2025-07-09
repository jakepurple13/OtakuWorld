package com.programmersbox.uiviews.presentation.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.PreviewThemeColorsSizes
import com.skydoves.landscapist.glide.GlideImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@PreviewThemeColorsSizes
@Composable
private fun SettingsPreview() {
    PreviewTheme {
        SettingScreen(
            composeSettingsDsl = ComposeSettingsDsl(),
            notificationClick = {},
            favoritesClick = {},
            historyClick = {},
            globalSearchClick = {},
            listClick = {},
            extensionClick = {},
            notificationSettingsClick = {},
            generalClick = {},
            otherClick = {},
            moreInfoClick = {},
            moreSettingsClick = {},
            geminiClick = {},
            sourcesOrderClick = {},
            appDownloadsClick = {},
            accountSettings = {},
            scanQrCode = {}
        )
    }
}

@Composable
fun AccountSettings(
    viewModel: AccountViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val navController = LocalNavActions.current

    val accountInfo = viewModel.accountInfo

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.done)) } },
            icon = {
                GlideImage(
                    imageModel = { accountInfo?.photoUrl },
                    loading = { Icon(Icons.Default.AccountCircle, null) },
                    failure = { Icon(Icons.Default.AccountCircle, null) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp)
                        .clickable { showDialog = true }
                )
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.account_category_title))
                    Text(accountInfo?.displayName ?: "User")
                }
            },
            text = {
                CategoryGroup {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(if (accountInfo != null) R.string.logOut else R.string.logIn)) },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                showDialog = false
                                (activity as? ComponentActivity)?.let { viewModel.signInOrOut(context, it) }
                            }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text("View Account Info") },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                showDialog = false
                                navController.accountInfo()
                            }
                        )
                    }
                }
            }
        )
    }

    GlideImage(
        imageModel = { accountInfo?.photoUrl },
        loading = { Icon(Icons.Default.AccountCircle, null) },
        failure = { Icon(Icons.Default.AccountCircle, null) },
        modifier = Modifier
            .clip(CircleShape)
            .size(40.dp)
            .clickable { showDialog = true }
    )
}
