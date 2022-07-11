package com.programmersbox.animeworld

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.icon
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.OtakuApp
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.Fetch.Impl.setDefaultInstanceConfiguration
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2core.deleteFile
import org.koin.core.context.loadKoinModules
import java.io.File
import java.net.HttpURLConnection
import javax.net.ssl.*

class AnimeApp : OtakuApp() {
    override fun onCreated() {

        loadKoinModules(appModule)

        FirebaseDb.DOCUMENT_ID = "favoriteShows"
        FirebaseDb.CHAPTERS_ID = "episodesWatched"
        FirebaseDb.COLLECTION_ID = "animeworld"
        FirebaseDb.ITEM_ID = "showUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "numEpisodes"

        val fetchConfiguration = FetchConfiguration.Builder(this)
            .enableAutoStart(true)
            .enableRetryOnNetworkGain(true)
            .enableLogging(true)
            .setProgressReportingInterval(1000L)
            .setGlobalNetworkType(NetworkType.ALL) //.setHttpDownloader(new HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
            .setHttpDownloader(HttpsUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL)) //.setHttpDownloader(new OkHttpDownloader(okHttpClient, Downloader.FileDownloaderType.PARALLEL))
            .setDownloadConcurrentLimit(1)
            //.setNotificationManager(DefaultFetchNotificationManager(this))
            .setNotificationManager(CustomFetchNotificationManager(this))
            .build()
        setDefaultInstanceConfiguration(fetchConfiguration)

        Fetch.getDefaultInstance().addListener(
            object : AbstractFetchListener() {

                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
                override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {}
                override fun onPaused(download: Download) {}
                override fun onResumed(download: Download) {}
                override fun onRemoved(download: Download) {}

                override fun onCompleted(download: Download) {
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(download.file))))
                    notificationManager.cancel(download.id)
                    Fetch.getDefaultInstance().remove(download.id)
                }

                override fun onCancelled(download: Download) {
                    notificationManager.cancel(download.id)
                    try {
                        deleteFile(download.file, this@AnimeApp)
                    } catch (e: IllegalArgumentException) {
                        Loged.w(e.message)//e.printStackTrace()
                    } catch (e: java.lang.NullPointerException) {
                        Loged.w(e.message)//e.printStackTrace()
                    }
                }

                override fun onDeleted(download: Download) {
                    notificationManager.cancel(download.id)
                    try {
                        deleteFile(download.file, this@AnimeApp)
                    } catch (e: IllegalArgumentException) {
                        Loged.w(e.message)//e.printStackTrace()
                    } catch (e: java.lang.NullPointerException) {
                        Loged.w(e.message)//e.printStackTrace()
                    }
                }
            }
        )

    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun shortcuts(): List<ShortcutInfo> = listOf(
        //download viewer
        ShortcutInfo.Builder(this, DownloaderViewModel.DownloadViewerRoute)
            .setIcon(Icon.createWithBitmap(IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_file_download).toBitmap()))
            .setShortLabel(getString(R.string.view_downloads))
            .setLongLabel(getString(R.string.view_downloads))
            .setIntent(Intent(Intent.ACTION_MAIN, Uri.parse(MainActivity.VIEW_DOWNLOADS), this, MainActivity::class.java))
            .build(),
        //video viewer
        ShortcutInfo.Builder(this, DownloadViewModel.VideoViewerRoute)
            .setIcon(Icon.createWithBitmap(IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_video_library).toBitmap()))
            .setShortLabel(getString(R.string.view_videos))
            .setLongLabel(getString(R.string.view_videos))
            .setIntent(Intent(Intent.ACTION_MAIN, Uri.parse(MainActivity.VIEW_VIDEOS), this, MainActivity::class.java))
            .build()
    )

}

class HttpsUrlConnectionDownloader(fileDownloaderType: Downloader.FileDownloaderType) : HttpUrlConnectionDownloader(fileDownloaderType) {
    override fun onPreClientExecute(client: HttpURLConnection, request: Downloader.ServerRequest): Void? {
        super.onPreClientExecute(client, request)
        if (request.url.startsWith("https")) {
            val httpsURLConnection: HttpsURLConnection = client as HttpsURLConnection
            httpsURLConnection.sslSocketFactory = sSLSocketFactory
        }
        return null
    }

    private val sSLSocketFactory: SSLSocketFactory?
        get() {
            var sslContext: SSLContext? = null
            try {
                val tm: Array<TrustManager> = arrayOf(
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkClientTrusted(p0: Array<out java.security.cert.X509Certificate>?, authType: String?) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkServerTrusted(p0: Array<out java.security.cert.X509Certificate>?, authType: String?) {
                        }

                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                    }
                )
                sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, tm, null)
                HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return sslContext?.socketFactory
        }
}

class GenericFileProvider : FileProvider()