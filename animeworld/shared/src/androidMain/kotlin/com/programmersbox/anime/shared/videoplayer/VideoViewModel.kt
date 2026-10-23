package com.programmersbox.anime.shared.videoplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.cast.CastRequest
import com.programmersbox.anime.shared.cast.castSourceHeaders
import com.programmersbox.anime.shared.cast.closestSubtitleIndex
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.helpfulutils.battery
import com.programmersbox.uiviews.utils.BatteryInformation2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.cert.X509Certificate
import java.util.Formatter
import java.util.Locale
import javax.net.ssl.X509TrustManager

class VideoViewModel(
    videoScreen: VideoScreen,
    context: Context,
    private val storageHolder: StorageHolder,
    val subtitleSettings: SubtitleSettingsRepository,
) : ViewModel() {

    val showPath = storageHolder.storageModel?.link ?: videoScreen.showPath
    val showName = videoScreen.showName
    val downloadOrStream = videoScreen.downloadOrStream
    val requestHeaders = playerHeaders(storageHolder.storageModel?.headers.orEmpty(), videoScreen.referer)
    private val sourceOnlyHeaders = castSourceHeaders(storageHolder.storageModel?.headers.orEmpty(), videoScreen.referer)
    private val localFile = if (downloadOrStream) OfflineSubtitles.resolveVideoFile(context, showPath) else null
    val subtitles = if (downloadOrStream) {
        localFile?.let(OfflineSubtitles::find).orEmpty()
    } else {
        storageHolder.storageModel?.subtitles.orEmpty()
    }
    val mimeType = storageHolder.storageModel?.let(::streamMime)
    /** Style as of player start; later changes arrive through [subtitleSettings]. */
    val initialSubtitleStyle = runBlocking { subtitleSettings.style.first() }
    val defaultSubtitle = defaultSubtitleIndex(
        subtitles,
        Locale.getDefault().language,
        Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
        initialSubtitleStyle.preferredLanguages,
        initialSubtitleStyle.skipSignsAndSongs,
    )

    /** Text tracks the player currently exposes; drives the CC sheet. */
    var textTracks by mutableStateOf<List<TextTrackOption>>(emptyList())
    var subtitleSheetOpen by mutableStateOf(false)

    fun updateTextTracks(tracks: Tracks) {
        textTracks = tracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .map { group ->
                val format = group.getTrackFormat(0)
                TextTrackOption(group, embeddedTrackLabel(format.label, format.language), group.isSelected)
            }
        Log.d(TAG, "textTracks=${textTracks.map { "${it.group.getTrackFormat(0).language}|${it.label}|selected=${it.selected}" }}")
    }

    fun selectTextTrack(option: TextTrackOption?) {
        val player = exoPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .apply {
                if (option == null) {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                } else {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, 0))
                }
            }
            .build()
    }

    var exoPlayer: ExoPlayer? by mutableStateOf(null)

    /** What is playing now, at the current position, with the selected subtitle carried over. Main thread only. */
    fun castRequest(): CastRequest {
        val selected = textTracks.firstOrNull { it.selected }?.group?.getTrackFormat(0)?.let { format ->
            // Sidecar tracks are labelled with their KmpSubtitle.lang; anything else is embedded in the video.
            subtitles.indexOfFirst { it.lang == format.label }.takeIf { it >= 0 }
                ?: closestSubtitleIndex(
                    format.language,
                    format.label,
                    subtitles,
                    format.language?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH) }?.ifBlank { null },
                )
                ?: defaultSubtitle
        }
        return CastRequest(
            showTitle = storageHolder.showTitle.orEmpty(),
            episodeName = showName,
            posterUrl = storageHolder.posterUrl,
            url = localFile?.path ?: showPath,
            mimeType = mimeType,
            headers = if (downloadOrStream) emptyMap() else requestHeaders,
            sourceHeaders = if (downloadOrStream) emptyMap() else sourceOnlyHeaders,
            subtitles = subtitles,
            selectedSubtitle = selected,
            startPositionMs = exoPlayer?.currentPosition ?: 0L,
            localFile = localFile,
            subtitleOrigin = if (downloadOrStream) "downloaded" else "source",
        )
    }

    fun playPause() {
        quickSeekAction = if (exoPlayer?.isPlaying == true) {
            exoPlayer?.pause()
            QuickSeekAction.pause()
        } else {
            exoPlayer?.play()
            QuickSeekAction.play()
        }
    }

    fun fastForward() {
        exoPlayer?.let { p -> p.seekTo(p.currentPosition + 30000) }
        quickSeekAction = QuickSeekAction.forward()
    }

    fun skipAhead() {
        exoPlayer?.let { p -> p.seekTo(p.currentPosition + 90_000) }
        quickSeekAction = QuickSeekAction.forward()
    }

    fun rewind() {
        exoPlayer?.let { p -> p.seekTo(p.currentPosition - 30000) }
        quickSeekAction = QuickSeekAction.rewind()
    }

    var visibility by mutableStateOf(VideoPlayerVisibility.Gone)
    var quickSeekAction by mutableStateOf(QuickSeekAction.none())

    var videoInfo by mutableStateOf(VideoInfo(0L, 0L, 0L))

    var isPlaying by mutableStateOf(false)

    var batteryColor by mutableStateOf(Color.White)
    var batteryIcon by mutableStateOf(BatteryInformation2.BatteryViewType.UNKNOWN)
    var batteryPercent by mutableFloatStateOf(0f)
    val batteryInformation by lazy { BatteryInformation2(context) }

    init {
        Log.d(TAG, "subtitles=${subtitles.map { it.lang + "|" + it.url }} default=$defaultSubtitle")
        viewModelScope.launch(Dispatchers.IO) {
            batteryInformation.composeSetupFlow(
                Color.White
            ) {
                batteryColor = it.first
                batteryIcon = it.second
            }
        }
    }

    val currentTime by derivedStateOf { stringForTime(videoInfo.currentPosition) }
    val totalTime by derivedStateOf { stringForTime(videoInfo.duration) }

    private fun stringForTime(milliseconded: Long): String {
        var milliseconds = milliseconded
        if (milliseconds < 0 || milliseconds >= 24 * 60 * 60 * 1000) {
            return "00:00"
        }
        milliseconds /= 1000
        var minute = (milliseconds / 60).toInt()
        val hour = minute / 60
        val second = (milliseconds % 60).toInt()
        minute %= 60
        val stringBuilder = StringBuilder()
        val mFormatter = Formatter(stringBuilder, Locale.getDefault())
        return if (hour > 0) {
            mFormatter.format("%02d:%02d:%02d", hour, minute, second).toString()
        } else {
            mFormatter.format("%02d:%02d", minute, second).toString()
        }
    }

    override fun onCleared() {
        super.onCleared()
        storageHolder.storageModel = null
        storageHolder.showTitle = null
        storageHolder.posterUrl = null
    }
}

@Composable
fun ExoPlayerAttributes(exoPlayer: ExoPlayer, viewModel: VideoViewModel) {
    LaunchedEffect(exoPlayer) {
        flow {
            while (true) {
                emit(VideoInfo(exoPlayer.currentPosition, exoPlayer.duration, exoPlayer.bufferedPosition))
                delay(100)
            }
        }
            .onEach { viewModel.videoInfo = it }
            .collect()
    }

    LaunchedEffect(exoPlayer) {
        flow {
            while (true) {
                emit(exoPlayer.isPlaying)
                delay(100)
            }
        }
            .onEach { viewModel.isPlaying = it }
            .collect()
    }

    /*val playing by flow {
        while (true) {
            emit(exoPlayer.isPlaying)
            delay(100)
        }
    }
        .filter { viewModel.visibility == VideoPlayerVisibility.Visible }
        .collectAsState(initial = VideoInfo(0L, 0L))*/

    val context = LocalContext.current

    DisposableEffect(context) {
        val batteryInfo = context.battery {
            viewModel.batteryPercent = it.percent
            viewModel.batteryInformation.batteryLevel.tryEmit(it.percent)
            viewModel.batteryInformation.batteryInfo.tryEmit(it)
        }
        onDispose { context.unregisterReceiver(batteryInfo) }
    }
}

enum class VideoPlayerVisibility {
    Visible, Gone;

    operator fun not() = when (this) {
        Visible -> Gone
        Gone -> Visible
    }
}

data class TextTrackOption(val group: Tracks.Group, val label: String, val selected: Boolean)

private const val TAG = "AnimeWorldPlayer"

data class VideoInfo(val currentPosition: Long, val duration: Long, val buffered: Long)

class SSLTrustManager : X509TrustManager {
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }

    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun Context.getMediaSource(
    url: Uri,
    preview: Boolean,
    bandwidthMeter: BandwidthMeter,
    headers: Map<String, String>,
    subtitles: List<KmpSubtitle> = emptyList(),
    mimeType: String? = null,
    defaultSubtitle: Int? = null,
): MediaSource {
    val subtitleHeaders = subtitles.filter { it.headers.isNotEmpty() }.associate { it.url to it.headers }
    val httpFactory = getHttpDataSourceFactory(preview, bandwidthMeter, headers)
    // Subtitle-specific headers (e.g. CloudStream SubtitleFile.headers) layered over the shared ones.
    // DefaultDataSource routes file:// and content:// (local sidecars) as well as http(s).
    val dataSourceFactory = if (subtitleHeaders.isEmpty()) {
        DefaultDataSource.Factory(this, httpFactory)
    } else {
        ResolvingDataSource.Factory(DefaultDataSource.Factory(this, httpFactory)) { spec ->
            subtitleHeaders[spec.uri.toString()]?.let(spec::withAdditionalHeaders) ?: spec
        }
    }
    val mediaItem = MediaItem.Builder()
        .setUri(url)
        .setMimeType(
            when (mimeType?.lowercase()) {
                "application/x-mpegurl" -> MimeTypes.APPLICATION_M3U8
                "application/dash+xml" -> MimeTypes.APPLICATION_MPD
                "video/mp4" -> MimeTypes.VIDEO_MP4
                else -> null
            }
        )
        .setSubtitleConfigurations(
            subtitles.mapIndexed { index, it ->
                MediaItem.SubtitleConfiguration.Builder(it.url.toUri())
                    .setMimeType(subtitleMime(it.url))
                    .setLanguage(it.lang)
                    .setLabel(it.lang)
                    // Unflagged sidecar tracks whose language doesn't match a preference are never auto-selected.
                    .setSelectionFlags(if (index == defaultSubtitle) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
        )
        .build()
    // Only DefaultMediaSourceFactory merges sidecar subtitle configurations.
    return DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun Context.getDataSourceFactory(
    preview: Boolean,
    bandwidthMeter: BandwidthMeter,
    headers: Map<String, String>,
): DataSource.Factory =
    DefaultDataSource.Factory(
        this,
        getHttpDataSourceFactory(preview, bandwidthMeter, headers)
    )

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun Context.getHttpDataSourceFactory(
    preview: Boolean,
    bandwidthMeter: BandwidthMeter,
    headers: Map<String, String>,
): DataSource.Factory = DefaultHttpDataSource.Factory().apply {
    if (headers.keys.none { it.equals("user-agent", ignoreCase = true) }) {
        setUserAgent(Util.getUserAgent(this@getHttpDataSourceFactory, "AnimeWorld"))
    }
    //setTransferListener(if (preview) null else bandwidthMeter)
    setDefaultRequestProperties(headers)
}
