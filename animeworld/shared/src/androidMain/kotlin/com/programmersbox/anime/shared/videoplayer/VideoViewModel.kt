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
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.helpfulutils.battery
import com.programmersbox.uiviews.utils.BatteryInformation2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.security.cert.X509Certificate
import java.util.Formatter
import java.util.Locale
import javax.net.ssl.X509TrustManager

class VideoViewModel(
    videoScreen: VideoScreen,
    context: Context,
    private val storageHolder: StorageHolder,
) : ViewModel() {

    val showPath = storageHolder.storageModel?.link ?: videoScreen.showPath
    val showName = videoScreen.showName
    val downloadOrStream = videoScreen.downloadOrStream
    val requestHeaders = playerHeaders(storageHolder.storageModel?.headers.orEmpty(), videoScreen.referer)
    val subtitles = storageHolder.storageModel?.subtitles.orEmpty()
    val mimeType = storageHolder.storageModel?.let(::streamMime)
    val defaultSubtitle = defaultSubtitleIndex(
        subtitles,
        Locale.getDefault().language,
        Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
    )

    /** Text tracks the player currently exposes; drives the CC menu. */
    var textTracks by mutableStateOf<List<TextTrackOption>>(emptyList())
    var subtitleMenuOpen by mutableStateOf(false)

    fun updateTextTracks(tracks: Tracks) {
        textTracks = tracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .map { group ->
                val format = group.getTrackFormat(0)
                TextTrackOption(group, format.label ?: format.language ?: "Unknown", group.isSelected)
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
    val dataSourceFactory = if (subtitleHeaders.isEmpty()) {
        httpFactory
    } else {
        ResolvingDataSource.Factory(httpFactory) { spec ->
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
