package com.programmersbox.animeworld.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.programmersbox.animeworld.StorageHolder
import com.programmersbox.animeworld.ignoreSsl
import com.programmersbox.helpfulutils.audioManager
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.components.AirBar
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.programmersbox.uiviews.utils.LifecycleHandle
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import java.security.SecureRandom
import java.util.Locale
import java.util.Objects
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import kotlin.math.abs

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerUi(
    context: Context = LocalContext.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    storageHolder: StorageHolder = koinInject(),
    viewModel: VideoViewModel = viewModel { VideoViewModel(createSavedStateHandle(), context, storageHolder) },
) {
    val activity = LocalActivity.current

    val audioManager = remember { context.audioManager }
    val originalAudioLevel = remember { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }
    val originalScreenBrightness = remember { getScreenBrightness(context) }

    HideSystemBarsWhileOnScreen()

    LifecycleHandle(
        onStop = {
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalAudioLevel, 0)
            activity?.let { setWindowBrightness(it, originalScreenBrightness.toFloat()) }
        },
        onDestroy = {
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalAudioLevel, 0)
            activity?.let { setWindowBrightness(it, originalScreenBrightness.toFloat()) }
        },
        onCreate = {
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        },
        onStart = {
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        },
        onResume = {
            context.findActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    )
    viewModel.exoPlayer?.let { ExoPlayerAttributes(exoPlayer = it, viewModel = viewModel) }

    val overlayVisibility = viewModel.visibility == VideoPlayerVisibility.Visible || !viewModel.isPlaying

    Scaffold(
        topBar = { VideoTopBar(viewModel, overlayVisibility) },
        bottomBar = {
            VideoBottomBar(
                visible = overlayVisibility,
                currentTime = viewModel.currentTime,
                totalTime = viewModel.totalTime,
                totalDuration = viewModel.videoInfo.duration,
                currentPosition = viewModel.videoInfo.currentPosition,
                isPlaying = viewModel.isPlaying,
                playPauseToggle = viewModel::playPause,
                seekTo = { viewModel.exoPlayer?.seekTo(it) },
                rewind = viewModel::rewind,
                fastForward = viewModel::fastForward
            )
        }
    ) { _ ->
        Box {
            VideoPlayer(
                source = remember {
                    if (viewModel.downloadOrStream) {
                        //download
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
            var draggingProgress: DraggingProgress? by remember { mutableStateOf(null) }
            MediaControlGestures(
                modifier = Modifier.fillMaxSize(),
                visible = true,
                enabled = true,
                gesturesEnabled = true,
                quickSeekDirection = viewModel.quickSeekAction.direction,
                onQuickSeekDirectionChange = { viewModel.quickSeekAction = it },
                draggingProgress = draggingProgress,
                onDraggingProgressChange = { draggingProgress = it },
                viewModel = viewModel
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    viewModel: VideoViewModel,
    source: MediaSource,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaSource(source)
                prepare()
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                repeatMode = Player.REPEAT_MODE_ONE
            }
            .also { viewModel.exoPlayer = it }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            exoPlayer.setMediaSource(source)
            exoPlayer.prepare()

            exoPlayer.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }
            )

            val pos = context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(viewModel.showPath, 0)
            exoPlayer.seekTo(pos)
            PlayerView(context).apply {
                hideController()
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        }
    )
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.currentPosition.also {
                viewModel.showPath.let { path -> context.getSharedPreferences("videos", Context.MODE_PRIVATE).edit().putLong(path, it).apply() }
            }
            exoPlayer.release()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTopBar(viewModel: VideoViewModel, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        TopAppBar(
            title = { Text(viewModel.showName.orEmpty()) },
            navigationIcon = { BackButton() },
            actions = {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        viewModel.batteryIcon.composeIcon,
                        contentDescription = null,
                        tint = animateColorAsState(
                            if (viewModel.batteryColor == Color.White) MaterialTheme.colorScheme.onSurface
                            else viewModel.batteryColor, label = ""
                        ).value
                    )
                    Text(
                        "${viewModel.batteryPercent.toInt()}%",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .5f))
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
    fastForward: () -> Unit,
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
                modifier = Modifier.padding(horizontal = 4.dp)
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

                    var seekChange by remember(
                        isSeeking,
                        if (isSeeking) Unit else currentPosition
                    ) { mutableFloatStateOf(currentPosition.toFloat()) }

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
                        valueRange = 0f..totalDuration.coerceAtLeast(0L).toFloat(),
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
        ) { Box(modifier = Modifier.padding(it)) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MediaControlGestures(
    visible: Boolean,
    enabled: Boolean,
    gesturesEnabled: Boolean,
    quickSeekDirection: QuickSeekDirection,
    onQuickSeekDirectionChange: (QuickSeekAction) -> Unit,
    draggingProgress: DraggingProgress?,
    onDraggingProgressChange: (DraggingProgress?) -> Unit,
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier,
) {
    if (enabled && visible && gesturesEnabled) {
        Box(
            modifier = modifier
                .draggingProgressOverlay(draggingProgress)
                .quickSeekAnimation(quickSeekDirection) { onQuickSeekDirectionChange(QuickSeekAction.none()) }
        ) {
            val scope = rememberCoroutineScope()

            var job: Job? = remember { null }
            var showVolume by remember { mutableStateOf(false) }
            var volumeLevel by remember { mutableIntStateOf(0) }

            var showBrightness by remember { mutableStateOf(false) }
            var brightnessLevel by remember { mutableIntStateOf(0) }

            var visibilityJob: Job? = remember { null }

            LaunchedEffect(viewModel.visibility) {
                if (viewModel.visibility == VideoPlayerVisibility.Visible && viewModel.isPlaying) {
                    visibilityJob?.cancel()
                    visibilityJob = scope.launch {
                        delay(2500)
                        viewModel.visibility = VideoPlayerVisibility.Gone
                    }
                }
            }

            GestureBox(
                doubleTap = viewModel::playPause,
                draggingProgress = onDraggingProgressChange,
                onTap = { viewModel.visibility = !viewModel.visibility },
                onHorizontalDragStart = { viewModel.exoPlayer?.pause() },
                onHorizontalDragEnd = { viewModel.exoPlayer?.play() },
                onVerticalDragStart = {},
                onVerticalDragEnd = {
                    job?.cancel()
                    job = scope.launch {
                        delay(1000)
                        showVolume = false
                        showBrightness = false
                    }
                },
                onVerticalDragLeft = { brightness ->
                    brightnessLevel = brightness.coerceIn(0, 100)
                    showBrightness = true
                },
                onVerticalDragRight = { volume ->
                    volumeLevel = (volume * 4).coerceIn(0, 100)
                    showVolume = true
                },
                onSeek = {
                    viewModel.exoPlayer?.seekTo(it)
                    viewModel.videoInfo = VideoInfo(
                        viewModel.exoPlayer?.currentPosition ?: 0,
                        viewModel.exoPlayer?.duration ?: 0,
                        viewModel.exoPlayer?.bufferedPosition ?: 0
                    )
                },
                viewModel = viewModel
            )

            AnimatedVisibility(
                visible = showBrightness,
                enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterStart)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$brightnessLevel%",
                        color = MaterialTheme.colorScheme.primary
                    )
                    AirBar(
                        progress = brightnessLevel.toFloat(),
                        valueChanged = {},
                        backgroundColor = Color.Black.copy(alpha = .4f),
                        fillColor = Color.White,
                        modifier = Modifier.size(80.dp, 175.dp),
                        icon = { Icon(Icons.Default.BrightnessHigh, null, tint = MaterialTheme.colorScheme.primary) },
                    )
                }
            }

            AnimatedVisibility(
                visible = showVolume,
                enter = fadeIn() + expandIn(expandFrom = Alignment.CenterEnd),
                exit = shrinkOut(shrinkTowards = Alignment.CenterEnd) + fadeOut(),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterEnd)
            ) {
                val context = LocalContext.current
                val audioManager = remember { context.audioManager }
                val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$volumeLevel%",
                        color = MaterialTheme.colorScheme.primary
                    )
                    AirBar(
                        progress = volumeLevel.toFloat(),
                        valueChanged = {},
                        backgroundColor = Color.Black.copy(alpha = .4f),
                        fillColor = Color.White,
                        modifier = Modifier.size(80.dp, 175.dp),
                        maxValue = maxVolume.toDouble(),
                        icon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary) },
                    )
                }
            }
        }
    }
}

@Composable
fun GestureBox(
    doubleTap: () -> Unit,
    draggingProgress: (DraggingProgress?) -> Unit,
    onTap: () -> Unit,
    onHorizontalDragStart: (Offset) -> Unit,
    onHorizontalDragEnd: () -> Unit,
    onVerticalDragStart: (Offset) -> Unit,
    onVerticalDragEnd: () -> Unit,
    onVerticalDragLeft: (Int) -> Unit,
    onVerticalDragRight: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val audioManager = remember { context.audioManager }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            var wasPlaying = true
            var totalOffset = Offset.Zero
            var diffTime = -1f

            var duration: Long = 0
            var currentPosition: Long = 0

            var mDownX = -1f
            var mDownY = -1f

            var mGestureDownBrightness = getScreenBrightness(context)

            // When this job completes, it seeks to desired position.
            // It gets cancelled if delay does not complete
            var seekJob: Job? = null

            fun resetState() {
                totalOffset = Offset.Zero
                //controller.setDraggingProgress(null)
                draggingProgress(null)
            }

            detectMediaPlayerGesture(
                onDoubleTap = { doubleTapPosition ->
                    when {
                        /*doubleTapPosition.x < size.width * 0.4f -> {
                            //controller.quickSeekRewind()
                            doubleTapStart()
                        }
                        doubleTapPosition.x > size.width * 0.6f -> {
                            //controller.quickSeekForward()
                            doubleTapEnd()
                        }*/
                        else -> doubleTap()
                    }
                },
                onTap = { onTap() },
                onVerticalDrag = { dragAmount: Float, offset ->
                    seekJob?.cancel()

                    val deltaX = offset.position.x - mDownX
                    var deltaY = offset.position.y - mDownY

                    when {
                        //brightness == start
                        offset.position.x < size.width * 0.3f -> {
                            deltaY = -deltaY
                            val deltaV = (255f * deltaY * 3f / size.height).toInt()
                            val brightnessValue = mGestureDownBrightness + deltaV
                            if (brightnessValue in 0..255) {
                                activity?.let { setWindowBrightness(it, brightnessValue.toFloat()) }
                            }
                            val brightnessPercent = (mGestureDownBrightness + deltaY * 255f * 3f / size.height).toInt()
                            seekJob = coroutineScope.launch {
                                onVerticalDragLeft(brightnessPercent)
                            }
                        }
                        //volume == end
                        offset.position.x > size.width * 0.7f -> {
                            deltaY = -deltaY
                            val mGestureDownVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val deltaV = (maxVolume.toFloat() * deltaY * 3f / size.height).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0)
                            //val volumePercent = (mGestureDownVolume * 100 / maxVolume + deltaY * 3f * 100f / size.height).toInt()

                            seekJob = coroutineScope.launch {
                                onVerticalDragRight(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                            }
                        }
                    }
                },
                onVerticalDragEnd = {
                    onVerticalDragEnd()
                    resetState()
                },
                onVerticalDragStart = {
                    onVerticalDragStart(it)
                    mDownY = it.y
                    mDownX = it.x
                    mGestureDownBrightness = getScreenBrightness(context)
                    resetState()
                },
                onDragStart = { offset ->
                    wasPlaying = viewModel.exoPlayer?.isPlaying == true
                    onHorizontalDragStart(offset)

                    currentPosition = viewModel.videoInfo.currentPosition
                    duration = viewModel.videoInfo.duration

                    resetState()
                },
                onDragEnd = {
                    if (wasPlaying) onHorizontalDragEnd()
                    resetState()
                },
                onDrag = { dragAmount: Float ->
                    seekJob?.cancel()

                    totalOffset += Offset(x = dragAmount, y = 0f)

                    val diff = totalOffset.x

                    diffTime = if (duration <= 60_000) {
                        duration.toFloat() * diff / size.width.toFloat()
                    } else {
                        60_000.toFloat() * diff / size.width.toFloat()
                    }

                    var finalTime = currentPosition + diffTime
                    if (finalTime < 0) {
                        finalTime = 0f
                    } else if (finalTime > duration) {
                        finalTime = duration.toFloat()
                    }
                    diffTime = finalTime - currentPosition

                    draggingProgress(
                        DraggingProgress(
                            finalTime = finalTime,
                            diffTime = diffTime
                        )
                    )

                    seekJob = coroutineScope.launch {
                        delay(200)
                        onSeek(finalTime.toLong())
                    }
                }
            )
        }
        .then(modifier)
    )
}

private fun setWindowBrightness(activity: Activity, brightness: Float) {
    val lp = activity.window.attributes
    lp.screenBrightness = brightness / 255.0f
    if (lp.screenBrightness > 1) {
        lp.screenBrightness = 1f
    } else if (lp.screenBrightness < 0.1) {
        lp.screenBrightness = 0.1.toFloat()
    }
    activity.window.attributes = lp
}

private fun getScreenBrightness(context: Context): Int {
    var nowBrightnessValue = 0
    val resolver = context.contentResolver
    try {
        nowBrightnessValue = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return nowBrightnessValue
}

suspend fun PointerInputScope.detectMediaPlayerGesture(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onVerticalDragStart: (Offset) -> Unit,
    onVerticalDragEnd: () -> Unit,
    onVerticalDrag: (Float, PointerInputChange) -> Unit,
) {
    coroutineScope {
        launch {
            detectHorizontalDragGestures(
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onHorizontalDrag = { change, dragAmount ->
                    onDrag(dragAmount)
                    if (change.positionChange() != Offset.Zero) change.consume()
                },
            )
        }

        launch {
            detectVerticalDragGestures(
                onDragStart = onVerticalDragStart,
                onDragEnd = onVerticalDragEnd,
                onVerticalDrag = { change, dragAmount ->
                    onVerticalDrag(dragAmount, change)
                    if (change.positionChange() != Offset.Zero) change.consume()
                },
            )
        }

        launch {
            detectTapGestures(
                onTap = onTap,
                onDoubleTap = onDoubleTap
            )
        }
    }
}

fun Modifier.quickSeekAnimation(
    quickSeekDirection: QuickSeekDirection,
    onAnimationEnd: () -> Unit,
) = composed {
    val alphaRewind = remember { Animatable(0f) }
    val alphaForward = remember { Animatable(0f) }
    val alphaPlayPause = remember { Animatable(0f) }

    LaunchedEffect(quickSeekDirection) {
        when (quickSeekDirection) {
            QuickSeekDirection.Rewind -> alphaRewind
            QuickSeekDirection.Forward -> alphaForward
            QuickSeekDirection.Play, QuickSeekDirection.Pause -> alphaPlayPause
            else -> null
        }?.let { animatable ->
            animatable.animateTo(1f)
            animatable.animateTo(0f)
            onAnimationEnd()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ShadowedIcon(
                Icons.Filled.FastRewind,
                modifier = Modifier
                    .graphicsLayer { alpha = alphaRewind.value }
                    .align(Alignment.Center)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ShadowedIcon(
                if (quickSeekDirection == QuickSeekDirection.Pause) Icons.Default.Pause else Icons.Filled.PlayArrow,
                modifier = Modifier
                    .graphicsLayer { alpha = alphaPlayPause.value }
                    .align(Alignment.Center)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ShadowedIcon(
                Icons.Filled.FastForward,
                modifier = Modifier
                    .graphicsLayer { alpha = alphaForward.value }
                    .align(Alignment.Center)
            )
        }
    }

    this
}

@Composable
fun ShadowedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
) {
    Box(modifier = modifier) {
        Icon(
            imageVector = icon,
            tint = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier
                .size(iconSize)
                .offset(2.dp, 2.dp)
                .then(modifier),
            contentDescription = null
        )
        Icon(
            imageVector = icon,
            modifier = Modifier.size(iconSize),
            contentDescription = null
        )
    }
}

fun Modifier.draggingProgressOverlay(draggingProgress: DraggingProgress?) = composed {
    if (draggingProgress != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                draggingProgress.progressText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        blurRadius = 8f,
                        offset = Offset(2f, 2f)
                    )
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
    this
}

data class DraggingProgress(
    val finalTime: Float,
    val diffTime: Float,
) {
    val progressText: String
        get() {
            val duration = getDurationString(finalTime.toLong(), false)
            val type = if (diffTime < 0) "-" else "+"
            val total = getDurationString(abs(diffTime.toLong()), false)
            return "$duration [$type$total]"
        }
}

fun getDurationString(durationMs: Long, negativePrefix: Boolean): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)

    return if (hours > 0) {
        String.format(
            Locale.getDefault(), "%s%02d:%02d:%02d",
            if (negativePrefix) "-" else "",
            hours,
            minutes - TimeUnit.HOURS.toMinutes(hours),
            seconds - TimeUnit.MINUTES.toSeconds(minutes)
        )
    } else String.format(
        Locale.getDefault(), "%s%02d:%02d",
        if (negativePrefix) "-" else "",
        minutes,
        seconds - TimeUnit.MINUTES.toSeconds(minutes)
    )
}

enum class QuickSeekDirection {
    None,
    Rewind,
    Forward,
    Play,
    Pause
}

data class QuickSeekAction(
    val direction: QuickSeekDirection,
) {
    // Each action is unique
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(direction)
    }

    companion object {
        fun none() = QuickSeekAction(QuickSeekDirection.None)
        fun forward() = QuickSeekAction(QuickSeekDirection.Forward)
        fun rewind() = QuickSeekAction(QuickSeekDirection.Rewind)
        fun play() = QuickSeekAction(QuickSeekDirection.Play)
        fun pause() = QuickSeekAction(QuickSeekDirection.Pause)
    }
}