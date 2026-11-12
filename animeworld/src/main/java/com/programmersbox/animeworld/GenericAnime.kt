package com.programmersbox.animeworld

import com.programmersbox.kmpuiviews.utils.composables.transitions.rememberChapterRevealOrigin
import androidx.compose.foundation.layout.Box
import com.programmersbox.kmpuiviews.utils.composables.transitions.ChapterOpenMotion
import com.programmersbox.kmpuiviews.utils.composables.transitions.chapterOpenTransition
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonalVideo
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.obsez.android.lib.filechooser.ChooserDialog
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.cast.CastButton
import com.programmersbox.anime.shared.cast.CastConnection
import com.programmersbox.anime.shared.cast.CastController
import com.programmersbox.anime.shared.cast.CastDevicePickerSheet
import com.programmersbox.anime.shared.cast.CastMediaServer
import com.programmersbox.anime.shared.cast.CastRemoteRoute
import com.programmersbox.anime.shared.cast.CastRemoteScreen
import com.programmersbox.anime.shared.cast.CastRequest
import com.programmersbox.anime.shared.cast.CastResult
import com.programmersbox.anime.shared.cast.castSourceHeaders
import com.programmersbox.anime.shared.cast.errorMessage
import com.programmersbox.anime.shared.cast.wifiIpv4
import com.programmersbox.anime.shared.downloads.DownloadQueueSource
import com.programmersbox.anime.shared.downloads.EpisodeDownloadRequest
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.downloads.enqueueEpisodeDownload
import com.programmersbox.anime.shared.subtitles.DataStoreSubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRoute
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsScreen
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsViewModel
import com.programmersbox.anime.shared.episodeload.EpisodeLoadHost
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videoplayer.defaultSubtitleIndex
import com.programmersbox.anime.shared.videoplayer.playerHeaders
import com.programmersbox.anime.shared.videoplayer.streamMime
import com.programmersbox.anime.shared.videoplayer.PlayerPreviewDefaults
import com.programmersbox.anime.shared.videoplayer.PlayerStyleSheetSetting
import com.programmersbox.anime.shared.videoplayer.videoPlayerBlur
import com.programmersbox.anime.shared.videos.SharedVideoContent
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.FreeUpSpaceRoute
import com.programmersbox.anime.shared.videos.FreeUpSpaceScreen
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.aniyomi.aniyomiModule
import com.programmersbox.cloudstream.cloudstreamModule
import com.programmersbox.datastore.asState
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import com.programmersbox.sharedcomponents.notifier.ProgressHandle
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koin.compose.koinInject
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

val appModule = module {
    singleOf(::GenericAnime) { bindsGenericInfo() }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    single { StorageHolder() }
    single<SubtitleSettingsRepository> { DataStoreSubtitleSettingsRepository(get<Context>()) }
    single {
        // Same User-Agent the phone player sends, so proxied requests look like local playback.
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        val userAgent = androidx.media3.common.util.Util.getUserAgent(get<Context>(), "AnimeWorld")
        CastMediaServer(OkHttpClient(), { wifiIpv4(get<Context>()) }, userAgent = userAgent)
    }
    single { CastController(get<Context>(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) }
    viewModelOf(::SubtitleSettingsViewModel)
    single { VideoLibrarySource(get(), downloadFolder = { get<Context>().folderLocation }) }
    singleOf(::DownloadQueueSource)
    single { AnimeDataStoreHandling() }
    singleOf(::Backup)
    factory { Zipper(get(), getAll<BackupProcessor>(), get()) }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }

    navigation<VideoScreen>(metadata = ChapterOpenMotion.readerMetadata()) {
        // Library rows record a tap point; streams arrive from the loading overlay and get depth
        val origin = rememberChapterRevealOrigin(it.showPath)
        Box(Modifier.fillMaxSize().chapterOpenTransition(origin)) { VideoPlayerUi(it) }
    }

    navigation<CastRemoteRoute> { CastRemoteScreen() }

    navigation<SubtitleSettingsRoute> { SubtitleSettingsScreen() }

    navigation<Screen.SourcePreferences> { SourcePreferencesRoute(it.serviceName) }

    includes(aniyomiModule)
    includes(cloudstreamModule)

    navigation<FreeUpSpaceRoute> { FreeUpSpaceScreen() }

    navigation<VideoViewerRoute> {
        val genericAnime = koinInject<GenericAnime>()
        ViewVideoScreen(
            isCastActive = genericAnime::isCasting,
            onCastLoad = genericAnime::castDownload,
            castButton = { CastPickerButton() },
            deleteDialog = { content, onResult ->
                AlertDialog(
                    onDismissRequest = { onResult(false) },
                    title = { Text(stringResource(R.string.remove)) },
                    text = { Text(content.videoName) },
                    confirmButton = {
                        TextButton(onClick = { onResult(true) }) { Text(stringResource(R.string.remove)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { onResult(false) }) { Text(stringResource(R.string.cancelText)) }
                    }
                )
            }
        )
    }
}

class GenericAnime(
    val context: Context,
    val storageHolder: StorageHolder,
    val animeDataStoreHandling: AnimeDataStoreHandling,
    appConfig: AppConfig,
    private val navController: NavigationActions,
    private val castController: CastController,
    private val subtitleSettings: SubtitleSettingsRepository,
    private val notifier: InAppNotifier,
) : GenericSharedAnime(appConfig = appConfig), GenericInfo {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override val deepLinkUri: String get() = "animeworld://"

    override val updateUnit: UpdateUnit get() = UpdateUnit.Episodes

    private var storageAccessDialog by mutableStateOf<StorageAccessPrompt?>(null)

    override fun onResolveFailed(storage: KmpStorage, error: Throwable) {
        val name = listOfNotNull(storage.source, storage.quality).joinToString(" ").ifBlank { "stream" }
        notifier.error("Couldn't load $name", error.message ?: error::class.simpleName)
    }

    override fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        seedColor: Int?,
    ) {
        if (isCasting()) {
            launchCast { progress ->
                startCast(
                    progress,
                    CastRequest(
                        showTitle = infoModel.title,
                        episodeName = model.name,
                        posterUrl = infoModel.imageUrl,
                        url = storage.link.orEmpty(),
                        mimeType = streamMime(storage),
                        headers = playerHeaders(storage.headers, storage.headers["referer"] ?: storage.source.orEmpty()),
                        sourceHeaders = castSourceHeaders(storage.headers, storage.headers["referer"] ?: storage.source),
                        subtitles = storage.subtitles,
                        selectedSubtitle = defaultCastSubtitle(storage.subtitles),
                        startPositionMs = context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(storage.link, 0L),
                    )
                )
            }
            episodeLoader.finishHandoff()
        } else {
            storageHolder.storageModel = storage
            storageHolder.showTitle = infoModel.title
            storageHolder.posterUrl = infoModel.imageUrl
            navController.navigate(
                VideoScreen(
                    showPath = storage.link.orEmpty(),
                    showName = model.name,
                    downloadOrStream = false,
                    referer = storage.headers["referer"] ?: storage.source.orEmpty(),
                    seedColor = seedColor
                )
            )
        }
    }

    fun isCasting(): Boolean = castController.state.value.connection == CastConnection.Connected

    /** Casts a downloaded episode from the Videos screen, replacing whatever is on the TV. */
    fun castDownload(content: SharedVideoContent) {
        launchCast { progress ->
            val (file, subtitles) = withContext(Dispatchers.IO) {
                val file = OfflineSubtitles.resolveVideoFile(context, content.path) ?: File(content.path)
                file to OfflineSubtitles.find(file)
            }
            startCast(
                progress,
                CastRequest(
                    showTitle = "",
                    episodeName = content.videoName,
                    posterUrl = null,
                    url = file.path,
                    mimeType = null,
                    headers = emptyMap(),
                    sourceHeaders = emptyMap(),
                    subtitles = subtitles,
                    selectedSubtitle = defaultCastSubtitle(subtitles),
                    startPositionMs = context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(content.path, 0L),
                    localFile = file,
                    subtitleOrigin = "downloaded",
                )
            )
        }
    }

    /** Same pick as the phone player: none when auto-enable is off, else [defaultSubtitleIndex]. */
    private suspend fun defaultCastSubtitle(subtitles: List<KmpSubtitle>): Int? {
        val style = subtitleSettings.style.first()
        if (!style.autoEnable) return null
        return defaultSubtitleIndex(
            subtitles,
            Locale.getDefault().language,
            Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
            style.preferredLanguages,
            style.skipSignsAndSongs,
        )
    }

    private val castInFlight = AtomicBoolean(false)

    /** Runs [block] unless a cast is already starting, so a double tap doesn't load twice. */
    private fun launchCast(block: suspend (ProgressHandle) -> Unit) {
        if (!castInFlight.compareAndSet(false, true)) return
        val progress = notifier.progress("Casting…")
        scope.launch {
            try {
                // startCast resolves the handle itself; dismissing afterwards would remove its error banner.
                block(progress)
            } catch (t: Throwable) {
                // Exception or cancellation before startCast resolved it: clear the spinner.
                progress.dismiss()
                throw t
            } finally {
                castInFlight.set(false)
            }
        }
    }

    private suspend fun startCast(progress: ProgressHandle, request: CastRequest) {
        val result = castController.cast(request)
        if (result == CastResult.Started) {
            progress.dismiss()
            navController.navigate(CastRemoteRoute)
        } else {
            val message = result.errorMessage()
            if (message != null) progress.error(context.getString(R.string.couldnt_cast), message) else progress.dismiss()
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        scope.launch(Dispatchers.IO) { loadForDownload(model, infoModel) }
    }

    override fun onNoDownloadSources(model: KmpChapterModel) {
        notifier.error(context.getString(R.string.source_no_download, model.source.serviceName))
    }

    override fun canStartDownload(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                storageAccessDialog = StorageAccessPrompt.AllFiles
                return false
            }
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storageAccessDialog = StorageAccessPrompt.Storage
            return false
        }
        return true
    }

    override suspend fun enqueueResolved(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel) {
        enqueueEpisodeDownload(
            context,
            EpisodeDownloadRequest(
                url = storage.link.orEmpty(),
                headers = playerHeaders(storage.headers, storage.headers["referer"] ?: storage.source),
                mimeType = storage.mimeType,
                qualityHint = storage.quality,
                subtitles = storage.subtitles,
                title = "${infoModel.title} - ${model.name}",
                folder = context.folderLocation,
            )
        )
        notifier.success(context.getString(R.string.download_started))
    }

    @Composable
    override fun DetailActions(infoModel: KmpInfoModel, tint: Color) {
        CastPickerButton()
    }

    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            segmentedListItem(
                onClick = { navController.navigate(VideoViewerRoute) },
                leadingContent = { Icon(Icons.Default.VideoLibrary, null) },
                content = { Text(stringResource(R.string.video_menu_title)) },
            )
        }

        generalSettings {
            val context = LocalContext.current
            var folderLocation by remember { mutableStateOf(context.folderLocation) }
            val activity = LocalActivity.current

            CategoryGroup {
                item {
                    val castState by castController.state.collectAsStateWithLifecycle()
                    val connected = castState.connection == CastConnection.Connected
                    var pickerOpen by remember { mutableStateOf(false) }
                    if (pickerOpen) {
                        CastDevicePickerSheet(onDismiss = { pickerOpen = false }, onConnected = { pickerOpen = false })
                    }
                    ShowWhen(castState.connection != CastConnection.Unavailable && castState.connection != CastConnection.NoDevices) {
                        PreferenceSetting(
                            settingTitle = { Text(stringResource(R.string.cast_menu_title)) },
                            settingIcon = {
                                Icon(
                                    if (connected) Icons.Default.CastConnected else Icons.Default.Cast,
                                    null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            modifier = Modifier.clickable(
                                indication = ripple(),
                                interactionSource = null
                            ) {
                                if (connected) navController.navigate(CastRemoteRoute) else pickerOpen = true
                            }
                        )
                    }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text(stringResource(R.string.folder_location)) },
                        summaryValue = { Text(folderLocation) },
                        settingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.fillMaxSize()) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) {
                            (activity as? FragmentActivity)?.requestPermissions(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            ) {
                                if (it.isGranted) {
                                    ChooserDialog(context)
                                        .withIcon(R.mipmap.ic_launcher)
                                        .withResources(R.string.choose_a_directory, R.string.chooseText, R.string.cancelText)
                                        .withFilter(true, false)
                                        .withStartFile(context.folderLocation)
                                        .enableOptions(true)
                                        .withChosenListener { dir, _ ->
                                            context.folderLocation = "$dir/"
                                            println(dir)
                                            folderLocation = context.folderLocation
                                        }
                                        .build()
                                        .show()
                                }
                            }
                        }
                    )
                }
            }
        }

        playerSettings {
            val scope = rememberCoroutineScope()
            val useNewPlayer = animeDataStoreHandling.useNewPlayer
            val player by useNewPlayer.asState()
            val ignoreSsl = animeDataStoreHandling.ignoreSsl
            val ignoreSslState by ignoreSsl.asState()
            var playerBlur by videoPlayerBlur.asState()

            CategoryGroup {
                item { PlayerStyleSheetSetting(PlayerPreviewDefaults.Android) }

                item {
                    val navController = LocalNavActions.current
                    PreferenceSetting(
                        settingTitle = { Text(stringResource(R.string.subtitles)) },
                        settingIcon = { Icon(Icons.Default.Subtitles, null, modifier = Modifier.fillMaxSize()) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) { navController.navigate(SubtitleSettingsRoute) }
                    )
                }

                item {
                    SwitchSetting(
                        settingTitle = { Text(stringResource(R.string.use_new_player)) },
                        summaryValue = { Text(stringResource(R.string.use_new_player_description)) },
                        settingIcon = { Icon(Icons.Default.PersonalVideo, null, modifier = Modifier.fillMaxSize()) },
                        value = player,
                        updateValue = { scope.launch { useNewPlayer.set(it) } }
                    )
                }

                item {
                    SwitchSetting(
                        settingTitle = { Text(stringResource(id = R.string.ignore_ssl)) },
                        settingIcon = { Icon(Icons.Default.Security, null, modifier = Modifier.fillMaxSize()) },
                        value = ignoreSslState
                    ) { scope.launch { ignoreSsl.set(it) } }
                }

                item {
                    SwitchSetting(
                        settingTitle = { Text(stringResource(R.string.video_player_blur)) },
                        summaryValue = { Text(stringResource(R.string.video_player_blur_description)) },
                        settingIcon = { Icon(Icons.Default.BlurOn, null, modifier = Modifier.fillMaxSize()) },
                        value = playerBlur,
                        updateValue = { playerBlur = it }
                    )
                }
            }
        }

        onboardingSettings {
            item {
                PlayerOnboarding(
                    animeDataStoreHandling = animeDataStoreHandling,
                    onSubtitlesClick = { navController.navigate(SubtitleSettingsRoute) },
                )
            }
        }

        tips(animeTips)
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<VideoScreen> {
            VideoPlayerUi(it)
        }
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<FreeUpSpaceRoute> { FreeUpSpaceScreen() }
        navGraph.entry<VideoViewerRoute> {
            ViewVideoScreen(
                isCastActive = ::isCasting,
                onCastLoad = ::castDownload,
                castButton = { CastPickerButton() },
                deleteDialog = { content, onResult ->
                    AlertDialog(
                        onDismissRequest = { onResult(false) },
                        title = { Text(stringResource(R.string.remove)) },
                        text = { Text(content.videoName) },
                        confirmButton = {
                            TextButton(onClick = { onResult(true) }) { Text(stringResource(R.string.remove)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { onResult(false) }) { Text(stringResource(R.string.cancelText)) }
                        }
                    )
                }
            )
        }
    }

    @Composable
    override fun DialogSetups() {
        storageAccessDialog?.let { prompt ->
            AlertDialog(
                onDismissRequest = { storageAccessDialog = null },
                title = { Text(prompt.title) },
                text = { Text(prompt.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            storageAccessDialog = null
                            context.startActivity(
                                Intent(prompt.action, "package:${context.packageName}".toUri())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) { Text("Open settings") }
                },
                dismissButton = {
                    TextButton(onClick = { storageAccessDialog = null }) { Text("Cancel") }
                }
            )
        }
        EpisodeLoadHost(episodeLoader)
    }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkDetailsUri(itemModel),
            activity = MainActivity::class.java
        )
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkSettingsUri(),
            activity = MainActivity::class.java
        )
    }

    override fun deepLinkUpdateCheckErrors(context: Context): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkUpdateCheckErrorsUri(),
            activity = MainActivity::class.java
        )
    }

    override fun deepLink(context: Context, uri: Uri, cancelNotificationId: Int?): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = uri,
            activity = MainActivity::class.java,
            cancelNotificationId = cancelNotificationId,
        )
    }
}

private enum class StorageAccessPrompt(val title: String, val message: String, val action: String) {
    AllFiles(
        "Allow file access",
        "AnimeWorld needs All files access to save episodes and their subtitles to your download folder.",
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
    ),
    Storage(
        "Allow storage access",
        "AnimeWorld needs storage permission to save episodes and their subtitles.",
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    ),
}

/** Cast button that opens the device picker; a new connection just closes the sheet. */
@Composable
private fun CastPickerButton() {
    var pickerOpen by remember { mutableStateOf(false) }
    CastButton(onClick = { pickerOpen = true })
    if (pickerOpen) {
        CastDevicePickerSheet(onDismiss = { pickerOpen = false }, onConnected = { pickerOpen = false })
    }
}
