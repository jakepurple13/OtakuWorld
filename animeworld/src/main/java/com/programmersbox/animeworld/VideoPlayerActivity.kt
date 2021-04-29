package com.programmersbox.animeworld

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.exoplayer.PreviewTimeBar
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.button.MaterialButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.helpfulutils.*
import com.programmersbox.rxutils.invoke
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.uiviews.utils.batteryAlertPercent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL as IMAGE_ORIGINAL_SIZE

class VideoPlayerActivity : AppCompatActivity() {

    //private val args by navArgs<VideoPlayerActivityArgs>()

    private var currentVolume: Int = 0

    private var currentPos: Long = 0

    private lateinit var player: SimpleExoPlayer

    private var locked = false
        set(value) {
            field = value
            runOnUiThread { findViewById<MaterialButton>(R.id.video_lock).setText(if (locked) R.string.locked else R.string.unlocked) }
        }

    private lateinit var gesture: GestureDetector

    private var lockTimer = TimerStuff {
        /*video_info_layout.animate().setDuration(500).alpha(0f).withEndAction {
            topShowing.set(false)
        }*/
    }

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

    private var mVolumeDialog: Dialog? = null
    private var mDialogVolumeProgressBar: ProgressBar? = null

    private var mBrightnessDialog: Dialog? = null
    private lateinit var mDialogBrightnessProgressBar: ProgressBar

    private var mProgressDialog: Dialog? = null
    private var mDialogProgressBar: ProgressBar? = null
    private var mDialogSeekTime: TextView? = null
    private var mDialogTotalTime: TextView? = null
    private var mDialogIcon: ImageView? = null
    private var mChangePosition: Boolean = false
    private var mTouchingProgressBar = false
    private var mDownPosition: Int = 0
    private var mSeekTimePosition: Int = 0

    private val topShowing = AtomicBoolean(true)

    val playerView by lazy { findViewById<PlayerView>(R.id.playerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        enableImmersiveMode()

        findViewById<ImageView>(R.id.video_back).setOnClickListener { finish() }

        /*if (args.showPath.isEmpty()) {
            finish()
        }*/

        val showPath = intent.getStringExtra("showPath")
        val showName = intent.getStringExtra("showName")
        val downloadOrStream = intent.getBooleanExtra("downloadOrStream", true)

        findViewById<TextView>(R.id.video_name).text = showName

        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        mScreenWidth = this.resources.displayMetrics.widthPixels
        mScreenHeight = this.resources.displayMetrics.heightPixels

        player = SimpleExoPlayer.Builder(this).build()

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    //player back ended
                    finish()
                }
            }
        })

        findViewById<PreviewTimeBar>(R.id.exo_progress)?.let {
            it.setPreviewLoader { currentPosition, _ ->
                Glide.with(this)
                    .asBitmap()
                    .load(showPath)
                    .thumbnail(0.05f)
                    .frame(currentPosition * 1000)
                    .override(IMAGE_ORIGINAL_SIZE, IMAGE_ORIGINAL_SIZE)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(findViewById(R.id.imageView))
            }

            it.addOnScrubListener(object : PreviewBar.OnScrubListener {
                override fun onScrubStart(previewBar: PreviewBar?) {
                    player.playWhenReady = false
                }

                override fun onScrubMove(previewBar: PreviewBar?, progress: Int, fromUser: Boolean) {

                }

                override fun onScrubStop(previewBar: PreviewBar?) {
                    player.playWhenReady = true
                }

            })
        }

        playerView.player = player

        //download
        if (downloadOrStream) {
            val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "AnimeWorld"))
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(showPath!!.toUri())
            player.prepare(videoSource)
        } else {

            //stream
            //fun buildMediaSource(uri: Uri): MediaSource =
            //ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory("exoplayer-codelab")).createMediaSource(uri)

            val source = getMediaSource(showPath!!.toUri(), false)!!
            player.prepare(source, true, false)
        }
        playerView.controllerAutoShow = true
        //playerView.controllerHideOnTouch = true
        playerView.controllerShowTimeoutMs = 2000
        playerView.player!!.playWhenReady = true

        //video_lock.icon = IconicsDrawable(this).icon(FontAwesome.Icon.faw_unlock).sizeDp(24)
        //video_lock.icon = IconicsDrawable(this).icon(FontAwesome.Icon.faw_unlock).size { IconicsSize.dp(24) }
        val videoLock = findViewById<MaterialButton>(R.id.video_lock)
        videoLock.setOnClickListener {
            locked = !locked

            //video_lock.setImageDrawable(IconicsDrawable(this).icon(if (locked) FontAwesome.Icon.faw_lock else FontAwesome.Icon.faw_unlock).sizeDp(24))
            videoLock.icon = ContextCompat.getDrawable(this, if (locked) R.drawable.ic_baseline_lock_24 else R.drawable.ic_baseline_lock_open_24)
            if (!locked)
                playerView.showController()
        }
        videoLock.setOnClickListener {
            onBackPressed()
        }

        initVideoPlayer()

        val pos = getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(showPath, 0)
        playerView.player!!.seekTo(pos)

        batterySetup()

    }

    private val disposable = CompositeDisposable()

    private var batteryInfo: BroadcastReceiver? = null

    private val batteryLevelAlert = PublishSubject.create<Float>()
    private val batteryInfoItem = PublishSubject.create<Battery>()

    enum class BatteryViewType(val icon: GoogleMaterial.Icon) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std),
        FULL(GoogleMaterial.Icon.gmd_battery_full),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown)
    }

    @SuppressLint("SetTextI18n")
    private fun batterySetup() {

        val batteryInformation = findViewById<TextView>(R.id.batteryInformation)

        batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = Color.WHITE
            sizePx = batteryInformation.textSize.roundToInt()
        }

        Flowables.combineLatest(
            batteryLevelAlert
                .map { it <= batteryAlertPercent }
                .map { if (it) Color.RED else Color.WHITE }
                .toLatestFlowable(),
            batteryInfoItem
                .map {
                    when {
                        it.isCharging -> BatteryViewType.CHARGING_FULL
                        it.percent <= batteryAlertPercent -> BatteryViewType.ALERT
                        it.percent >= 95 -> BatteryViewType.FULL
                        it.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                        else -> BatteryViewType.DEFAULT
                    }
                }
                .distinctUntilChanged { t1, t2 -> t1 != t2 }
                .map { IconicsDrawable(this, it.icon).apply { sizePx = batteryInformation.textSize.roundToInt() } }
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                it.second.colorInt = it.first
                batteryInformation.startDrawable = it.second
                batteryInformation.setTextColor(it.first)
                batteryInformation.startDrawable?.setTint(it.first)
            }
            .addTo(disposable)

        batteryInfo = battery {
            batteryInformation.text = "${it.percent.toInt()}%"
            batteryLevelAlert(it.percent)
            batteryInfoItem(it)
        }
    }

    override fun onDestroy() {
        val position = currentPos
        getSharedPreferences("videos", Context.MODE_PRIVATE).edit().putLong(intent.getStringExtra("showPath"), position).apply()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
        disposable.dispose()
        unregisterReceiver(batteryInfo)
        super.onDestroy()
    }

    override fun onStop() {
        try {
            playerView.player!!.playWhenReady = false
            playerView.player!!.release()
        } catch (e: IllegalStateException) {

        }
        lockTimer.stopLock()
        super.onStop()
    }

    override fun onBackPressed() {
        currentPos = playerView.player!!.currentPosition
        playerView.player!!.release()
        super.onBackPressed()
    }

    private fun playVideo() {
        val pause = playerView.findViewById<ImageButton>(R.id.exo_pause)
        if (pause.visibility != View.GONE)
            pause.performClick()
    }

    private fun pauseVideo() {
        val play = playerView.findViewById<ImageButton>(R.id.exo_play)
        if (play.visibility != View.GONE)
            play.performClick()
    }

    private fun initVideoPlayer() {
        gesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {})
        gesture.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(p0: MotionEvent?): Boolean {
                val play = playerView.findViewById<ImageButton>(R.id.exo_play)
                val pause = playerView.findViewById<ImageButton>(R.id.exo_pause)
                if (play.visibility == View.GONE)
                    pause.performClick()
                else
                    play.performClick()
                return true
            }

            override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
                return false
            }

            override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
                return false
            }

        })
        playerView.setOnTouchListener(onTouch)
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouch = View.OnTouchListener { v, event ->
        lockTimer.stopLock()
        //Loged.v("$event")
        if (!locked) {
            gesture.onTouchEvent(event!!)
            val x = event.x
            val y = event.y
            val id = v.id
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

        showLayout()
        if (topShowing.get()) {
            lockTimer.action()
        } else {
            //showLayout()
            lockTimer.startLock()
        }
        false
    }

    private fun showLayout() {
        findViewById<LinearLayout>(R.id.video_info_layout).animate().setDuration(500).alpha(1f).withEndAction {
            topShowing.set(true)
        }
    }

    private fun showProgressDialog(
        deltaX: Float, seekTime: String,
        seekTimePosition: Int, totalTime: String, totalTimeDuration: Int
    ) {
        if (mProgressDialog == null) {
            val localView = View.inflate(this, R.layout.mx_progress_dialog, null)
            mDialogProgressBar = localView.findViewById<View>(R.id.duration_progressbar) as ProgressBar
            //mDialogProgressBar = ((PreviewSeekBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = localView.findViewById<View>(R.id.video_current) as TextView
            mDialogTotalTime = localView.findViewById<View>(R.id.video_duration) as TextView
            mDialogIcon = localView.findViewById<View>(R.id.duration_image_tip) as ImageView
            mProgressDialog = Dialog(this, R.style.mx_style_dialog_progress)
            mProgressDialog!!.setContentView(localView)
            if (mProgressDialog!!.window != null) {
                mProgressDialog!!.window!!.addFlags(Window.FEATURE_ACTION_BAR)
                mProgressDialog!!.window!!.addFlags(32)
                mProgressDialog!!.window!!.addFlags(16)
                mProgressDialog!!.window!!.setLayout(-2, -2)
            }
            val params = mProgressDialog!!.window!!.attributes
            params.gravity = 49
            params.y = resources.getDimensionPixelOffset(R.dimen.mx_progress_dialog_margin_top)
            params.width = this.resources
                .getDimensionPixelOffset(R.dimen.mx_mobile_dialog_width)
            mProgressDialog!!.window!!.attributes = params
        }
        if (!mProgressDialog!!.isShowing) {
            mProgressDialog!!.show()
        }
        val seekedTime = abs(playerView.player!!.currentPosition - seekTimePosition)
        val seekTimeText = "(" + stringForTime(seekedTime) + ") " + seekTime
        mDialogSeekTime!!.text = seekTimeText
        mDialogTotalTime!!.text = String.format(" / %s", totalTime)
        mDialogProgressBar!!.progress = if (totalTimeDuration <= 0) 0 else seekTimePosition * 100 / totalTimeDuration
        if (deltaX > 0) {
            mDialogIcon!!.setBackgroundResource(R.drawable.mx_forward_icon)
        } else {
            mDialogIcon!!.setBackgroundResource(R.drawable.mx_backward_icon)
        }
    }

    private fun dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    private fun showVolumeDialog(v: Float, volumePercent: Int) {
        if (mVolumeDialog == null) {
            val localView = View.inflate(this, R.layout.mx_mobile_volume_dialog, null)
            mDialogVolumeProgressBar = localView.findViewById<View>(R.id.volume_progressbar) as ProgressBar
            mVolumeDialog = Dialog(this, R.style.mx_style_dialog_progress)
            mVolumeDialog!!.setContentView(localView)
            if (mVolumeDialog!!.window != null) {
                mVolumeDialog!!.window!!.addFlags(8)
                mVolumeDialog!!.window!!.addFlags(32)
                mVolumeDialog!!.window!!.addFlags(16)
                mVolumeDialog!!.window!!.setLayout(-2, -2)
            }
            val params = mVolumeDialog!!.window!!.attributes
            params.gravity = 49
            params.y = resources
                .getDimensionPixelOffset(R.dimen.mx_volume_dialog_margin_top)
            params.width = resources
                .getDimensionPixelOffset(R.dimen.mx_mobile_dialog_width)
            mVolumeDialog!!.window!!.attributes = params
        }
        if (!mVolumeDialog!!.isShowing) {
            mVolumeDialog!!.show()
        }
        mDialogVolumeProgressBar!!.progress = volumePercent
    }

    private fun dismissVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog!!.dismiss()
        }
    }

    private fun showBrightnessDialog(v: Float, brightnessPercent: Int) {
        if (mBrightnessDialog == null) {
            val localView = View.inflate(this, R.layout.mx_mobile_brightness_dialog, null)
            mDialogBrightnessProgressBar = localView.findViewById<View>(R.id.brightness_progressbar) as ProgressBar
            mBrightnessDialog = Dialog(this, R.style.mx_style_dialog_progress)
            mBrightnessDialog!!.setContentView(localView)
            if (mBrightnessDialog!!.window != null) {
                mBrightnessDialog!!.window!!.addFlags(8)
                mBrightnessDialog!!.window!!.addFlags(32)
                mBrightnessDialog!!.window!!.addFlags(16)
                mBrightnessDialog!!.window!!.setLayout(-2, -2)
            }
            val params = mBrightnessDialog!!.window!!.attributes
            params.gravity = 49
            params.y = resources
                .getDimensionPixelOffset(R.dimen.mx_volume_dialog_margin_top)
            params.width = resources
                .getDimensionPixelOffset(R.dimen.mx_mobile_dialog_width)
            mBrightnessDialog!!.window!!.attributes = params
        }
        if (!mBrightnessDialog!!.isShowing) {
            mBrightnessDialog!!.show()
        }
        mDialogBrightnessProgressBar.progress = brightnessPercent
    }

    private fun dismissBrightnessDialog() {
        if (mBrightnessDialog != null) {
            mBrightnessDialog!!.dismiss()
        }
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

    class TimerStuff(private val TIME_TO_WAIT: Long = 2000, var action: () -> Unit) {
        private var myRunnable: Runnable = Runnable {
            action()
        }

        private var myHandler = Handler()

        fun startLock() {
            myHandler.postDelayed(myRunnable, TIME_TO_WAIT)
        }

        fun stopLock() {
            myHandler.removeCallbacks(myRunnable)
        }

        fun restartLock() {
            myHandler.removeCallbacks(myRunnable)
            myHandler.postDelayed(myRunnable, TIME_TO_WAIT)
        }
    }

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(this).build() }

    private fun getMediaSource(url: Uri, preview: Boolean): MediaSource? {
        return when (val f = Util.inferContentType(url.lastPathSegment.toString())) {
            C.TYPE_SS -> SsMediaSource.Factory(
                DefaultDataSourceFactory(
                    this, null,
                    getHttpDataSourceFactory(preview)
                )
            )//.createMediaSource(url)
            C.TYPE_DASH -> DashMediaSource.Factory(
                DefaultDataSourceFactory(
                    this, null,
                    getHttpDataSourceFactory(preview)
                )
            )//.createMediaSource(url)
            C.TYPE_HLS -> HlsMediaSource.Factory(getDataSourceFactory(preview))//.createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(getDataSourceFactory(preview))//.createMediaSource(uri)
            else -> null
        }?.createMediaSource(url)
    }

    private fun getDataSourceFactory(preview: Boolean): DataSource.Factory {
        return DefaultDataSourceFactory(
            this, if (preview) null else bandwidthMeter,
            getHttpDataSourceFactory(preview)
        )
    }

    private fun getHttpDataSourceFactory(preview: Boolean): DataSource.Factory {
        return DefaultHttpDataSourceFactory(
            Util.getUserAgent(
                this,
                "AnimeWorld"
            ), if (preview) null else bandwidthMeter
        )
    }

}