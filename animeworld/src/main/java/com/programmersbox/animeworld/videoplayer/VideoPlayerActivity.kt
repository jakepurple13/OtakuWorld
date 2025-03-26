package com.programmersbox.animeworld.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.animeworld.R
import com.programmersbox.animeworld.databinding.ActivityVideoPlayerBinding
import com.programmersbox.animeworld.databinding.ExoControlsBinding
import com.programmersbox.animeworld.databinding.MxMobileBrightnessDialogBinding
import com.programmersbox.animeworld.databinding.MxMobileVolumeDialogBinding
import com.programmersbox.animeworld.databinding.MxProgressDialogBinding
import com.programmersbox.animeworld.ignoreSsl
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.audioManager
import com.programmersbox.helpfulutils.battery
import com.programmersbox.helpfulutils.enableImmersiveMode
import com.programmersbox.helpfulutils.startDrawable
import com.programmersbox.models.ChapterModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.BatteryInformation
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import com.programmersbox.uiviews.utils.toolTipText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.security.cert.X509Certificate
import java.util.Formatter
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager
import kotlin.math.abs
import kotlin.math.roundToInt

@UnstableApi
class VideoPlayerActivity : AppCompatActivity() {

    private var currentVolume: Int = 0

    private var currentPos: Long = 0

    private lateinit var player: ExoPlayer

    private var locked = false
        set(value) {
            field = value
            runOnUiThread { exoBinding.videoLock.setText(if (locked) R.string.locked else R.string.unlocked) }
        }

    private lateinit var gesture: GestureDetector

    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()
    private var mChangeLight: Boolean = false
    private var mChangeVolume: Boolean = false
    private var mGestureDownVolume: Int = 0
    private var mGestureDownBrightness: Int = 0

    private var mScreenWidth: Int = 0
    private var mScreenHeight: Int = 0

    private val mAudioManager: AudioManager by lazy { audioManager }

    @Suppress("PrivatePropertyName")
    private val THRESHOLD = 70

    private val mVolumeDialog: Dialog by lazy {
        Dialog(this, R.style.mx_style_dialog_progress).apply {
            setContentView(volumeBinding.root)
            window!!.addFlags(8)
            window!!.addFlags(32)
            window!!.addFlags(16)
            window!!.setLayout(-2, -2)
            val params = window!!.attributes
            params.gravity = Gravity.CENTER or Gravity.TOP
            params.y = resources.getDimensionPixelOffset(R.dimen.mx_volume_dialog_margin_top)
            params.width = resources.getDimensionPixelOffset(R.dimen.mx_mobile_dialog_width)
            window!!.attributes = params
        }
    }
    private val volumeBinding by lazy { MxMobileVolumeDialogBinding.inflate(layoutInflater) }

    private val mBrightnessDialog: Dialog by lazy {
        Dialog(this, R.style.mx_style_dialog_progress).apply {
            setContentView(brightBinding.root)
            window!!.addFlags(8)
            window!!.addFlags(32)
            window!!.addFlags(16)
            window!!.setLayout(-2, -2)
            val params = window!!.attributes
            params.gravity = Gravity.CENTER or Gravity.TOP
            params.y = resources.getDimensionPixelOffset(R.dimen.mx_volume_dialog_margin_top)
            params.width = resources.getDimensionPixelOffset(R.dimen.mx_mobile_dialog_width)
            window!!.attributes = params
        }
    }
    private val brightBinding by lazy { MxMobileBrightnessDialogBinding.inflate(layoutInflater) }

    private val mProgressDialog: Dialog by lazy {
        Dialog(this, R.style.mx_style_dialog_progress).apply {
            setContentView(progressBinding.root)
            window!!.addFlags(Window.FEATURE_ACTION_BAR)
            window!!.addFlags(32)
            window!!.addFlags(16)
            window!!.setLayout(-2, -2)

            val params = window!!.attributes
            params.gravity = Gravity.CENTER or Gravity.TOP
            params.y = resources.getDimensionPixelOffset(R.dimen.mx_progress_dialog_margin_top)
            params.width = resources.getDimensionPixelOffset(R.dimen.mx_mobile_dialog_width)
            window!!.attributes = params
        }
    }
    private val progressBinding by lazy { MxProgressDialogBinding.inflate(layoutInflater) }

    private var mChangePosition: Boolean = false
    private var mTouchingProgressBar = false
    private var mDownPosition: Int = 0
    private var mSeekTimePosition: Int = 0

    private val playerView: PlayerView by lazy { videoBinding.playerView }

    private val retriever = MediaMetadataRetriever()
    private var showPath: String? = null

    private val genericInfo: GenericInfo by inject()

    private val chapterModel: ChapterModel? by lazy {
        intent.getStringExtra("chapterModel")
            ?.fromJson(ChapterModel::class.java to ChapterModelDeserializer())
    }

    private lateinit var videoBinding: ActivityVideoPlayerBinding
    private lateinit var exoBinding: ExoControlsBinding

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoBinding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(videoBinding.root)
        enableImmersiveMode()
        exoBinding = ExoControlsBinding.bind(videoBinding.root.findViewById(R.id.exo_controls))
        exoBinding.videoBack.setOnClickListener { finish() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            exoBinding.videoBack.toolTipText(R.string.videoPlayerBack)
            exoBinding.exoFfwd.toolTipText(R.string.videoPlayerFF)
            exoBinding.exoRew.toolTipText(R.string.videoPlayerRewind)
            exoBinding.videoLock.toolTipText(R.string.videoPlayerLockUnlock)
        }

        //exoBinding.backThirty.onAnimationStart = { playerView.player?.let { p -> p.seekTo(p.currentPosition - 30000) } }
        //exoBinding.forwardThirty.onAnimationStart = { playerView.player?.let { p -> p.seekTo(p.currentPosition + 30000) } }

        videoBinding.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { exoBinding.videoInfoLayout.visibility = it }
        )

        /*if (args.showPath.isEmpty()) {
            finish()
        }*/

        showPath = intent.data?.toString() ?: intent.getStringExtra("showPath")
        val showName = intent.getStringExtra("showName")
        val downloadOrStream = intent.getBooleanExtra("downloadOrStream", true)

        exoBinding.videoName.text = showName

        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        mScreenWidth = this.resources.displayMetrics.widthPixels
        mScreenHeight = this.resources.displayMetrics.heightPixels

        player = ExoPlayer.Builder(this).build()

        player.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    //player back ended
                    finish()
                }
            }
        })

        try {
            retriever.setDataSource(this@VideoPlayerActivity, showPath?.toUri())
            /*var lastPos = 0L
            exoBinding.exoProgress.setPreviewLoader { currentPosition, _ ->
                if (abs(lastPos - currentPosition) > 1000) {
                    exoBinding.imageView.setImageBitmap(retriever.getFrameAtTime(currentPosition * 1000))
                }
                lastPos = currentPosition
            }*/
        } catch (e: Exception) {
            //exoBinding.exoProgress.isPreviewEnabled = false
        }

        /*exoBinding.exoProgress.addOnScrubListener(object : PreviewBar.OnScrubListener {
            override fun onScrubStart(previewBar: PreviewBar?) {
                player.playWhenReady = false
            }

            override fun onScrubMove(previewBar: PreviewBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onScrubStop(previewBar: PreviewBar?) {
                player.playWhenReady = true
            }

        })*/

        playerView.player = player

        //download
        val source = if (downloadOrStream) {
            val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "AnimeWorld"))
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(showPath!!.toUri()))
        } else {
            //stream
            //fun buildMediaSource(uri: Uri): MediaSource =
            //ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory("exoplayer-codelab")).createMediaSource(uri)

            if (runBlocking { ignoreSsl.first() }) {
                val sslContext: SSLContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(SSLTrustManager()), java.security.SecureRandom())
                sslContext.createSSLEngine()
                HttpsURLConnection.setDefaultHostnameVerifier { _: String, _: SSLSession -> true }
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            }

            val headers = intent.getStringExtra("referer") ?: chapterModel?.url ?: ""
            getMediaSource(showPath!!.toUri(), false, headers)
        }
        player.setMediaSource(source, true)
        player.prepare()
        player.play()
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        playerView.controllerAutoShow = true
        //playerView.controllerHideOnTouch = true
        playerView.controllerShowTimeoutMs = 2000
        playerView.player!!.playWhenReady = true

        //video_lock.icon = IconicsDrawable(this).icon(FontAwesome.Icon.faw_unlock).sizeDp(24)
        //video_lock.icon = IconicsDrawable(this).icon(FontAwesome.Icon.faw_unlock).size { IconicsSize.dp(24) }
        exoBinding.videoLock.setOnClickListener {
            locked = !locked

            //video_lock.setImageDrawable(IconicsDrawable(this).icon(if (locked) FontAwesome.Icon.faw_lock else FontAwesome.Icon.faw_unlock).sizeDp(24))
            exoBinding.videoLock.icon =
                ContextCompat.getDrawable(this, if (locked) R.drawable.ic_baseline_lock_24 else R.drawable.ic_baseline_lock_open_24)
            if (!locked)
                playerView.showController()
        }

        initVideoPlayer()

        val pos = getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(showPath, 0)
        playerView.player!!.seekTo(pos)

        batterySetup()

    }

    private var batteryInfo: BroadcastReceiver? = null

    private val batteryInformation by lazy { BatteryInformation(this) }

    @SuppressLint("SetTextI18n")
    private fun batterySetup() {

        exoBinding.batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = Color.WHITE
            sizePx = exoBinding.batteryInformation.textSize.roundToInt()
        }

        lifecycleScope.launch {
            batteryInformation.setupFlow(
                size = exoBinding.batteryInformation.textSize.roundToInt(),
                normalBatteryColor = Color.WHITE
            ) {
                it.second.colorInt = it.first
                exoBinding.batteryInformation.startDrawable = it.second
                exoBinding.batteryInformation.setTextColor(it.first)
                exoBinding.batteryInformation.startDrawable?.setTint(it.first)
            }
        }

        batteryInfo = battery {
            exoBinding.batteryInformation.text = "${it.percent.toInt()}%"
            batteryInformation.batteryLevel.tryEmit(it.percent)
            batteryInformation.batteryInfo.tryEmit(it)
        }
    }

    override fun onDestroy() {
        playerView.player?.currentPosition?.also {
            currentPos = it
            showPath?.let { path -> getSharedPreferences("videos", Context.MODE_PRIVATE).edit().putLong(path, it).apply() }
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
        unregisterReceiver(batteryInfo)
        super.onDestroy()
    }

    override fun onStop() {
        try {
            playerView.player!!.playWhenReady = false
            playerView.player!!.release()
        } catch (e: IllegalStateException) {

        }
        retriever.release()
        super.onStop()
    }

    override fun onBackPressed() {
        currentPos = playerView.player!!.currentPosition
        playerView.player!!.release()
        super.onBackPressed()
    }

    private fun initVideoPlayer() {
        gesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {})
        gesture.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(p0: MotionEvent): Boolean {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                /*if (exoBinding.exoPlay.visibility == View.GONE)
                    exoBinding.exoPause.performClick()
                else
                    exoBinding.exoPlay.performClick()*/
                return true
            }

            override fun onDoubleTapEvent(p0: MotionEvent): Boolean {
                return false
            }

            override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
                return false
            }

        })
        playerView.setOnTouchListener(onTouch)
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouch = View.OnTouchListener { _, event ->
        //Loged.v("$event")
        if (!locked) {
            gesture.onTouchEvent(event!!)
            val x = event.x
            val y = event.y
            //val id = v.id
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    //Loged.i("onTouch: surfaceContainer actionDown [" + this.hashCode() + "] ")
                    mTouchingProgressBar = true
                    mChangePosition = false
                    mDownX = x
                    mDownY = y
                    mChangeLight = false
                    mChangeVolume = false
                }
                MotionEvent.ACTION_MOVE -> {
                    //Loged.i("onTouch: surfaceContainer actionMove [" + this.hashCode() + "] ")
                    val deltaX = x - mDownX
                    var deltaY = y - mDownY
                    val absDeltaX = abs(deltaX)
                    val absDeltaY = abs(deltaY)
                    if (!mChangePosition && !mChangeVolume && !mChangeLight) {
                        if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                            //cancelProgressTimer()
                            if (absDeltaX >= THRESHOLD) { // adjust progress
                                //if (mCurrentState != CURRENT_STATE_ERROR) {
                                mChangePosition = true
                                mDownPosition = playerView.player!!.currentPosition.toInt()//getCurrentPositionWhenPlaying()
                                //}
                            } else {
                                if (x <= playerView.videoSurfaceView!!.width / 2) {  // adjust the volume
                                    mChangeVolume = true
                                    mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                } else {  // adjust the light
                                    mChangeLight = true
                                    mGestureDownBrightness = getScreenBrightness(this)
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        val totalTimeDuration = playerView.player!!.duration//getDuration()
                        mSeekTimePosition = (mDownPosition + deltaX * 100).toInt()
                        if (mSeekTimePosition > totalTimeDuration) {
                            mSeekTimePosition = totalTimeDuration.toInt()
                        }
                        val seekTime = stringForTime(mSeekTimePosition.toLong())
                        val totalTime = stringForTime(totalTimeDuration)
                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration.toInt())
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY  // up is -, down is +
                        val maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val deltaV = (maxVolume.toFloat() * deltaY * 3f / mScreenHeight).toInt()
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0)
                        val volumePercent = (mGestureDownVolume * 100 / maxVolume + deltaY * 3f * 100f / mScreenHeight).toInt()
                        showVolumeDialog(-deltaY, volumePercent)
                    }
                    if (mChangeLight) {
                        deltaY = -deltaY  // up is -, down is +
                        val deltaV = (255f * deltaY * 3f / mScreenHeight).toInt()
                        val brightnessValue = mGestureDownBrightness + deltaV
                        if (brightnessValue in 0..255) {
                            setWindowBrightness(this@VideoPlayerActivity, brightnessValue.toFloat())
                        }
                        val brightnessPercent = (mGestureDownBrightness + deltaY * 255f * 3f / mScreenHeight).toInt()
                        showBrightnessDialog(-deltaY, brightnessPercent)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    //Loged.i("onTouch: surfaceContainer actionUp [" + this.hashCode() + "] ")
                    dismissProgressDialog()
                    dismissVolumeDialog()
                    dismissBrightnessDialog()
                    if (mChangePosition) {
                        //onActionEvent(MxUserAction.ON_TOUCH_SCREEN_SEEK_POSITION)
                        //val duration = playerView.player.duration
                        //val progress = mSeekTimePosition * 100 / if (duration == 0L) 1 else duration
                        playerView.player!!.seekTo(mSeekTimePosition.toLong())
                        //mProgressBar.setProgress(progress)
                    }
                    if (mChangeVolume) {
                        //onActionEvent(MxUserAction.ON_TOUCH_SCREEN_SEEK_VOLUME)
                    }
                    if (mChangeLight) {
                        //onActionEvent(MxUserAction.ON_TOUCH_SCREEN_SEEK_BRIGHTNESS)
                    }
                    //startProgressTimer()
                }
                else -> {
                }
            }
        }

        //playerView.useController = !locked
        false
    }

    private fun showProgressDialog(
        deltaX: Float, seekTime: String,
        seekTimePosition: Int, totalTime: String, totalTimeDuration: Int
    ) {
        if (!mProgressDialog.isShowing) mProgressDialog.show()
        val seekedTime = abs(playerView.player!!.currentPosition - seekTimePosition)
        val seekTimeText = "(" + stringForTime(seekedTime) + ") " + seekTime
        progressBinding.videoCurrent.text = seekTimeText
        progressBinding.videoDuration.text = String.format(" / %s", totalTime)
        progressBinding.durationProgressbar.progress = if (totalTimeDuration <= 0) 0 else seekTimePosition * 100 / totalTimeDuration
        progressBinding.durationImageTip.setBackgroundResource(if (deltaX > 0) R.drawable.mx_forward_icon else R.drawable.mx_backward_icon)
    }

    private fun dismissProgressDialog() {
        mProgressDialog.dismiss()
    }

    private fun showVolumeDialog(v: Float, volumePercent: Int) {
        if (!mVolumeDialog.isShowing) mVolumeDialog.show()
        volumeBinding.volumeProgressbar.progress = volumePercent
    }

    private fun dismissVolumeDialog() {
        mVolumeDialog.dismiss()
    }

    private fun showBrightnessDialog(v: Float, brightnessPercent: Int) {
        if (!mBrightnessDialog.isShowing) mBrightnessDialog.show()
        brightBinding.brightnessProgressbar.progress = brightnessPercent
    }

    private fun dismissBrightnessDialog() {
        mBrightnessDialog.dismiss()
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

    private fun getScreenBrightness(activity: Activity): Int {
        var nowBrightnessValue = 0
        val resolver = activity.contentResolver
        try {
            nowBrightnessValue = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return nowBrightnessValue
    }

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

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(this).build() }

    private fun getMediaSource(url: Uri, preview: Boolean, header: String): MediaSource =
        when (Util.inferContentType(url.lastPathSegment.toString())) {
            //C.TYPE_SS -> SsMediaSource.Factory(getHttpDataSourceFactory(preview, header))//.createMediaSource(url)
            C.TYPE_DASH -> DashMediaSource.Factory(getHttpDataSourceFactory(preview, header))//.createMediaSource(url)
            C.TYPE_HLS -> HlsMediaSource.Factory(getHttpDataSourceFactory(preview, header))//.createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(getHttpDataSourceFactory(preview, header))//.createMediaSource(uri)
            else -> DefaultMediaSourceFactory(getHttpDataSourceFactory(preview, header))
        }.createMediaSource(MediaItem.fromUri(url))

    private fun getDataSourceFactory(preview: Boolean, header: String): DataSource.Factory = DefaultDataSourceFactory(
        this, if (preview) null else bandwidthMeter,
        getHttpDataSourceFactory(preview, header)
    )

    private fun getHttpDataSourceFactory(preview: Boolean, header: String): DataSource.Factory = DefaultHttpDataSource.Factory().apply {
        setUserAgent(Util.getUserAgent(this@VideoPlayerActivity, "AnimeWorld"))
        setTransferListener(if (preview) null else bandwidthMeter)
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

    class SSLTrustManager : X509TrustManager {
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

}