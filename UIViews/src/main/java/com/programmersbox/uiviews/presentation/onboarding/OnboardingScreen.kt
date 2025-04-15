package com.programmersbox.uiviews.presentation.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.datastore.asState
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.rememberDrawablePainter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    appLogo: AppLogo = koinInject(),
    navController: NavController,
    dataStoreHandling: DataStoreHandling = koinInject(),
) {
    HideSystemBarsWhileOnScreen()

    var hasSeenOnboarding by dataStoreHandling
        .hasGoneThroughOnboarding
        .asState()

    LaunchedEffect(hasSeenOnboarding) {
        if (hasSeenOnboarding) {
            navController.navigate(Screen.RecentScreen) {
                popUpTo(Screen.OnboardingScreen) {
                    inclusive = true
                }
            }
        }
    }

    val appName = stringResource(R.string.app_name)

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState {
        2 + if (BuildConfig.FLAVOR == "full") 1 else 0
    }

    NormalOtakuScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Welcome!") },
                    subtitle = { Text(appName) }
                )
                HorizontalDivider()
            }
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    TextButton(
                        onClick = { hasSeenOnboarding = true }
                    ) { Text("Skip") }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = when {
                                pagerState.currentPage == iteration -> MaterialTheme.colorScheme.primary
                                pagerState.currentPage > iteration -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }

                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(16.dp)
                            )
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (pagerState.canScrollForward) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                hasSeenOnboarding = true
                            }
                        }
                    ) {
                        if (pagerState.canScrollForward) {
                            Text("Next")
                        } else {
                            Text("Finish!")
                        }
                    }
                }
            )
        }
    ) { padding ->

        //TODO: Add login screen
        // add account info screen
        // maybe even see if we can get the account created when
        // maybe add a theme screen here

        HorizontalPager(
            state = pagerState,
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) { page ->
            when (page) {
                0 -> WelcomeContent(
                    appLogo = appLogo,
                )

                //TODO: Maybe make an enum for the flavors?
                1 -> if (BuildConfig.FLAVOR == "full") {
                    AccountContent()
                } else {
                    FinishContent()
                }

                pagerState.pageCount -> FinishContent()
            }
        }
    }
}

@Composable
private fun AccountContent(
    viewModel: AccountViewModel = koinViewModel(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Log in to save all your favorites and chapters/episodes read to the cloud so you can access them on any device!")

        Text("No one who works on these apps get access to your data.")

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

    }
}

@Composable
private fun FinishContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .size(150.dp)
        ) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.successfully_done)
            )

            LottieAnimation(
                composition = composition,
                modifier = Modifier.matchParentSize()
            )
        }

        HorizontalDivider()

        Text(
            "All done! Press finish to start using the app!",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun WelcomeContent(
    appLogo: AppLogo,
) {
    val appName = stringResource(R.string.app_name)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .size(150.dp)
        ) {
            Image(
                rememberDrawablePainter(appLogo.logo),
                null,
                modifier = Modifier.matchParentSize()
            )
        }

        HorizontalDivider()

        Text(
            stringResource(R.string.welcome_description, appName),
            modifier = Modifier.padding(16.dp)
        )
    }
}