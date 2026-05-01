package com.programmersbox.kmpuiviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpuiviews.di.kmpModule
import com.programmersbox.kmpuiviews.presentation.HomeNav
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import java.awt.Cursor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ApplicationScope.BaseDesktopUi(
    title: String,
    moduleBlock: KoinApplication.() -> Unit,
) {
    //TODO: add a screen where you paste a url and select a source that then opens the details screen

    //TODO: Also need to create a generic module in kmpuiviews
    /*LaunchedEffect(Unit) {
        DataStoreSettings { File(System.getProperty("user.home"), it).absolutePath }
    }*/
    //TODO: UI Goes here!
    //UrlOpenerScreen()
    //ScanQrCode()
    /*
    val backStack = rememberNavBackStack(Screen.SettingsScreen)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Screen.SettingsScreen> {
                SettingScreen(
                    composeSettingsDsl = ComposeSettingsDsl(),
                    accountSettings = {},
                    onDebugBuild = {},
                    scanQrCode = {}
                )
            }
        }
    )*/
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                printLogger(Level.DEBUG)
                modules(
                    module {
                        includes(kmpModule)

                        singleOf<KmpFirebaseConnection>(::KmpFirebaseConnectionImpl)
                        factory<KmpFirebaseConnection.KmpFirebaseListener> { KmpFirebaseConnectionImpl.KmpFirebaseListenerImpl() }

                        singleOf(::DataStoreHandling)
                        single {
                            NewSettingsHandling(
                                createProtobuf(
                                    serializer = SettingsSerializer(),
                                    fileName = File(
                                        System.getProperty("user.home"),
                                        "Settings.preferences_pb"
                                    ).absolutePath,
                                ),
                            )
                        }

                    }
                )
                moduleBlock()
            }
        ),
        content = {
            val navigationActions = koinInject<NavigationActions>()
            val dataStoreHandling = koinInject<DataStoreHandling>()
            LaunchedEffect(Unit) {
                if (dataStoreHandling.hasGoneThroughOnboarding.getOrNull() == false) {
                    navigationActions.toOnboarding()
                }
            }

            val sourceLoader = koinInject<SourceLoader>()

            LaunchedEffect(Unit) {
                sourceLoader.blockingLoad()
            }

            val windowState = rememberWindowState()

            Window(
                onCloseRequest = ::exitApplication,
                title = title,
                state = windowState,
                undecorated = true,
                transparent = true,
            ) {
                MaterialTheme(
                    createColorScheme(
                        isSystemInDarkTheme(),
                        isExpressive = true
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            KmpLocalCompositionSetup(
                            ) {
                                CompositionLocalProvider(
                                    LocalNavHostPadding provides PaddingValues()
                                ) {
                                    CustomTitleBar(
                                        title = title,
                                        onMinimizeClick = { windowState.isMinimized = true },
                                        onCloseClick = ::exitApplication
                                    )
                                    HorizontalDivider()
                                    //UrlOpenerScreen()
                                    //ScanQrCode()
                                    val genericInfo = koinInject<KmpGenericInfo>()
                                    val customSettings = remember {
                                        ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences())
                                    }
                                    val windowSize = calculateWindowSizeClass()
                                    HomeNav(
                                        genericInfo = genericInfo,
                                        windowSize = windowSize,
                                        bottomBarAdditions = {},
                                        customPreferences = customSettings,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameWindowScope.CustomTitleBar(
    title: String,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    WindowDraggableArea {
        TopAppBar(
            title = { Text(title) },
            actions = {
                IconButton(
                    onClick = onMinimizeClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                ) {
                    Icon(
                        Icons.Default.Minimize,
                        contentDescription = "Minimize",
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            )
        )
    }
}

class KmpFirebaseConnectionImpl : KmpFirebaseConnection {
    override fun getAllShows(): List<DbModel> = emptyList()
    override fun insertShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun removeShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun updateShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> = flowOf(Unit)
    override fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> = flowOf(Unit)

    class KmpFirebaseListenerImpl : KmpFirebaseConnection.KmpFirebaseListener {
        override fun getAllShowsFlow(): Flow<List<DbModel>> = flowOf(emptyList())

        override fun getShowFlow(url: String?): Flow<DbModel?> = flowOf(null)

        override fun findItemByUrlFlow(url: String?): Flow<Boolean> = flowOf(false)

        override fun getAllEpisodesByShowFlow(showUrl: String): Flow<List<ChapterWatched>> = flowOf(emptyList())

        override fun unregister() {

        }
    }
}
