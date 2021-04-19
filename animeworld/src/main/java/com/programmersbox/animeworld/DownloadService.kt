package com.programmersbox.animeworld

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.text.Html
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import com.github.se_bastiaan.torrentstream.utils.FileUtils
import com.programmersbox.animeworld.ytsdatabase.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random
import com.programmersbox.animeworld.ytsdatabase.Torrent as ATorrent


@SuppressLint("WakelockTimeout")
class DownloadService : IntentService("blank") {

    companion object {
        const val TORRENT_JOB = "torrent_job"
    }

    //@Inject
    val pauseRepository: PauseRepository by lazy { PauseRepository(DownloadDatabase.getInstance(this).getPauseDao()) }

    //@Inject
    val downloadRepository: DownloadRepository by lazy { DownloadRepository(DownloadDatabase.getInstance(this).getDownloadDao()) }

    val TAG = "DownloadService"
    private var pendingJobs = ArrayList<ATorrent>()
    private var currentModel: com.github.se_bastiaan.torrentstream.Torrent? = null
    private var currentTorrentModel: ATorrent? = null
    private lateinit var context: Context
    private var wakeLock: WakeLock? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var contentIntent: PendingIntent
    private lateinit var cancelIntent: PendingIntent
    private lateinit var torrentStream: TorrentStream
    private val FOREGROUND_ID = 1
    private var toDelete = false
    private var totalGap: Long = 0
    private var lastProgress: Float? = 0f

    private val SHOW_LOG_FROM_THIS_CLASS = true

    init {
        setIntentRedelivery(true)
    }

    override fun onCreate() {

        DS_LOG("=> onCreate() called")

        context = applicationContext

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "yts_01",
                "ytsdownload",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description = "Movie Download Service"

            val channel = NotificationChannel(
                "yts_02",
                context.getString(R.string.download),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(notificationChannel)
            notificationManager.createNotificationChannel(channel)
        }

        val newIntent = Intent(context, CommonBroadCast::class.java)
        newIntent.action = STOP_SERVICE
        cancelIntent = PendingIntent.getBroadcast(context, 5, newIntent, 0)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "app:Wakelock")
        wakeLock?.acquire()

        /** Registering local broadcast receiver */

        val filter = IntentFilter()
        filter.addAction(REMOVE_CURRENT_JOB)
        filter.addAction(PAUSE_JOB)
        LocalBroadcastManager.getInstance(context).registerReceiver(localBroadcastReceiver, filter)

        super.onCreate()
    }

    private fun setContentIntent(config: ATorrent, torrentJob: TorrentJob? = null) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val t: TorrentJob = torrentJob
            ?: TorrentJob(
                config.title, config.banner_url, 0, 0, 0f, 0, 0, false, "Pending",
                0, getMagnetHash(config.url)
            )
        notificationIntent.action = MODEL_UPDATE
        notificationIntent.putExtra("model", t)
        notificationIntent.putExtra("currentModel", config)
        notificationIntent.putExtra("pendingModels", pendingJobs)
        contentIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(400) + 150,
            notificationIntent,
            0
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return super.onStartCommand(intent, flags, startId)
        val config: ATorrent = intent.getSerializableExtra(TORRENT_JOB) as ATorrent
        pendingJobs.add(config)

        DS_LOG("=> onStartCommand(): ${config.title} - $config")

        setContentIntent(config)

        val notification = NotificationCompat.Builder(context, "yts_01")
            .setContentTitle(getContentTitle(config.title, 0))
            .addAction(R.mipmap.ic_launcher, "Cancel", cancelIntent)
            .setContentText(getContentText("0 KB/s"))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_ID, notification)
        } else {
            notificationManager.notify(FOREGROUND_ID, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    var ANONYMOUS_TORRENT_DOWNLOAD = true
    var DOWNLOAD_TIMEOUT_SECOND = 40
    var DOWNLOAD_CONNECTION_TIMEOUT = 100

    override fun onHandleIntent(intent: Intent?) {

        var isJobCompleted = false
        var isConnected = false
        var isError = false
        var isStopped = false
        var isCurrentProgressUpdated = false
        var isAutoStopped = false
        toDelete = false

        val model = intent?.getSerializableExtra(TORRENT_JOB) as ATorrent

        updateNotification(model, null, false)

        currentTorrentModel = model

        /** Removing pauseModel from database if current model is already a pause Job */

        pauseRepository.deletePause(model.hash)

        DS_LOG("=> onHandleIntent(): ${model.title}")

        var isExist = false
        for (c in pendingJobs) {
            if (c.title == model.title) isExist = true
        }

        if (!isExist) return

        if (pendingJobs.size > 0)
            pendingJobs.removeAt(0)

        /** Update pending jobs */
        updatePendingJobs()

        updateNotification(model, null, true)
        DS_LOG(model.toString())
        DS_LOG("=> Loading Job: ${model.title}")

        val torrentOptions: TorrentOptions = TorrentOptions.Builder()
            .saveLocation(folderLocation)
            .autoDownload(true)
            .removeFilesAfterStop(false)
            .anonymousMode(ANONYMOUS_TORRENT_DOWNLOAD)
            .build()

        torrentStream = TorrentStream.init(torrentOptions)
        torrentStream.addListener(object : TorrentListener {
            override fun onStreamReady(torrent: com.github.se_bastiaan.torrentstream.Torrent?) {
                DS_LOG("=> Stream Ready")
            }

            override fun onStreamPrepared(torrent: com.github.se_bastiaan.torrentstream.Torrent?) {
                updateNotification(model, torrent, true)
                currentModel = torrent
                DS_LOG("=> Preparing Job: ${torrent?.saveLocation}")
                lastProgress = 0f
                totalGap = getCurrentTimeSecond()
                isConnected = true
            }

            override fun onStreamStopped() {
                DS_LOG("=> Stream Stopped")
                if (currentModel != null && toDelete) {
                    FileUtils.recursiveDelete(currentModel?.saveLocation)
                }
                isStopped = true
                isJobCompleted = true
            }

            override fun onStreamStarted(torrent: com.github.se_bastiaan.torrentstream.Torrent?) {
                DS_LOG("=> Stream Started")
            }

            override fun onStreamProgress(
                torrent: com.github.se_bastiaan.torrentstream.Torrent?,
                status: StreamStatus?
            ) {
                if (lastProgress == 0f || lastProgress != status?.progress) {
                    updateNotification(model, torrent, false, status)
                    DS_LOG(
                        "=> Progress: ${status?.progress}, Download queue: ${torrent?.torrentHandle?.downloadQueue?.size}, " +
                                "Piece availability: ${torrent?.torrentHandle?.pieceAvailability()?.size}, " +
                                "Piece size: ${
                                    torrent?.torrentHandle?.torrentFile()
                                        ?.numPieces()
                                }, " +
                                "Pieces to prepare: ${torrent?.piecesToPrepare}"
                    )
                    if (status?.progress?.toInt() == 100) {
                        isJobCompleted = true
                        DS_LOG("=> JobCompleted")
                    }
                }
                totalGap = getCurrentTimeSecond()
                isCurrentProgressUpdated = true
                lastProgress = status?.progress
            }

            override fun onStreamError(
                torrent: com.github.se_bastiaan.torrentstream.Torrent?,
                e: Exception?
            ) {
                DS_LOG("=> Stream Error")
                isJobCompleted = true
                isError = true
            }
        })

        torrentStream.startStream(model.url)

        do {
            if ((isCurrentProgressUpdated && getCurrentTimeSecond() > totalGap + DOWNLOAD_TIMEOUT_SECOND && lastProgress?.toInt()!! >= 98)
                || (isConnected && getCurrentTimeSecond() > totalGap + DOWNLOAD_CONNECTION_TIMEOUT && lastProgress?.toInt()!! == 0)
            ) {
                DS_LOG("=> Auto Stopping Stream")
                toDelete = false
                isAutoStopped = true
                torrentStream.stopStream()

                if (lastProgress == 0f) {
                    val i = Intent(context, CommonBroadCast::class.java)
                    i.action = TORRENT_NOT_SUPPORTED
                    sendBroadcast(i)
                    stopSelf()
                }
            }
        } while (!isJobCompleted)

        if (!isStopped || isAutoStopped) handleAfterJobComplete(model, isError)

        onClear()
    }

    private fun onClear() {
        currentModel = null
        currentTorrentModel = null
    }

    fun updateNotification(
        model: ATorrent, torrent: com.github.se_bastiaan.torrentstream.Torrent?,
        isIndeterminate: Boolean, status: StreamStatus? = null
    ) {

        /** Prepare current model */

        val torrentJob: TorrentJob
        val magnetHash: String = getMagnetHash(model.url)

        if (status != null) {

            /** No passing of currentSize since torrent downloads usually create a skeleton structure
             *  with total torrent size and then update byte location provided by torrent pieces
             *
             *  val currentSize = Utils.getDirSize(torrent?.saveLocation!!)
             */

            torrentJob = TorrentJob(
                model.title,
                model.banner_url,
                status.progress.toInt(),
                status.seeds,
                status.downloadSpeed.toFloat(),
                0,
                torrent?.torrentHandle?.torrentFile()?.totalSize(),
                true,
                "Downloading",
                torrent?.torrentHandle?.peerInfo()?.size as Int,
                magnetHash
            )
        } else {
            val size = torrent?.torrentHandle?.torrentFile()?.totalSize() ?: 0
            torrentJob = TorrentJob(
                model.title, model.banner_url, 0, 0, 0f, 0,
                size, false, "Preparing", 0, magnetHash
            )
        }

        /** Update the notification channel */

        val speed = status?.downloadSpeed ?: 0

        val progress = status?.progress ?: 0f

        setContentIntent(model, torrentJob)

        val speedString = formatDownloadSpeed(speed.toFloat())

        val notificationBuilder =
            NotificationCompat.Builder(context, "yts_01")
                .setContentTitle(getContentTitle(model.title, progress.toInt()))
                .addAction(R.mipmap.ic_launcher, "Cancel", cancelIntent)
                .setContentText(getContentText(speedString))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)

        if (isIndeterminate)
            notificationBuilder.setProgress(100, 0, true)

        notificationManager.notify(FOREGROUND_ID, notificationBuilder.build())

        /** Update the current model */

        val intent = Intent(MODEL_UPDATE)
        intent.putExtra("model", torrentJob)
        intent.putExtra("currentModel", model)
        intent.putExtra("models", pendingJobs)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun formatDownloadSpeed(downloadSpeed: Float): String {
        val speed = downloadSpeed.toDouble() / 1000.00
        return if (speed > 1000) {
            DecimalFormat("0.0").format(speed / 1000) + " MB/s"
        } else DecimalFormat("0.0").format(speed) + " KB/s"
    }

    private fun getContentText(speedString: String?): String {
        return "Downloading: ${pendingJobs.size + 1}  ${Html.fromHtml("&#8226;")}  $speedString ${
            Html.fromHtml(
                "&#8595;"
            )
        }"
    }

    private fun getContentTitle(title: String?, progress: Int?): String {
        return "$title  ${Html.fromHtml("&#8226;")}   ${progress}%"
    }


    private fun getMagnetHash(url: String): String {
        return url.substring(url.lastIndexOf("/") + 1)
    }

    private fun updatePendingJobs() {
        val intent = Intent(PENDING_JOB_UPDATE)
        intent.putExtra("models", pendingJobs)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun updateEmptyQueue() {
        val intent = Intent(EMPTY_QUEUE)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun getVideoDuration(context: Context, videoFile: File): Long? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(videoFile))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return time?.toLong()
    }

    fun saveImageFromUrl(src: String, file: File) {
        try {
            val url = URL(src)
            val connection: HttpURLConnection = url
                .openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            file.outputStream().use { input.copyTo(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun handleAfterJobComplete(model: ATorrent, isError: Boolean = false) {

        /** Create notification based on error bool */
        try {
            if (!isError) {

                /** Save details of file in database. */
                val imagePath = File(currentModel?.saveLocation, "banner.png")
                //saveImageFromUrl(model.banner_url, imagePath)

                val todayDate = SimpleDateFormat("yyyy-MM-dd")
                    .format(Calendar.getInstance().time)

                val movieSize = getVideoDuration(this, currentModel?.videoFile!!)
                    .takeUnless { it == null } ?: 0L

                val downloadResponse = Model.response_download(
                    title = model.title,
                    imagePath = imagePath.path,
                    downloadPath = currentModel?.saveLocation?.path,
                    size = model.size,
                    date_downloaded = todayDate,
                    hash = model.hash,
                    total_video_length = movieSize,
                    videoPath = currentModel?.videoFile?.path,
                    movieId = currentTorrentModel?.movieId,
                    imdbCode = currentTorrentModel?.imdbCode
                )

                downloadRepository.saveDownload(downloadResponse)

                /** Save a detail.json file. */
                //val detailPath = File(currentModel?.saveLocation, "details.json")
                //detailPath.writeText(Gson().toJson(downloadResponse))

                /** Send download complete notification */
                Notifications.sendDownloadNotification(context, model.title)
                DS_LOG("Download Completed for ${model.title}")
            } else {
                /** Send download failed notification */
                Notifications.sendDownloadFailedNotification(this, model.title)
                DS_LOG("Download Failed for ${model.title}")
            }
        } catch (e: Exception) {
            /** Something unexpected occurred. */
            Log.e(TAG, "Download failed for ${model.title} due to ${e.message}", e)
            Notifications.sendDownloadFailedNotification(this, model.title)
        }
    }

    private val localBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PAUSE_JOB -> {
                    val torrentJob = intent.getSerializableExtra("model") as TorrentJob
                    torrentJob.status = "Paused"//getString(R.string.paused)
                    val modelPause = Model.response_pause(
                        job = torrentJob,
                        hash = torrentJob.magnetHash,
                        torrent = currentTorrentModel,
                        saveLocation = currentModel?.saveLocation?.path
                    )

                    /** Saving current job to database and making it pause*/
                    pauseRepository.savePauseModel(
                        modelPause
                    )

                    /** Removing current job */
                    torrentStream.stopStream()
                }
                REMOVE_CURRENT_JOB -> {
                    toDelete = intent.getBooleanExtra("deleteFile", false)
                    torrentStream.stopStream()
                }
            }
        }
    }

    fun getCurrentTimeSecond(): Long {
        return System.currentTimeMillis() / 1000
    }

    private fun saveInterruptDownloads() {
        val model = currentTorrentModel
        val currentTorrent = currentModel
        if (model != null && currentTorrent != null) {
            pauseRepository.savePauseModel(
                Model.response_pause(
                    job = TorrentJob.from(model),
                    hash = model.hash,
                    saveLocation = currentTorrent.saveLocation.path,
                    torrent = model
                )
            )
        }
        for (job in pendingJobs) {
            pauseRepository.savePauseModel(
                Model.response_pause(
                    job = TorrentJob.from(job),
                    hash = job.hash,
                    saveLocation = null,
                    torrent = job
                )
            )
        }
    }

    override fun onDestroy() {

        DS_LOG("=> onDestroy() called")

        /** Save all interrupted downloads */
        saveInterruptDownloads()

        /** Update receiver to know that all jobs completed */
        updateEmptyQueue()

        /** Remove registered localbroadcast */
        LocalBroadcastManager.getInstance(context).unregisterReceiver(localBroadcastReceiver)

        pendingJobs.clear()
        if (torrentStream.isStreaming) torrentStream.stopStream()
        wakeLock?.release()
        notificationManager.cancel(FOREGROUND_ID)

        super.onDestroy()
    }

    private fun DS_LOG(message: String?) {
        if (SHOW_LOG_FROM_THIS_CLASS)
            Log.e(TAG, message.toString())
    }
}

const val TORRENT_NOT_SUPPORTED = "com.kpstv.yts.TORRENT_NOT_SUPPORTED"
const val MODEL_UPDATE = "com.kpstv.yts.MODEL_UPDATE"
const val STOP_SERVICE = "com.kpstv.yts.STOP_SERVICE"
const val PENDING_JOB_UPDATE = "com.kpstv.yts.PENDING_JOB_UPDATE"
const val EMPTY_QUEUE = "com.kpstv.yts.EMPTY_QUEUE"
const val PAUSE_JOB = "com.kpstv.yts.PAUSE_JOB"
const val UNPAUSE_JOB = "com.kpstv.yts.ADD_ONLY_JOB"
const val REMOVE_CURRENT_JOB = "com.kpstv.yts.REMOVE_CURRENT_JOB"

class CommonBroadCast : BroadcastReceiver() {

    companion object {
        const val STOP_UPDATE_WORKER = "com.kpstv.actions.stop_update_worker"
        const val STOP_CAST_SERVICE = "com.kpstv.actions.stop_cast_service"
        const val INSTALL_APK = "com.kpstv.actions.install_apk"

        const val ARGUMENT_APK_FILE = "apk_file_argument"
    }

    private val TAG = "CommonBroadCast"

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            STOP_SERVICE -> {
                val serviceIntent = Intent(context, DownloadService::class.java)
                context?.stopService(serviceIntent)
            }
            TORRENT_NOT_SUPPORTED -> {

            }
            UNPAUSE_JOB -> {

                Log.e(TAG, "=> UNPAUSE JOB")

                val serviceDownload = Intent(context, DownloadService::class.java)
                serviceDownload.putExtra(DownloadService.TORRENT_JOB, intent.getSerializableExtra("model") as ATorrent)
                ContextCompat.startForegroundService(context!!, serviceDownload)
            }
            /*STOP_CAST_SERVICE -> {
                val serviceIntent = Intent(context, CastTorrentService::class.java)
                context?.stopService(serviceIntent)
            }*/
        }
    }
}

data class Progress(
    val currentBytes: Long,
    val totalBytes: Long
)

object Notifications {

    private val TAG = javaClass.simpleName

    private const val UPDATE_REQUEST_CODE = 129
    private const val UPDATE_NOTIFICATION_ID = 7
    private const val UPDATE_PROGRESS_NOTIFICATION_ID = 21

    private lateinit var mgr: NotificationManager

    fun setup(context: Context) = with(context) {
        mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            mgr.createNotificationChannel(
                NotificationChannel("yts_02", context.getString(R.string.download), NotificationManager.IMPORTANCE_DEFAULT)
            )

            mgr.createNotificationChannel(
                NotificationChannel("yts_03", getString(R.string.update), NotificationManager.IMPORTANCE_LOW)
            )

            mgr.createNotificationChannel(
                NotificationChannel("yts_04", "stream", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun sendUpdateProgressNotification(
        context: Context,
        progress: Progress,
        fileName: String,
        cancelRequestCode: Int
    ) = with(context) {
        /*val cancelIntent = Intent(this, CommonBroadCast::class.java).apply {
            action = CommonBroadCast.STOP_UPDATE_WORKER
        }
        val pendingIntent =
            PendingIntent.getBroadcast(this, cancelRequestCode, cancelIntent, 0)
        val notification = NotificationCompat.Builder(this, getString(R.string.CHANNEL_ID_3))
            .setContentTitle(fileName)
            .setContentText("${CommonUtils.getSizePretty(progress.currentBytes, false)} / ${CommonUtils.getSizePretty(progress.totalBytes)}")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(
                100,
                ((progress.currentBytes * 100) / progress.totalBytes).toInt(),
                false
            )
            .setShowWhen(false)
            .addAction(R.drawable.ic_close, getString(R.string.close), pendingIntent)
            .build()

        mgr.notify(UPDATE_PROGRESS_NOTIFICATION_ID, notification)*/
    }

    fun removeUpdateProgressNotification() = mgr.cancel(UPDATE_PROGRESS_NOTIFICATION_ID)

    fun sendUpdateNotification(context: Context) = with(context) {
        /*val updateIntent = Intent(this, StartActivity::class.java)
            .apply {
                action = ActivityIntentHelper.ACTION_FORCE_CHECK_UPDATE
            }
        val pendingIntent = PendingIntent.getActivity(
            this,
            UPDATE_REQUEST_CODE,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.CHANNEL_ID_2))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.update_message))
            .setSmallIcon(R.drawable.ic_support)
            .setColor(colorFrom(R.color.colorPrimary_New_DARK))
            .setColorized(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        mgr.notify(UPDATE_NOTIFICATION_ID, notification)*/
    }

    /*fun sendDownloadCompleteNotification(context: Context, file: File) = with(context) {
        val installIntent = Intent(this, CommonBroadCast::class.java).apply {
            action = CommonBroadCast.INSTALL_APK
            putExtra(CommonBroadCast.ARGUMENT_APK_FILE, file.absolutePath)
        }

        val notification = NotificationCompat.Builder(this, getString(R.string.CHANNEL_ID_2))
            .setContentTitle(getString(R.string.update_download))
            .setContentText(getString(R.string.update_install))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(PendingIntent.getBroadcast(this, getRandomNumberCode(), installIntent, 0))
            .setAutoCancel(true)

        mgr.notify(UPDATE_NOTIFICATION_ID,  notification.build())
    }*/

    val ACTION_MOVE_TO_LIBRARY = "action_move_to_library"

    fun sendDownloadNotification(context: Context, contentText: String) = with(context) {
        val downloadIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_MOVE_TO_LIBRARY
        }

        val pendingIntent =
            PendingIntent.getActivity(this, getRandomNumberCode(), downloadIntent, 0)

        val notification =
            NotificationCompat.Builder(this, "yts_02").apply {
                setContentTitle("Completed")
                setContentText(contentText)
                setSmallIcon(R.drawable.exo_ic_check)
                setContentIntent(pendingIntent)
                setAutoCancel(true)
                priority = Notification.PRIORITY_LOW
            }.build()

        mgr.notify(getRandomNumberCode(), notification)
    }

    fun sendDownloadFailedNotification(context: Context, contentText: String) = with(context) {
        val notification =
            NotificationCompat.Builder(this, "yts_02").apply {
                setDefaults(Notification.DEFAULT_ALL)
                setContentTitle("Failed")
                setContentText(contentText)
                setSmallIcon(R.drawable.ic_dialog_close_dark)
                setAutoCancel(true)
                priority = Notification.PRIORITY_LOW
            }.build()

        mgr.notify(getRandomNumberCode(), notification)
    }

    fun sendSSLHandshakeNotification(context: Context) = with(context) {
        /*val notification =
            NotificationCompat.Builder(this, getString(R.string.CHANNEL_ID_2))
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(getString(R.string.error_ssl_handshake_title))
                .setContentText(getString(R.string.error_ssl_handshake_text))
                .setColor(colorFrom(R.color.colorPrimary_New_DARK))
                .setSmallIcon(R.drawable.ic_error_outline)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build()

        mgr.notify(getRandomNumberCode(), notification)*/
    }

    fun createCastNotification(
        context: Context,
        movieName: String = "Processing...",
        progress: Int = 0,
        closePendingIntent: PendingIntent? = null
    ): Notification = with(context) {
        val builder = NotificationCompat.Builder(this, "yts_04").apply {
            setDefaults(Notification.DEFAULT_ALL)
            setContentTitle(movieName)
            setOngoing(true)
            setShowWhen(false)
            color = Color.BLUE//colorFrom(R.color.colorPrimary_New_DARK)
            setSmallIcon(R.drawable.mx_forward_icon)
            priority = Notification.PRIORITY_LOW
        }

        if (progress == 0)
            builder.setProgress(100, 0, true)
        else
            builder.setContentText("Streaming in background ($progress%)")

        if (closePendingIntent != null)
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.exo_notification_small_icon, "Close", closePendingIntent
                ).build()
            )

        builder.build()
    }

    fun getRandomNumberCode() = java.util.Random().nextInt(400) + 150
}
