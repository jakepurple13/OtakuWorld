package com.programmersbox.kmpuiviews

import androidx.compose.foundation.layout.Box
import java.awt.Desktop
import java.util.logging.Logger
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
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
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import androidx.compose.runtime.collectAsState
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.tabs.BrowserWindow
import com.programmersbox.kmpuiviews.tabs.LocalAwtWindow
import com.programmersbox.kmpuiviews.tabs.shouldConfirmQuit
import com.programmersbox.kmpuiviews.tabs.tabCount
import com.programmersbox.kmpuiviews.tabs.tabsConfirmQuitPref
import com.programmersbox.kmpuiviews.tabs.TabsSettingsPage
import com.programmersbox.kmpuiviews.tabs.NewTabPage
import com.programmersbox.kmpuiviews.tabs.NewTabRoute
import com.programmersbox.kmpuiviews.tabs.TabsSettingsRoute
import com.programmersbox.kmpuiviews.tabs.TabsController
import com.programmersbox.kmpuiviews.tabs.TabSessionEffects
import com.programmersbox.kmpuiviews.tabs.TabSessionStore
import com.programmersbox.kmpuiviews.tabs.rememberMainWindowState
import com.programmersbox.kmpuiviews.tabs.desktopTabsModule
import com.programmersbox.kmpuiviews.utils.bindsPlatformGenericInfo
import com.programmersbox.supabaseintegration.Res
import com.programmersbox.supabaseintegration.supabase_logo_icon
import dev.nucleusframework.application.NucleusApplicationScope
import dev.nucleusframework.window.DecoratedWindow
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBarLayoutPolicy
import dev.nucleusframework.window.TitleBarScope
import dev.nucleusframework.window.material.MaterialTitleBar
import dev.nucleusframework.window.material.rememberMaterialTitleBarStyle
import dev.nucleusframework.window.material.rememberMaterialWindowStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import com.programmersbox.kmpuiviews.tabs.PendingWrites
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import com.programmersbox.kmpuiviews.telemetry.TelemetryController
import java.io.File
import kotlin.system.exitProcess

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

/** Puts the Actions menu in the macOS menu bar. Must run before any window is created. */
private fun useScreenMenuBarOnMac() {
    if (isMacOs) System.setProperty("apple.laf.useScreenMenuBar", "true")
}

/** Where a quit request came from, for the quit log line. */
private enum class QuitSource(val label: String) {
    WindowClose("main window close"),
    OsQuit("OS quit (⌘Q, Dock or app menu)"),
}

private val quitLog: Logger = Logger.getLogger("com.programmersbox.kmpuiviews.Quit")

/** Brings the whole app to the foreground where the OS allows it (macOS); elsewhere does nothing. */
private fun requestAppForeground() {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) desktop.requestForeground(true)
    }
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
    val quitReply = PendingQuitReply()
    // application() returns only after the composition is disposed, so every onDispose teardown has run before
    // the held OS quit is answered (answering it exits the JVM on macOS).
    quitReply.releaseAfter {
        application(exitProcessOnExit = false) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides desktopViewModelStoreOwner
            ) {
                BaseDesktopUi(
                    title = name,
                    quitReply = quitReply,
                    moduleBlock = {
                        modules(
                            module {
                                single { appConfig() }
                                single<UpdatePrefs> { DataStoreUpdatePrefs(get()) }
                                single<UpdaterEngine> { NucleusUpdaterEngine(channel = name.lowercase()) }
                                single { DesktopUpdater(get(), get(), CoroutineScope(Dispatchers.IO + SupervisorJob())) }
                                single { jvmAppLogo() }
                                navigation<MotionSettingsRoute> { MotionSettingsScreen() }
                                navigation<TabsSettingsRoute> { TabsSettingsPage() }
                                navigation<NewTabRoute> { NewTabPage() }
                                genericInfo(this).bindsPlatformGenericInfo()
                                moduleBlock()
                            }
                        )
                    }
                )
            }
        }
        // Dispose has queued the Notes panel's last write and the tab session's last save; give them a moment before
        // the quit is answered
        runBlocking { withTimeoutOrNull(1_000) { PendingWrites.App.awaitAll() } }
    }
    exitProcess(0)
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
    quitReply: PendingQuitReply,
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
        quitReply = quitReply,
        moduleBlock = moduleBlock
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NucleusApplicationScope.BaseDesktopUi(
    title: String,
    quitReply: PendingQuitReply,
    moduleBlock: KoinApplication.() -> Unit,
) {
    InternalDesktopUi(
        title = title,
        tray = {},
        exitApplication = ::exitApplication,
        quitReply = quitReply,
        moduleBlock = moduleBlock
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun InternalDesktopUi(
    title: String,
    tray: @Composable () -> Unit,
    exitApplication: () -> Unit,
    // Held OS quit; the caller answers it with PendingQuitReply.releaseAfter once the app loop has ended.
    quitReply: PendingQuitReply,
    moduleBlock: KoinApplication.() -> Unit,
) {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                printLogger(Level.DEBUG)
                modules(
                    module {
                        includes(kmpModule, noOpTelemetryModule(printToConsole = true), desktopTabsModule)

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
            val telemetryController = koinInject<TelemetryController>()
            LaunchedEffect(Unit) {
                setupRepository.setup(this)
                telemetryController.start(this)
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

            // Created at Koin start, which also restored the last session (tabs and the main window's bounds).
            val tabsController = koinInject<TabsController>()
            val mainWindow = tabsController.windows.first { it.isMain }
            val windowState = rememberMainWindowState(mainWindow)
            TabSessionEffects(tabsController, koinInject<TabSessionStore>(), mainWindow, windowState)
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
                    // The held OS quit is answered by the caller after the app loop ends, never here: answering exits the
                    // JVM, and exitApplication() only schedules the disposal.
                    exit = exitApplication,
                )
            }
            // "Ask before quitting with several tabs open": window close and ⌘Q both come through here.
            // Plain collectAsState: this runs outside the Window, where there is no LocalLifecycleOwner.
            val confirmQuit by remember { tabsConfirmQuitPref.asFlow() }.collectAsState(true)
            var askingQuit by remember { mutableStateOf(false) }
            // Bumped by every quit request while the dialog is up, so each one brings it back to the front.
            var askAgain by remember { mutableIntStateOf(0) }
            val requestQuit: (QuitSource) -> Unit = { source ->
                // Permanent: tells a real quit request apart from the process being killed (which logs nothing).
                quitLog.info("Quit requested from ${source.label}")
                when {
                    askingQuit -> askAgain++
                    quitController.state == QuitController.State.Idle &&
                        shouldConfirmQuit(tabsController.tabCount, confirmQuit) -> askingQuit = true
                    else -> quitController.requestQuit()
                }
            }
            // A request arriving after exit is a no-op; its held answer is still sent once the app loop ends.
            AppQuitHandler(quitReply) { requestQuit(QuitSource.OsQuit) }
            val quitInWindow = (quitController.state as? QuitController.State.Playing)
                ?.takeIf { it.plan.host == QuitHost.InWindow }
                ?.run
            OtakuDecoratedWindow(
                title = title,
                // The main window's close button quits the app, closing every window.
                onCloseRequest = { requestQuit(QuitSource.WindowClose) },
                state = windowState,
                // Hidden once exiting too, so the content never shows unfaded before the window goes.
                visible = !quitController.hideMainWindow && quitController.state != QuitController.State.Exited,
                showQuickActions = true,
                hostsOverlays = tabsController.focusedWindow == mainWindow.id,
                onAnyKey = { launchRun.skip() },
                titleBarContent = { WindowTitleBar(mainWindow, tabsController, title) },
                onKeyEvent = { windowTabShortcut(it, mainWindow, tabsController) },
                overlay = { WindowOverlays(mainWindow, tabsController) },
            ) {
                if (askingQuit) {
                    // The dialog lives in the main window: un-minimize it and bring it forward, so ⌘Q from a torn-out
                    // window, or with the main window minimized or behind others, never waits on a hidden prompt.
                    val awtWindow = LocalAwtWindow.current
                    LaunchedEffect(askAgain) {
                        // A Dock quit leaves the app in the background on macOS, where toFront() alone can't activate it.
                        requestAppForeground()
                        windowState.isMinimized = false
                        awtWindow?.toFront()
                        awtWindow?.requestFocus()
                    }
                    ConfirmQuitDialog(
                        tabCount = tabsController.tabCount,
                        onQuit = { dontAskAgain ->
                            askingQuit = false
                            // On the queue quit waits for, so exiting at once can't drop it.
                            if (dontAskAgain) PendingWrites.App.launch { tabsConfirmQuitPref.set(false) }
                            quitController.requestQuit()
                        },
                        onCancel = {
                            askingQuit = false
                            // An OS quit (⌘Q, Dock) waits for an answer; without one macOS refuses later quits.
                            quitReply.cancel()
                        },
                    )
                }
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
                        BrowserWindow(window = mainWindow, controller = tabsController)
                        val updater = koinInject<DesktopUpdater>()
                        LaunchedEffect(Unit) { updater.checkOnLaunch() }
                        UpdateLaunchDialog(updater)
                        JustUpdatedSnackbar(updater)
                    }
                }
            }

            // Gone once quitting: the app's exit closes every window.
            if (quitController.state != QuitController.State.Exited) TornOutWindows(tabsController, appColors = contentColors)

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
                        includes(kmpModule, noOpTelemetryModule(printToConsole = true), desktopTabsModule)

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
internal fun OtakuDecoratedWindow(
    title: String,
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    showQuickActions: Boolean = false,
    /** Whether this window shows the shortcuts dialog. With several windows, only the focused one does. */
    hostsOverlays: Boolean = showQuickActions,
    /** Replaces the centered title, filling the space between the start and end items. */
    titleBarContent: (@Composable TitleBarScope.() -> Unit)? = null,
    /** Runs after the window's own shortcut handlers; return true to consume the event. */
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    /** Drawn over the title bar and the content, outside the content surface's clip. */
    overlay: (@Composable () -> Unit)? = null,
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

                    else -> onKeyEvent?.invoke(event) == true
                }
            },
        ) {
            KmpLocalCompositionSetup {
                CompositionLocalProvider(LocalAwtWindow provides window) {
                    OtakuMaterialTheme(
                        settingsHandling = koinInject(),
                    ) {
                        val colorScheme = MaterialTheme.colorScheme
                        SideEffect { appColorScheme = colorScheme }

                        if (showQuickActions && isMacOs) QuickActionsMenuBar()

                        // Title bar must be a direct child of the window's layout (it is placed by layoutId).
                        MaterialTitleBar(
                            style = rememberMaterialTitleBarStyle(colorScheme),
                            layoutPolicy = if (titleBarContent != null) TitleBarLayoutPolicy.FillCenter else TitleBarLayoutPolicy.Default,
                        ) {
                            if (showQuickActions && quickActionsController != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.Start),
                                ) {
                                    if (!isMacOs) QuickActionsTitleBarMenu()
                                    ShortcutsTitleBarButton(onClick = quickActionsController::openShortcuts)
                                }
                            }
                            if (titleBarContent != null) {
                                titleBarContent()
                            } else {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                            }
                        }

                        // One child for the window's layout: the Surface clips to its bounds, the overlay does not.
                        Box(modifier = Modifier.fillMaxSize()) {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                CompositionLocalProvider(
                                    LocalNavHostPadding provides PaddingValues()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        content()
                                        if (hostsOverlays && quickActionsController != null) {
                                            ShortcutsDialogHost(quickActionsController)
                                        }
                                    }
                                }
                            }
                            overlay?.invoke()
                        }
                    }
                }
            }
        }
    }
}
