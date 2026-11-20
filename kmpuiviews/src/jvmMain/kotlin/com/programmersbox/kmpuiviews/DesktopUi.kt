package com.programmersbox.kmpuiviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import com.programmersbox.kmpuiviews.presentation.shortcuts.AppShortcut
import com.programmersbox.kmpuiviews.presentation.shortcuts.ShortcutsDialogHost
import com.programmersbox.kmpuiviews.presentation.shortcuts.matches
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res as KmpRes
import otakuworld.kmpuiviews.generated.resources.qa_shortcuts_button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.WindowState
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
import com.programmersbox.kmpuiviews.motion.AppQuitHandler
import com.programmersbox.kmpuiviews.motion.PendingQuitReply
import com.programmersbox.kmpuiviews.motion.QuitController
import com.programmersbox.kmpuiviews.motion.REDUCED_MOTION_REFRESH_MILLIS
import com.programmersbox.kmpuiviews.motion.QuitHost
import com.programmersbox.kmpuiviews.motion.QuitOverlay
import com.programmersbox.kmpuiviews.motion.WindowPicture
import com.programmersbox.kmpuiviews.motion.quitPlan
import com.programmersbox.kmpuiviews.motion.recordInto
import com.programmersbox.kmpuiviews.motion.refreshSystemReducesMotion
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.WindowPlacement
import com.programmersbox.kmpuiviews.motion.DesktopMotionCatalog
import com.programmersbox.kmpuiviews.motion.MotionConditions
import com.programmersbox.kmpuiviews.motion.MotionHost
import com.programmersbox.kmpuiviews.motion.MotionPhase
import com.programmersbox.kmpuiviews.motion.MotionRun
import com.programmersbox.kmpuiviews.motion.MotionSettingsRoute
import com.programmersbox.kmpuiviews.motion.MotionSettingsScreen
import com.programmersbox.kmpuiviews.motion.OffMotion
import com.programmersbox.kmpuiviews.motion.OverlayPlatform
import com.programmersbox.kmpuiviews.motion.Play
import com.programmersbox.kmpuiviews.motion.launchMotion
import com.programmersbox.kmpuiviews.motion.resolve
import com.programmersbox.kmpuiviews.motion.skipMotionOnInput
import com.programmersbox.kmpuiviews.motion.systemReducesMotion
import com.programmersbox.kmpuiviews.presentation.HomeNav
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.quickactions.OpenOrigin
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsController
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsMenuBar
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsTitleBarMenu
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import com.programmersbox.kmpuiviews.presentation.quickactions.isQuickActionsShortcut
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridge
import com.programmersbox.kmpuiviews.repository.SetupRepository
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import com.programmersbox.kmpuiviews.updates.DataStoreUpdatePrefs
import com.programmersbox.kmpuiviews.updates.DesktopUpdater
import com.programmersbox.kmpuiviews.updates.JustUpdatedSnackbar
import com.programmersbox.kmpuiviews.updates.NucleusUpdaterEngine
import com.programmersbox.kmpuiviews.updates.UpdateLaunchDialog
import com.programmersbox.kmpuiviews.updates.UpdatePrefs
import com.programmersbox.kmpuiviews.updates.UpdaterEngine
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.bindsPlatformGenericInfo
import com.programmersbox.supabaseintegration.Res
import com.programmersbox.supabaseintegration.supabase_logo_icon
import dev.nucleusframework.application.NucleusApplicationScope
import dev.nucleusframework.window.DecoratedWindow
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.material.MaterialTitleBar
import dev.nucleusframework.window.material.rememberMaterialTitleBarStyle
import dev.nucleusframework.window.material.rememberMaterialWindowStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.definition.KoinDefinition
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
import org.koin.dsl.koinConfiguration
import org.koin.dsl.navigation3.navigation
import org.koin.dsl.module
import com.programmersbox.telemetry.noOpTelemetryModule
import java.io.File

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

/** Puts the Actions menu in the macOS menu bar. Must run before any window is created. */
private fun useScreenMenuBarOnMac() {
    if (isMacOs) System.setProperty("apple.laf.useScreenMenuBar", "true")
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
    useScreenMenuBarOnMac()
    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    NativeSplash.start()
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
                            single<UpdatePrefs> { DataStoreUpdatePrefs(get()) }
                            single<UpdaterEngine> { NucleusUpdaterEngine(channel = name.lowercase()) }
                            single { DesktopUpdater(get(), get(), CoroutineScope(Dispatchers.IO + SupervisorJob())) }
                            single { jvmAppLogo() }
                            navigation<MotionSettingsRoute> { MotionSettingsScreen() }
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
    useScreenMenuBarOnMac()
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
                        includes(kmpModule, noOpTelemetryModule(printToConsole = true))

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

            val windowState = rememberWindowState()
            val settings = koinInject<MangaDesktopSettings>()
            val koin = getKoin()
            val catalog = remember { koin.getOrNull<DesktopMotionCatalog>() }
            val launchRun = remember {
                val key = runBlocking { settings.launchAnimation.get() }
                val chosen = catalog?.resolve(MotionPhase.Launch, key) ?: OffMotion
                MotionRun(
                    launchMotion(
                        chosen,
                        // Off never reads the system setting (it runs a process); otherwise this first read blocks once.
                        MotionConditions(chosen !== OffMotion && systemReducesMotion(), platform = OverlayPlatform.current()),
                    )
                )
            }
            // Keeps systemReducesMotion()'s cache fresh off the UI thread, so the quit read never runs a process.
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    while (true) {
                        refreshSystemReducesMotion()
                        delay(REDUCED_MOTION_REFRESH_MILLIS)
                    }
                }
            }
            val picture = remember { WindowPicture() }
            var contentHeight by remember { mutableStateOf(Dp.Unspecified) }
            var contentColors by remember { mutableStateOf<ColorScheme?>(null) }
            val quitReply = remember { PendingQuitReply() }
            val quitController = remember {
                QuitController(
                    plan = {
                        val key = runBlocking { settings.quitAnimation.get() }
                        val chosen = catalog?.resolve(MotionPhase.Quit, key) ?: OffMotion
                        val plan = quitPlan(
                            chosen,
                            MotionConditions(
                                chosen !== OffMotion && systemReducesMotion(),
                                fullscreen = windowState.placement == WindowPlacement.Fullscreen,
                                minimized = windowState.isMinimized,
                                platform = OverlayPlatform.current(),
                            ),
                        )
                        // The overlay needs the window's real position.
                        if (windowState.position.isSpecified) plan else {
                            println("Quit motion: window position unspecified, playing in-window")
                            plan.copy(host = QuitHost.InWindow)
                        }
                    },
                    launch = { launchRun },
                    exit = {
                        exitApplication()
                        // Accept the OS quit (logout/restart/shutdown) only now that the app has exited.
                        quitReply.release()
                    },
                )
            }
            AppQuitHandler(quitReply) {
                quitController.requestQuit()
                // Already exited before this request (it was a no-op): answer the OS now.
                if (quitController.state == QuitController.State.Exited) quitReply.release()
            }
            val quitInWindow = (quitController.state as? QuitController.State.Playing)
                ?.takeIf { it.plan.host == QuitHost.InWindow }
                ?.run
            OtakuDecoratedWindow(
                title = title,
                onCloseRequest = quitController::requestQuit,
                state = windowState,
                // Hidden once exiting too, so the content never shows unfaded before the window goes.
                visible = !quitController.hideMainWindow && quitController.state != QuitController.State.Exited,
                showQuickActions = true,
                onAnyKey = { launchRun.skip() },
            ) {
                val colors = MaterialTheme.colorScheme
                SideEffect { contentColors = colors }
                launchRun.Play()
                quitInWindow?.Play(onEnd = quitController::finished)
                val activeRun = quitInWindow ?: launchRun
                MotionHost(
                    motion = if (activeRun.finished) OffMotion else activeRun.motion,
                    time = { activeRun.timeMillis },
                    modifier = Modifier.fillMaxSize().skipMotionOnInput(launchRun),
                ) {
                    val density = LocalDensity.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .recordInto(picture)
                            .onSizeChanged { contentHeight = with(density) { it.height.toDp() } }
                    ) {
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
                        val updater = koinInject<DesktopUpdater>()
                        LaunchedEffect(Unit) { updater.checkOnLaunch() }
                        UpdateLaunchDialog(updater)
                        JustUpdatedSnackbar(updater)
                    }
                }
            }

            val chromeColors = contentColors ?: MaterialTheme.colorScheme
            QuitOverlay(
                controller = quitController,
                picture = picture,
                windowState = windowState,
                contentHeight = { contentHeight },
                titleBarColor = rememberMaterialTitleBarStyle(chromeColors).colors.background,
                surfaceColor = chromeColors.surface,
            )

            // Tray must come after Window: the Skiko graphics implementation is registered
            // when the window's ComposeContainer is created, and Tray converts its icon
            // painter to an AWT image as soon as it enters composition.
            tray()
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
                        includes(kmpModule, noOpTelemetryModule(printToConsole = true))

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
            OtakuDecoratedWindow(
                title = title,
                onCloseRequest = exitApplication,
                content = content,
            )

            // Tray must come after Window: the Skiko graphics implementation is registered
            // when the window's ComposeContainer is created, and Tray converts its icon
            // painter to an AWT image as soon as it enters composition.
            tray()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutsTitleBarButton(onClick: () -> Unit) {
    val description = stringResource(KmpRes.string.qa_shortcuts_button)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Keyboard, contentDescription = description)
        }
    }
}

@Composable
fun BaseWindow(
    title: String,
    exitApplication: () -> Unit,
    content: @Composable () -> Unit,
) {
    OtakuDecoratedWindow(
        title = title,
        onCloseRequest = exitApplication,
        content = content,
    )
}

/**
 * Native-decorated window (Nucleus JNI backend): real OS window controls, drag, snap and resize,
 * with the title bar colored from the app's Material theme.
 */
@Composable
private fun OtakuDecoratedWindow(
    title: String,
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    showQuickActions: Boolean = false,
    onAnyKey: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val quickActionsController = if (showQuickActions) koinInject<QuickActionsController>() else null
    val navigationActions = if (showQuickActions) koinInject<NavigationActions>() else null

    // The window chrome reads its styles outside the window's content, where the app theme isn't
    // composed yet (and isSystemInDarkTheme() isn't resolvable). The content publishes its color
    // scheme back up here so the border and resize background follow the app theme.
    var appColorScheme by remember { mutableStateOf<ColorScheme?>(null) }
    val chromeColorScheme = appColorScheme ?: MaterialTheme.colorScheme

    NucleusDecoratedWindowTheme(
        isDark = chromeColorScheme.background.luminance() < 0.5f,
        windowStyle = rememberMaterialWindowStyle(chromeColorScheme),
        titleBarStyle = rememberMaterialTitleBarStyle(chromeColorScheme),
    ) {
        DecoratedWindow(
            onCloseRequest = onCloseRequest,
            state = state,
            visible = visible,
            title = title,
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown) onAnyKey()
                when {
                    quickActionsController == null -> false
                    !isMacOs && isQuickActionsShortcut(event, isMac = false) -> {
                        quickActionsController.toggle(OpenOrigin.Keyboard)
                        true
                    }

                    !isMacOs && AppShortcut.GlobalSearch.matches(event, isMac = false) -> {
                        quickActionsController.closeAll()
                        navigationActions?.globalSearch()
                        true
                    }

                    !isMacOs && AppShortcut.ShortcutsList.matches(event, isMac = false) -> {
                        quickActionsController.openShortcuts()
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                            event.key == Key.Escape &&
                            quickActionsController.shortcutsOpen.value -> {
                        quickActionsController.closeShortcuts()
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                            event.key == Key.Escape &&
                            quickActionsController.open.value != null -> {
                        quickActionsController.close()
                        true
                    }

                    else -> false
                }
            },
        ) {
            KmpLocalCompositionSetup {
                OtakuMaterialTheme(
                    settingsHandling = koinInject(),
                ) {
                    val colorScheme = MaterialTheme.colorScheme
                    SideEffect { appColorScheme = colorScheme }

                    if (showQuickActions && isMacOs) QuickActionsMenuBar()

                    // Title bar must be a direct child of the window's layout (it is placed by layoutId).
                    MaterialTitleBar(style = rememberMaterialTitleBarStyle(colorScheme)) {
                        if (showQuickActions && quickActionsController != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.Start),
                            ) {
                                if (!isMacOs) QuickActionsTitleBarMenu()
                                ShortcutsTitleBarButton(onClick = quickActionsController::openShortcuts)
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(
                            LocalNavHostPadding provides PaddingValues()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                content()
                                if (showQuickActions && quickActionsController != null) {
                                    ShortcutsDialogHost(quickActionsController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
