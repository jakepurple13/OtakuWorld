package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsScreenDisplayNames
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchViewModel
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.versionCode
import com.programmersbox.supabaseintegration.ui.SyncIconComposable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentVersion
import otakuworld.kmpuiviews.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingScreen(
    composeSettingsDsl: ComposeSettingsDsl,
    navigationActions: NavigationActions = LocalNavActions.current,
    accountSettings: @Composable () -> Unit = {},
) {
    val viewModelStoreOwner = rememberViewModelStoreOwner()
    val viewModel = koinViewModel<SettingsSearchViewModel>(
        viewModelStoreOwner = viewModelStoreOwner
    )
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    val appVersion = appVersion()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = viewModel.textFieldState,
            onSearch = {},
            placeholder = { Text(stringResource(Res.string.settings)) },
            leadingIcon = {
                IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
        )
    }

    Box {
        OtakuScaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(Res.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { searchBarState.animateToExpanded() } }) {
                            Icon(Icons.Default.Search, null)
                        }
                    },
                    actions = {
                        SyncIconComposable(modifier = Modifier.padding(horizontal = 16.dp))
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { p ->
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(p)
            ) {
                // Quick Actions
                CategoryGroupListItem {
                    segmentedListItem(
                        content = { Text("Scan QR Code") },
                        leadingContent = { Icon(Icons.Default.QrCodeScanner, null) },
                        onClick = navigationActions::scanQrCode,
                    )
                    segmentedListItem(
                        content = { Text("Favorites") },
                        leadingContent = { Icon(Icons.Default.Star, null) },
                        onClick = navigationActions::favorites,
                    )
                    segmentedListItem(
                        content = { Text("Saved Notifications") },
                        leadingContent = { Icon(Icons.Default.Notifications, null) },
                        onClick = navigationActions::notifications,
                    )
                    segmentedListItem(
                        content = { Text("Global Search") },
                        leadingContent = { Icon(Icons.Default.Search, null) },
                        onClick = { navigationActions.globalSearch() },
                    )
                    apply(composeSettingsDsl.quickActionsSettings)
                }

                // App-level viewSettings injection
                CategoryGroupListItem {
                    apply(composeSettingsDsl.viewSettings)
                }

                // Main setting categories
                CategoryGroupListItem {
                    segmentedListItem(
                        content = { Text("Library") },
                        leadingContent = { Icon(Icons.Default.Star, null) },
                        onClick = navigationActions::library,
                    )
                    segmentedListItem(
                        content = { Text("Discover") },
                        leadingContent = { Icon(Icons.Default.Widgets, null) },
                        onClick = navigationActions::discover,
                    )
                    segmentedListItem(
                        content = { Text("Sources & Extensions") },
                        leadingContent = { Icon(Icons.Default.Source, null) },
                        onClick = navigationActions::sources,
                    )
                    segmentedListItem(
                        content = { Text("Integrations") },
                        leadingContent = { Icon(Icons.Default.DataObject, null) },
                        onClick = navigationActions::integrations,
                    )
                    segmentedListItem(
                        content = { Text("Appearance") },
                        leadingContent = { Icon(Icons.Default.Palette, null) },
                        onClick = navigationActions::appearance,
                    )
                    segmentedListItem(
                        content = { Text("Behavior") },
                        leadingContent = { Icon(Icons.Default.Settings, null) },
                        onClick = navigationActions::behaviorSettings,
                    )
                    segmentedListItem(
                        content = { Text("Data Management") },
                        leadingContent = { Icon(Icons.Default.Storage, null) },
                        onClick = navigationActions::dataManagement,
                    )
                    segmentedListItem(
                        content = { Text("About") },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        onClick = navigationActions::aboutSettings,
                    )

                    segmentedListItem(
                        leadingContent = {
                            Image(
                                painterLogo(),
                                null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        },
                        overlineContent = { Text(platform()) },
                        content = { Text(stringResource(Res.string.currentVersion, appVersion)) },
                        supportingContent = { Text("Version code: ${versionCode()}") },
                        onClick = { navigationActions.navigate(Screen.Settings.AppInfo) },
                    )
                }

                accountSettings()
            }
        }

        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField,
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(viewModel.searchResults) { item ->
                    val crumb = SettingsScreenDisplayNames.breadcrumbText(item.breadcrumb)
                    ListItem(
                        content = { Text(item.displayName) },
                        supportingContent = { Text(crumb) },
                        leadingContent = { Icon(Icons.Default.Search, null) },
                        onClick = {
                            scope.launch { searchBarState.animateToCollapsed() }
                            navigationActions.navigate(item.targetScreen)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
