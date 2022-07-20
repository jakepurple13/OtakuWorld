package com.programmersbox.animeworld.videoplayer

import android.content.pm.ActivityInfo
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.programmersbox.animeworld.ignoreSsl
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.LifecycleHandle
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerUi() {

    val context = LocalContext.current

    LifecycleHandle(
        onStop = {
            BaseMainActivity.showNavBar = true
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        },
        onDestroy = {
            BaseMainActivity.showNavBar = true
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        },
        onCreate = {
            BaseMainActivity.showNavBar = false
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        },
        onStart = {
            BaseMainActivity.showNavBar = false
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        },
        onResume = {
            BaseMainActivity.showNavBar = false
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    )

    val genericInfo = LocalGenericInfo.current

    val viewModel: VideoViewModel = viewModel { VideoViewModel(createSavedStateHandle(), genericInfo, context) }

    viewModel.exoPlayer?.let { ExoPlayerAttributes(exoPlayer = it, viewModel = viewModel) }

    Scaffold(
        topBar = { VideoTopBar(viewModel, viewModel.visibility == VideoPlayerVisibility.Visible) },
        bottomBar = {
            VideoBottomBar(
                visible = viewModel.visibility == VideoPlayerVisibility.Visible,
                currentTime = viewModel.currentTime,
                totalTime = viewModel.totalTime,
                totalDuration = viewModel.videoInfo.duration,
                currentPosition = viewModel.videoInfo.currentPosition,
                isPlaying = viewModel.exoPlayer?.isPlaying ?: false,
                playPauseToggle = viewModel::playPause,
                seekTo = { viewModel.exoPlayer?.seekTo(it) },
                rewind = { viewModel.exoPlayer?.let { p -> p.seekTo(p.currentPosition - 30000) } },
                fastForward = { viewModel.exoPlayer?.let { p -> p.seekTo(p.currentPosition + 30000) } }
            )
        }
    ) {
        Box {
            VideoPlayer(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { viewModel.visibility = !viewModel.visibility },
                        onDoubleClick = viewModel::playPause,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                source = remember {
                    if (viewModel.downloadOrStream) {
                        val dataSourceFactory = DefaultDataSource.Factory(context)
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(viewModel.showPath.toUri()))
                    } else {
                        //stream
                        if (runBlocking { context.ignoreSsl.first() }) {
                            val sslContext: SSLContext = SSLContext.getInstance("TLS")
                            sslContext.init(null, arrayOf(SSLTrustManager()), SecureRandom())
                            sslContext.createSSLEngine()
                            HttpsURLConnection.setDefaultHostnameVerifier { _: String, _: SSLSession -> true }
                            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
                        }

                        context.getMediaSource(viewModel.showPath.toUri(), false, DefaultBandwidthMeter.Builder(context).build(), viewModel.headers)
                    }
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    viewModel: VideoViewModel,
    source: MediaSource,
) {

    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
            .apply {
                setMediaSource(source)
                prepare()
            }
            .also { viewModel.exoPlayer = it }
    }

    exoPlayer.playWhenReady = true
    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

    DisposableEffect(
        AndroidView(
            modifier = modifier,
            factory = {
                exoPlayer.setMediaSource(source)
                exoPlayer.prepare()
                PlayerView(context).apply {
                    hideController()
                    useController = false

                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
            }
        )
    ) { onDispose { exoPlayer.release() } }
}

@Composable
fun VideoTopBar(viewModel: VideoViewModel, visible: Boolean) {
    val navController = LocalNavController.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        SmallTopAppBar(
            title = { Text(viewModel.showName.orEmpty()) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .5f))
        )
    }
}

@Composable
fun VideoBottomBar(
    visible: Boolean,
    currentTime: String,
    totalTime: String,
    currentPosition: Long,
    totalDuration: Long,
    isPlaying: Boolean,
    playPauseToggle: () -> Unit,
    seekTo: (Long) -> Unit,
    rewind: () -> Unit,
    fastForward: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp - 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        currentTime,
                        modifier = Modifier.weight(1f, true),
                        textAlign = TextAlign.Center
                    )

                    var isSeeking by remember { mutableStateOf(false) }

                    var seekChange by remember(isSeeking, if (isSeeking) Unit else currentPosition) {
                        mutableStateOf(currentPosition.coerceAtLeast(0L).toFloat())
                    }

                    Slider(
                        value = seekChange,
                        onValueChange = {
                            isSeeking = true
                            seekChange = it
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            seekTo(seekChange.toLong())
                        },
                        valueRange = 0f..totalDuration.toFloat(),
                        modifier = Modifier.weight(8f, true)
                    )

                    Text(
                        totalTime,
                        modifier = Modifier.weight(1f, true),
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = rewind) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = playPauseToggle) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = fastForward) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun VideoPlayerPreview() {
    MaterialTheme(darkColorScheme()) {
        VideoPlayerUi()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
fun BottomBarPreview() {
    MaterialTheme(darkColorScheme()) {
        Scaffold(
            bottomBar = {
                VideoBottomBar(
                    visible = true,
                    currentTime = "00:00",
                    totalTime = "00:00",
                    currentPosition = 0L,
                    totalDuration = 100L,
                    isPlaying = true,
                    playPauseToggle = {},
                    seekTo = {},
                    rewind = {},
                    fastForward = {}
                )
            }
        ) {

        }
    }
}