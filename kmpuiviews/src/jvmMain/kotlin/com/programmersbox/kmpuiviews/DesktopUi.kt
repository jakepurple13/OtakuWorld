package com.programmersbox.kmpuiviews

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.jsextensionloader.ExtensionDiscovery
import com.programmersbox.jsextensionloader.JSExtensionLoader
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.di.kmpModule
import com.programmersbox.kmpuiviews.presentation.HomeNav
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridge
import com.programmersbox.kmpuiviews.repository.SetupRepository
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.bindsPlatformGenericInfo
import com.programmersbox.supabaseintegration.Res
import com.programmersbox.supabaseintegration.supabase_logo_icon
import dev.nucleusframework.application.NucleusApplicationScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.definition.KoinDefinition
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import java.awt.Cursor
import java.io.File

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

fun desktopSetup(
    args: Array<String>,
    name: String,
    appDirs: AppDirs,
    appConfig: Scope.() -> AppConfig,
    jvmAppLogo: Scope.() -> JvmAppLogo,
    genericInfo: Module.() -> KoinDefinition<PlatformGenericInfo>,
    moduleBlock: Module.() -> Unit,
) {
    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    val desktopViewModelStoreOwner = DesktopViewModelStoreOwner()
    application {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides desktopViewModelStoreOwner
        ) {
            BaseDesktopUi(
                title = name,
                moduleBlock = {
                    modules(
                        module {
                            single { appConfig() }
                            single { jvmAppLogo() }
                            genericInfo(this).bindsPlatformGenericInfo()
                            moduleBlock()
                        }
                    )
                }
            )
        }
    }
}

fun baseDesktopSetup(
    args: Array<String>,
    name: String,
    appDirs: AppDirs,
    appConfig: Scope.() -> AppConfig,
    jvmAppLogo: Scope.() -> JvmAppLogo = { JvmAppLogo(Res.drawable.supabase_logo_icon) },
    genericInfo: Module.() -> KoinDefinition<PlatformGenericInfo>,
    moduleBlock: Module.() -> Unit,
    content: @Composable () -> Unit,
) {
    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    val desktopViewModelStoreOwner = DesktopViewModelStoreOwner()
    application {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides desktopViewModelStoreOwner
        ) {
            InternalBaseDesktopUi(
                title = name,
                moduleBlock = {
                    modules(
                        module {
                            single { appConfig() }
                            single { jvmAppLogo() }
                            genericInfo(this).bindsPlatformGenericInfo()
                            moduleBlock()
                        }
                    )
                },
                tray = {},
                exitApplication = ::exitApplication,
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ApplicationScope.BaseDesktopUi(
    title: String,
    moduleBlock: KoinApplication.() -> Unit,
) {
    InternalDesktopUi(
        title = title,
        tray = {
            Tray(
                state = koinInject<TrayState>(),
                icon = painterLogo(),
                tooltip = koinInject<AppConfig>().appName,
                menu = {}
            )
        },
        exitApplication = ::exitApplication,
        moduleBlock = moduleBlock
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NucleusApplicationScope.BaseDesktopUi(
    title: String,
    moduleBlock: KoinApplication.() -> Unit,
) {
    InternalDesktopUi(
        title = title,
        tray = {},
        exitApplication = ::exitApplication,
        moduleBlock = moduleBlock
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun InternalDesktopUi(
    title: String,
    tray: @Composable () -> Unit,
    exitApplication: () -> Unit,
    moduleBlock: KoinApplication.() -> Unit,
) {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                printLogger(Level.DEBUG)
                modules(
                    module {
                        includes(kmpModule)

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
            val sourceRepository = koinInject<SourceRepository>()
            val setupRepository = koinInject<SetupRepository>()
            val extensionWatcher = koinInject<ExtensionWatcher>()
            LaunchedEffect(Unit) {
                setupRepository.setup(this)
                val exampleService = ExampleService.getSourceInformation()
                extensionWatcher
                    .observeExtensionsDir()
                    .onEach {
                        sourceLoader.blockingLoad()
                        sourceRepository.addSource(exampleService)
                    }
                    .launchIn(this)
            }

            val jsExtensionLoader = koinInject<JSExtensionLoader>()
            val jsExtensionRepository = koinInject<JsExtensionRepository>()
            val jsExtensionDiscovery = koinInject<ExtensionDiscovery>()
            val jsExtensionSourceBridge = koinInject<JsExtensionSourceBridge>()
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    jsExtensionDiscovery.scanBundledResources().forEach { source ->
                        val extension = jsExtensionLoader.load(source.scriptText, source.fileName, source.companionManifestJson)
                        jsExtensionRepository.register(extension)
                    }
                }
            }

            val backgroundWorkHandler = koinInject<BackgroundWorkHandler>()
            LaunchedEffect(Unit) {
                backgroundWorkHandler.setupPeriodicCheckers()
            }

            tray()

            val windowState = rememberWindowState()

            Window(
                onCloseRequest = exitApplication,
                title = title,
                state = windowState,
                undecorated = true,
                transparent = true,
            ) {
                KmpLocalCompositionSetup {
                    OtakuMaterialTheme(
                        settingsHandling = koinInject(),
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
                                CompositionLocalProvider(
                                    LocalNavHostPadding provides PaddingValues()
                                ) {
                                    CustomTitleBar(
                                        title = title,
                                        onMinimizeClick = { windowState.isMinimized = true },
                                        onMaximizeToggle = {
                                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                                WindowPlacement.Floating
                                            } else {
                                                WindowPlacement.Maximized
                                            }
                                        },
                                        onCloseClick = exitApplication
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun InternalBaseDesktopUi(
    title: String,
    tray: @Composable () -> Unit,
    exitApplication: () -> Unit,
    moduleBlock: KoinApplication.() -> Unit,
    content: @Composable () -> Unit,
) {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                printLogger(Level.DEBUG)
                modules(
                    module {
                        includes(kmpModule)

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
            tray()

            val windowState = rememberWindowState()

            Window(
                onCloseRequest = exitApplication,
                title = title,
                state = windowState,
                undecorated = true,
                transparent = true,
            ) {
                KmpLocalCompositionSetup {
                    OtakuMaterialTheme(
                        settingsHandling = koinInject(),
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
                                CompositionLocalProvider(
                                    LocalNavHostPadding provides PaddingValues()
                                ) {
                                    CustomTitleBar(
                                        title = title,
                                        onMinimizeClick = { windowState.isMinimized = true },
                                        onMaximizeToggle = {
                                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                                WindowPlacement.Floating
                                            } else {
                                                WindowPlacement.Maximized
                                            }
                                        },
                                        onCloseClick = exitApplication
                                    )
                                    HorizontalDivider()
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun BaseWindow(
    title: String,
    exitApplication: () -> Unit,
    content: @Composable () -> Unit,
) {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = exitApplication,
        title = title,
        state = windowState,
        undecorated = true,
        transparent = true,
    ) {
        KmpLocalCompositionSetup {
            OtakuMaterialTheme(
                settingsHandling = koinInject(),
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
                        CompositionLocalProvider(
                            LocalNavHostPadding provides PaddingValues()
                        ) {
                            CustomTitleBar(
                                title = title,
                                onMinimizeClick = { windowState.isMinimized = true },
                                onMaximizeToggle = {
                                    windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                        WindowPlacement.Floating
                                    } else {
                                        WindowPlacement.Maximized
                                    }
                                },
                                onCloseClick = exitApplication
                            )
                            HorizontalDivider()
                            content()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameWindowScope.CustomTitleBar(
    title: String,
    onMinimizeClick: () -> Unit,
    onMaximizeToggle: () -> Unit,
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
            ),
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    var lastClickTime = 0L
                    while (true) {
                        // Observe the pointer events without consuming them
                        val event = awaitPointerEvent(PointerEventPass.Initial)

                        // Check for standard mouse/touch presses
                        if (event.type == PointerEventType.Press) {
                            val currentTime = System.currentTimeMillis()

                            // 300ms is a standard threshold for a double-click
                            if (currentTime - lastClickTime < 300) {
                                onMaximizeToggle()
                                lastClickTime = 0L // Reset to prevent triple-clicks from triggering twice
                            } else {
                                lastClickTime = currentTime
                            }
                        }
                    }
                }
            },
        )
    }
}