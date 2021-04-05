package com.programmersbox.animeworld

import android.annotation.SuppressLint
import androidx.core.content.FileProvider
import com.programmersbox.anime_sources.Sources
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.loggingutils.Loged
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker
import com.programmersbox.uiviews.utils.FirebaseDb
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.Fetch.Impl.setDefaultInstanceConfiguration
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2core.deleteFile
import java.net.HttpURLConnection
import javax.net.ssl.*

class AnimeApp : OtakuApp() {
    override fun onCreated() {

        logo = R.mipmap.ic_launcher
        notificationLogo = R.mipmap.ic_launcher_foreground

        UpdateWorker.sourcesList = Sources.values().toList()
        UpdateWorker.sourceFromString = {
            try {
                Sources.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        FirebaseDb.DOCUMENT_ID = "favoriteShows"
        FirebaseDb.CHAPTERS_ID = "episodesWatched"
        FirebaseDb.COLLECTION_ID = "animeworld"
        FirebaseDb.ITEM_ID = "showUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "watched"

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
                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {

                }

                override fun onCompleted(download: Download) {
                    notificationManager.cancel(download.id)
                    Fetch.getDefaultInstance().remove(download.id)
                }


                override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
                }

                override fun onPaused(download: Download) {
                }

                override fun onResumed(download: Download) {
                }

                override fun onCancelled(download: Download) {
                    notificationManager.cancel(download.id)
                    try {
                        deleteFile(download.file, this@AnimeApp)
                    } catch (e: IllegalArgumentException) {
                        Loged.w(e.message!!)//e.printStackTrace()
                    } catch (e: java.lang.NullPointerException) {
                        Loged.w(e.message!!)//e.printStackTrace()
                    }
                }

                override fun onRemoved(download: Download) {
                }

                override fun onDeleted(download: Download) {
                    notificationManager.cancel(download.id)
                    try {
                        deleteFile(download.file, this@AnimeApp)
                    } catch (e: IllegalArgumentException) {
                        Loged.w(e.message!!)//e.printStackTrace()
                    } catch (e: java.lang.NullPointerException) {
                        Loged.w(e.message!!)//e.printStackTrace()
                    }
                }
            }
        )

    }
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