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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.di.appModule
import com.programmersbox.kmpuiviews.di.databases
import com.programmersbox.kmpuiviews.di.repositories
import com.programmersbox.kmpuiviews.di.viewModels
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navGraph
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.awt.Cursor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
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
    KoinApplication(
        application = {
            printLogger(Level.DEBUG)
            modules(
                module {
                    includes(
                        appModule,
                        viewModels,
                        repositories,
                        databases,
                    )

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

                    moduleBlock()
                }
            )
        }
    ) {
        val windowState = rememberWindowState()

        Window(
            onCloseRequest = ::exitApplication,
            title = title,
            state = windowState,
            undecorated = true,
            transparent = true,
        ) {
            val navController = rememberNavController()
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
                            navController,
                            remember { mutableStateListOf() }
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
                                val genericInfo = koinInject<KmpGenericInfo>()
                                NavHost(
                                    navController = navController,
                                    startDestination = Screen.Settings
                                ) {
                                    navGraph(
                                        customPreferences = ComposeSettingsDsl(),
                                        genericInfo = genericInfo,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/*@Composable
fun <T : NavKey> rememberNavBackStack(vararg elements: T): SnapshotStateList<NavKey> {
    return rememberSaveable {
        elements.toList().toMutableStateList()
    }
}*/

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
