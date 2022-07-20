package com.programmersbox.animeworld.cast

import android.app.Activity
import android.app.IntentService
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.media3.common.MimeTypes
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.*
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.images.WebImage
import com.programmersbox.animeworld.R
import io.github.dkbai.tinyhttpd.nanohttpd.webserver.SimpleWebServer
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

typealias SessionCallback = (Int) -> Unit
typealias SimpleCallback = () -> Unit

class CastOptions : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(p0: Context): MutableList<SessionProvider> = mutableListOf()
}


/**
 * A helper created to manage cast in the app.
 *
 * Instead of putting all the codes in activity/fragment, better to
 * separate the logic to a different class.
 */
class CastHelper {
    companion object {
        const val PORT = "8081"
        var deviceIpAddress: String? = null

        fun anyDeviceAvailable(context: Context) =
            CastContext.getSharedInstance(context).castState != CastState.NO_DEVICES_AVAILABLE

        /**
         * Denotes if the device supports casting.
         */
        fun isCastingSupported(context: Context): Boolean {
            val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            if (resultCode != ConnectionResult.SUCCESS
                || uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            ) {
                return false
            }
            return true
        }
    }

    private lateinit var mCastContext: CastContext
    private var mCastSession: CastSession? = null
    private lateinit var mSessionManagerListener: SessionManagerListener<CastSession>

    private var mIntroductoryOverlay: IntroductoryOverlay? = null

    private lateinit var mActivity: WeakReference<Activity>
    private lateinit var mApplicationContext: Context

    private var onSessionDisconnected: SessionCallback? = null
    private var onNeedToShowIntroductoryOverlay: SimpleCallback? = null
    private var onSessionConnected: () -> Unit = {}

    private val sessionConnected = BehaviorSubject.createDefault(false)

    fun sessionConnected(): Observable<Boolean> = sessionConnected

    private val sessionStatus = BehaviorSubject.createDefault(false)

    fun sessionStatus(): Observable<Boolean> = sessionStatus

    fun isCastActive() = try {
        mCastContext.castState == CastState.CONNECTED
    } catch (e: Exception) {
        false
    }

    /**
     * You need to call this method in the parent activity if you are setting up
     * fragment. Otherwise you can call [init] method.
     */
    fun initCastSession(activity: Activity) {
        mActivity = WeakReference(activity)
        mApplicationContext = mActivity.get()?.applicationContext ?: return
        mCastContext = CastContext.getSharedInstance(mApplicationContext)

        setUpCastListener()
        mCastContext.sessionManager.addSessionManagerListener(mSessionManagerListener, CastSession::class.java)
    }

    fun String?.toFile(): File? {
        return if (this != null) File(this) else null
    }

    /**
     * Set the activity along with the toolbar must be called in [onPostCreate]
     * of activity.
     */
    fun init(
        activity: Activity,
        /** Use this to save last play position, Integer value returns the last
         *  played position. */
        onSessionDisconnected: SessionCallback = {},
        onNeedToShowIntroductoryOverlay: SimpleCallback? = null,
        onSessionConnected: () -> Unit = {}
    ) {
        mActivity = WeakReference(activity)
        mApplicationContext = mActivity.get()?.applicationContext ?: return
        this.onSessionDisconnected = onSessionDisconnected
        this.onNeedToShowIntroductoryOverlay = onNeedToShowIntroductoryOverlay
        this.onSessionConnected = onSessionConnected

        deviceIpAddress = Utils.findIPAddress(mApplicationContext)

        setUpCastListener()

        initCastSession(activity)

        mCastContext.addCastStateListener(castListener)
    }

    /** Destroy the session callbacks to avoid memory leaks */
    fun unInit() {
        mCastContext.removeCastStateListener(castListener)
        mCastContext.sessionManager.removeSessionManagerListener(mSessionManagerListener, CastSession::class.java)
        onSessionDisconnected = null
        onNeedToShowIntroductoryOverlay = null
    }

    private val castListener: (Int) -> Unit = { state ->
        sessionConnected.onNext(state != CastState.NO_DEVICES_AVAILABLE)
        if (state != CastState.NO_DEVICES_AVAILABLE) this.onNeedToShowIntroductoryOverlay?.invoke()
        if (state == CastState.NOT_CONNECTED) {
            /** When casting is disconnected we post updateLastModel */
            SimpleWebServer.stopServer()
            //sessionConnected.onNext(false)
        }
        if (state == CastState.CONNECTED) {
            onSessionConnected()
            //sessionConnected.onNext(true)
        }
        sessionStatus.onNext(state == CastState.CONNECTED)
    }

    /** Separate UI logic to avoid memory, use context from view */
    fun setMediaRouteMenu(context: Context, menu: Menu): MenuItem? =
        CastButtonFactory.setUpMediaRouteButton(context, menu, R.id.media_route_menu_item)

    fun setMediaRouteMenu(context: Context, button: MediaRouteButton) =
        CastButtonFactory.setUpMediaRouteButton(context, button)

    /**
     * This is loadMedia is made to work will all activities/fragments
     */
    fun loadMedia(
        mediaFile: File,
        time: Long,
        bannerFile: File?,
        srtFile: File?
    ) {
        val deviceIp = Utils.findIPAddress(mApplicationContext)

        /** Get the remote names of the file */
        val remoteFileName = Utils.getRemoteFileName(deviceIp, mediaFile)
            ?.replace(" ", "%20")
        val remoteImageFileName =
            if (bannerFile != null) Utils.getRemoteFileName(deviceIp, bannerFile)?.replace(" ", "%20")
            else ""//APP_IMAGE_URL

        /** Generate movie meta data */
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, mediaFile.nameWithoutExtension)
        movieMetadata.addImage(WebImage(Uri.parse(remoteImageFileName)))

        buildSubtitle(srtFile) { mediaTracks ->
            val mediaInfo = MediaInfo.Builder(remoteFileName.orEmpty())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                .setMediaTracks(mediaTracks)
                .build()

            /** Start a local HTTP server */
            mApplicationContext.startService(Intent(mApplicationContext, WebService::class.java))

            /** Play the file on the device. */
            if (mCastSession?.remoteMediaClient == null) {
                return@buildSubtitle
            }

            val remoteMediaClient = mCastSession?.remoteMediaClient ?: return@buildSubtitle

            remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
                override fun onStatusUpdated() {
                    /** When media loaded we will start the fullscreen player activity. */
                    val intent = Intent(mApplicationContext, ExpandedControlsActivity::class.java)
                    mActivity.get()?.startActivity(intent)
                    remoteMediaClient.unregisterCallback(this)
                }
            })
            remoteMediaClient.load(
                MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .setCurrentTime(time)
                    .build()
            )
        }
    }

    fun loadUrl(
        mediaUrl: String?,
        title: String,
        name: String,
        poster: String?,
        headers: Map<String, String>
    ) {
        if (mediaUrl == null) return
        /** Generate movie meta data */
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, name)
        movieMetadata.addImage(WebImage(Uri.parse(poster)))
        //movieMetadata.addImage(WebImage(Uri.parse(remoteImageFileName)))

        val mediaInfo = MediaInfo.Builder(mediaUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_UNKNOWN)
            .setMetadata(movieMetadata)
            .build()

        val remoteMediaClient = mCastSession?.remoteMediaClient ?: return

        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                /** When media loaded we will start the fullscreen player activity. */
                val intent = Intent(mApplicationContext, ExpandedControlsActivity::class.java)
                mActivity.get()?.startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
            }
        })

        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(0L)
                .build()
        )
    }

    fun stopCast() {
        mCastSession?.remoteMediaClient?.stop()
        SimpleWebServer.stopServer()
    }

    private fun buildSubtitle(srtFile: File?, onComplete: (List<MediaTrack>) -> Unit) {
        //if (srtFile == null) return onComplete.invoke(listOf())
        GlobalScope.launch(Dispatchers.IO) {
            //val vttFile = File(mApplicationContext.externalCacheDir, srtFile.nameWithoutExtension + ".vtt")
            //val remoteSrtFileName = Utils.getRemoteFileName(deviceIpAddress, vttFile)
            /** Convert srt to vtt on background thread */
            //SubtitleConverter.convertFromSrtToVtt(srtFile, vttFile)
            /** Post subtitles on main thread */
            withContext(Dispatchers.Main) {
                onComplete.invoke(
                    listOf(
                        MediaTrack.Builder(1, MediaTrack.TYPE_TEXT)
                            //.setName(srtFile.name)
                            .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                            //.setContentId(remoteSrtFileName)
                            .setLanguage("en-US")
                            .build()
                    )
                )
            }
        }
    }

    private fun setUpCastListener() {
        mSessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, p1: String) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(p0: CastSession, p1: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionEnded(p0: CastSession, p1: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResumed(session: CastSession, p1: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(p0: CastSession, p1: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionSuspended(p0: CastSession, p1: Int) {}

            override fun onSessionStarting(castSession: CastSession) {
                mCastSession = castSession
            }

            override fun onSessionResuming(castSession: CastSession, p1: String) {
                mCastSession = castSession
            }

            override fun onSessionEnding(p0: CastSession) {}

            private fun onApplicationConnected(castSession: CastSession) {
                mCastSession = castSession
                mActivity.get()?.invalidateOptionsMenu()
            }

            private fun onApplicationDisconnected() {
                mActivity.get()?.invalidateOptionsMenu()
            }
        }
    }

    fun showIntroductoryOverlay(mediaRouteMenuItem: MediaRouteButton?) {
        mIntroductoryOverlay?.remove()

        if (mediaRouteMenuItem != null && mediaRouteMenuItem.isVisible)
            mIntroductoryOverlay = IntroductoryOverlay.Builder(
                mActivity.get()!!, mediaRouteMenuItem
            )
                .setTitleText(R.string.castIntro)
                .setSingleTime()
                .setOnOverlayDismissedListener { mIntroductoryOverlay = null }
                .build()
        mIntroductoryOverlay?.show()
    }
}

class Utils {
    companion object {
        fun findIPAddress(context: Context): String? {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            try {
                return if (wifiManager.connectionInfo != null) {
                    val wifiInfo = wifiManager.connectionInfo
                    InetAddress.getByAddress(
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(wifiInfo.ipAddress)
                            .array()
                    ).hostAddress
                } else
                    null
            } catch (e: Exception) {
                Log.e(Utils::class.java.name, "Error finding IpAddress: ${e.message}", e)
            }
            return null
        }

        fun getRemoteFileName(deviceIp: String?, file: File): String? {
            if (deviceIp == null) return null
            val root = Environment.getExternalStorageDirectory().absolutePath
            return "http://${deviceIp}:${CastHelper.PORT}${file.absolutePath.replace(root, "")}"
        }
    }
}

class WebService : IntentService("blank") {
    private val TAG = javaClass.simpleName

    override fun onStart(intent: Intent?, startId: Int) {
        SimpleWebServer.stopServer()
        super.onStart(intent, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            /** Running a server on Internal storage.
             *
             * I know the method [Environment.getExternalStorageDirectory] is deprecated
             * but it is needed to start the server in the required path.
             */

            SimpleWebServer.runServer(
                arrayOf(
                    "-h",
                    CastHelper.deviceIpAddress,
                    "-p",
                    CastHelper.PORT,
                    "-d",
                    Environment.getExternalStorageDirectory().absolutePath
                )
            )
            Log.d(TAG, "Service Started on ${CastHelper.deviceIpAddress}:${CastHelper.PORT}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        SimpleWebServer.stopServer()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }
}