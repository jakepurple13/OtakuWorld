package com.programmersbox.animeworld.videoplayer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.navigation.NavController
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.battery
import com.programmersbox.models.ChapterModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.BatteryInformation
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.X509TrustManager

class VideoViewModel(
    handle: SavedStateHandle,
    genericInfo: GenericInfo,
    context: Context
) : ViewModel() {

    companion object {
        const val VideoPlayerRoute =
            "video_player?showPath={showPath}&showName={showName}&downloadOrStream={downloadOrStream}&referer={referer}"

        fun navigateToVideoPlayer(
            navController: NavController,
            showPath: String,
            showName: String,
            downloadOrStream: Boolean,
            referer: String
        ) {
            navController.navigate(
                "video_player?showPath=$showPath&showName=$showName&downloadOrStream=$downloadOrStream&referer=$referer"
            ) { launchSingleTop = true }
        }
    }

    val chapterModel: ChapterModel? = handle.get<String>("chapterModel")
        ?.fromJson(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
    val showPath = handle.get<String>("showPath").orEmpty()
    val showName = handle.get<String>("showName")
    val downloadOrStream = handle.get<String>("downloadOrStream")?.toBoolean() ?: true
    val headers = handle.get<String>("referer") ?: chapterModel?.url ?: ""

    var exoPlayer: ExoPlayer? by mutableStateOf(null)

    fun playPause() {
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.pause()
            quickSeekAction = QuickSeekAction.pause()
        } else {
            exoPlayer?.play()
            quickSeekAction = QuickSeekAction.play()
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
    var batteryIcon by mutableStateOf(BatteryInformation.BatteryViewType.UNKNOWN)
    var batteryPercent by mutableFloatStateOf(0f)
    val batteryInformation by lazy { BatteryInformation(context) }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            batteryInformation.composeSetupFlow(
                Color.White
            ) {
                batteryColor = it.first
                batteryIcon = it.second
            }
        }
    }

    val currentTime get() = stringForTime(videoInfo.currentPosition)
    val totalTime get() = stringForTime(videoInfo.duration)

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
    header: String
): MediaSource =
    when (Util.inferContentType(url.lastPathSegment.orEmpty().toUri())) {
        C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(
            getHttpDataSourceFactory(
                preview,
                bandwidthMeter,
                header
            )
        )//.createMediaSource(url)
        C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(
            getHttpDataSourceFactory(
                preview,
                bandwidthMeter,
                header
            )
        )//.createMediaSource(uri)
        C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(
            getHttpDataSourceFactory(
                preview,
                bandwidthMeter,
                header
            )
        )//.createMediaSource(uri)
        else -> DefaultMediaSourceFactory(getHttpDataSourceFactory(preview, bandwidthMeter, header))
    }.createMediaSource(MediaItem.fromUri(url))

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun Context.getDataSourceFactory(
    preview: Boolean,
    bandwidthMeter: BandwidthMeter,
    header: String
): DataSource.Factory =
    DefaultDataSource.Factory(
        this,
        getHttpDataSourceFactory(preview, bandwidthMeter, header)
    )

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun Context.getHttpDataSourceFactory(
    preview: Boolean,
    bandwidthMeter: BandwidthMeter,
    header: String
): DataSource.Factory = DefaultHttpDataSource.Factory().apply {
    setUserAgent(Util.getUserAgent(this@getHttpDataSourceFactory, "AnimeWorld"))
    //setTransferListener(if (preview) null else bandwidthMeter)
    //header?.let { setDefaultRequestProperties(hashMapOf("Referer" to it)) }
    setDefaultRequestProperties(
        mapOf(
            "referer" to header,
            "accept" to "*/*",
            "sec-ch-ua" to "\"Chromium\";v=\"91\", \" Not;A Brand\";v=\"99\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-fetch-user" to "?1",
            "sec-fetch-mode" to "navigate",
            "sec-fetch-dest" to "video"
        )
    )
}