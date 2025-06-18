package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.kmpuiviews.utils.AppConfig
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.import_favorites

@Composable
internal fun AccountContent(
    navController: NavigationActions,
    cloudContent: @Composable () -> Unit,
    importViewModel: MoreSettingsViewModel = koinViewModel(),
    appConfig: AppConfig = koinInject(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Account") }
        )

        HorizontalDivider()

        if (appConfig.buildType == BuildType.Full) {
            Text(
                "Log in to save all your favorites and chapters/episodes read to the cloud so you can access them on any device!",
                modifier = Modifier.padding(16.dp)
            )

            Text(
                "No one who works on these apps get access to your data.",
                modifier = Modifier.padding(16.dp)
            )

            cloudContent()
            /*Crossfade(
                viewModel.accountInfo
            ) { target ->
                if (target == null) {
                    val context = LocalContext.current
                    val activity = LocalActivity.current

                    Card(
                        onClick = {
                            (activity as? ComponentActivity)?.let {
                                viewModel.signInOrOut(context, it)
                            }
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text("Log in") },
                            leadingContent = { Icon(Icons.Default.AccountCircle, null) }
                        )
                    }
                } else {
                    Card {
                        ListItem(
                            headlineContent = { Text(target.displayName.orEmpty()) },
                            leadingContent = {
                                GlideImage(
                                    imageModel = { target.photoUrl },
                                    loading = { Icon(Icons.Default.AccountCircle, null) },
                                    failure = { Icon(Icons.Default.AccountCircle, null) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(40.dp)
                                )
                            }
                        )
                    }
                }
            }*/
        } else {
            val importLauncher = rememberFilePickerLauncher(
                type = FileKitType.File("json")
            ) { document -> document?.let { importViewModel.importFavorites(it) } }

            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.import_favorites)) },
                settingIcon = { Icon(Icons.Default.Favorite, null) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { importLauncher.launch() }
            )
        }

        HorizontalDivider()

        val importListLauncher = rememberFilePickerLauncher(
            type = FileKitType.File("json")
        ) { document -> document?.let { navController.importFullList(it.toString()) } }

        PreferenceSetting(
            settingTitle = { Text("Import Lists") },
            settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
            modifier = Modifier.clickable(
                enabled = true,
                indication = ripple(),
                interactionSource = null
            ) { importListLauncher.launch() }
        )
    }
}