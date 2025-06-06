package com.programmersbox.uiviews.presentation.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.skydoves.landscapist.glide.GlideImage
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    navController: NavigationActions,
    customPreferences: ComposeSettingsDsl,
    dataStoreHandling: DataStoreHandling = koinInject(),
) {
    OnboardingScreen(
        navController = navController,
        customPreferences = customPreferences,
        accountContent = {
            val viewModel: AccountViewModel = koinViewModel()
            Crossfade(
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
            }
        },
        appConfig = koinInject(),
    )
}