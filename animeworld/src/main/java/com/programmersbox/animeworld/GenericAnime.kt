package com.programmersbox.animeworld

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
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
import com.programmersbox.anime.shared.videochoice.VideoChoiceScreen
import com.programmersbox.anime.shared.videochoice.VideoSourceModel
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videoplayer.defaultSubtitleIndex
import com.programmersbox.anime.shared.videoplayer.playerHeaders
import com.programmersbox.anime.shared.videoplayer.streamMime
import com.programmersbox.anime.shared.videoplayer.videoPlayerBlur
import com.programmersbox.anime.shared.videos.SharedVideoContent
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.aniyomi.aniyomiModule
import com.programmersbox.cloudstream.cloudstreamModule
import com.programmersbox.datastore.asState
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.kmpuiviews.SystemAlerter
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
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import kotlinx.coroutines.CancellationException
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

    navigation<VideoScreen> { VideoPlayerUi(it) }

    navigation<CastRemoteRoute> { CastRemoteScreen() }

    navigation<SubtitleSettingsRoute> { SubtitleSettingsScreen() }

    navigation<Screen.SourcePreferences> { SourcePreferencesRoute(it.serviceName) }

    includes(aniyomiModule)
    includes(cloudstreamModule)

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
) : GenericSharedAnime(appConfig = appConfig), GenericInfo {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override val deepLinkUri: String get() = "animeworld://"

    private var storageAccessDialog by mutableStateOf<StorageAccessPrompt?>(null)

    override fun onResolveFailed(storage: KmpStorage, error: Throwable) {
        val name = listOfNotNull(storage.source, storage.quality).joinToString(" ").ifBlank { "stream" }
        runOnUIThread {
            Toast.makeText(context, "Couldn't load $name: ${error.message ?: error::class.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    override fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) {
        if (isCasting()) {
            launchCast {
                startCast(
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
        } else {
            storageHolder.storageModel = storage
            storageHolder.showTitle = infoModel.title
            storageHolder.posterUrl = infoModel.imageUrl
            navController.navigate(
                VideoScreen(
                    showPath = storage.link.orEmpty(),
                    showName = model.name,
                    downloadOrStream = false,
                    referer = storage.headers["referer"] ?: storage.source.orEmpty()
                )
            )
        }
    }

    fun isCasting(): Boolean = castController.state.value.connection == CastConnection.Connected

    /** Casts a downloaded episode from the Videos screen, replacing whatever is on the TV. */
    fun castDownload(content: SharedVideoContent) {
        launchCast {
            val (file, subtitles) = withContext(Dispatchers.IO) {
                val file = OfflineSubtitles.resolveVideoFile(context, content.path) ?: File(content.path)
                file to OfflineSubtitles.find(file)
            }
            startCast(
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
    private fun launchCast(block: suspend () -> Unit) {
        if (!castInFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                Toast.makeText(context, "Casting…", Toast.LENGTH_SHORT).show()
                block()
            } finally {
                castInFlight.set(false)
            }
        }
    }

    private suspend fun startCast(request: CastRequest) {
        val result = castController.cast(request)
        if (result == CastResult.Started) {
            navController.navigate(CastRemoteRoute)
        } else {
            result.errorMessage()?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        scope.launch {
            downloadOnClick(
                model = model,
                infoModel = infoModel,
                onNone = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.source_no_download, model.source.serviceName),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                download = { downloadResolved(model, infoModel, it) },
            )
        }
    }

    /** Lets the source resolve a lazily-described stream first, then hands it to the episode downloader. */
    suspend fun downloadResolved(model: KmpChapterModel, infoModel: KmpInfoModel, storage: KmpStorage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                storageAccessDialog = StorageAccessPrompt.AllFiles
                return
            }
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storageAccessDialog = StorageAccessPrompt.Storage
            return
        }
        val resolved = try {
            withContext(Dispatchers.IO) { model.source.resolve(storage) }.also { r ->
                if (r !== storage) {
                    if (r.headers.isEmpty()) r.headers.putAll(storage.headers)
                    if (r.extras.isEmpty()) r.extras.putAll(storage.extras)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            onResolveFailed(storage, e)
            return
        }
        val link = resolved.link
        if (link.isNullOrBlank()) {
            onResolveFailed(storage, IllegalStateException("No link"))
            return
        }
        enqueueEpisodeDownload(
            context,
            EpisodeDownloadRequest(
                url = link,
                headers = playerHeaders(resolved.headers, resolved.headers["referer"] ?: resolved.source),
                mimeType = resolved.mimeType,
                qualityHint = resolved.quality,
                subtitles = resolved.subtitles,
                title = "${infoModel.title} - ${model.name}",
                folder = context.folderLocation,
            )
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.downloading_dots_no_percent, Toast.LENGTH_SHORT).show()
        }
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
        VideoSourceModel.showVideoSources?.let {
            val navController = LocalNavActions.current
            VideoChoiceScreen(
                items = it.c,
                infoModel = it.infoModel,
                model = it.model,
                genericInfo = this,
                navController = navController,
                onPlay = { s ->
                    if (it.isStreaming) playResolved(navController, s, it.model, it.infoModel)
                    else downloadResolved(it.model, it.infoModel, s)
                }
            )
        }
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
