package com.programmersbox.animeworld.downloads

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import com.programmersbox.helpfulutils.notificationManager
import com.tonyodev.fetch2.*

class DownloadData(val download: Download, val id: Int = download.id) {
    var eta: Long = -1
    var downloadedBytesPerSecond: Long = 0
    override fun hashCode(): Int = id
    override fun toString(): String = download.toString()
    override fun equals(other: Any?): Boolean = other === this || other is DownloadData && other.id == id
}

class DownloaderViewModel(context: Context) : ViewModel(), ActionListener {
    companion object {
        private const val UNKNOWN_REMAINING_TIME: Long = -1
        private const val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0

        const val DownloadViewerRoute = "view_downloads"
    }

    val fetch: Fetch = Fetch.getDefaultInstance()
    val downloadState = mutableStateListOf<DownloadData>()

    fun onResume() {
        fetch.getDownloads { downloads ->
            downloadState.clear()
            val list = ArrayList(downloads)
            list.sortWith { first, second -> first.created.compareTo(second.created) }
            downloadState.addAll(list.fastMap { DownloadData(it) })
        }.addListener(fetchListener)
    }

    private fun updateUI(
        download: Download,
        etaTime: Long = UNKNOWN_REMAINING_TIME,
        downloadPs: Long = UNKNOWN_DOWNLOADED_BYTES_PER_SECOND,
        addOrRemove: Boolean = true
    ) {
        val d = downloadState.withIndex().find { it.value.id == download.id }

        if (addOrRemove) {
            val item = DownloadData(download).apply {
                eta = etaTime
                downloadedBytesPerSecond = downloadPs
            }

            if (d == null) {
                downloadState.add(item)
            } else {
                downloadState.remove(d.value)
                downloadState.add(d.index, item)
            }
        } else {
            if (download.status == Status.REMOVED || download.status == Status.DELETED) {
                downloadState.removeAt(d!!.index)
            }
        }
    }

    private val fetchListener = object : AbstractFetchListener() {
        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            updateUI(download)
        }

        override fun onCompleted(download: Download) {
            updateUI(download)
            context.notificationManager.cancel(download.id)
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            super.onError(download, error, throwable)
            updateUI(download)
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            updateUI(download, etaInMilliSeconds, downloadedBytesPerSecond)
        }

        override fun onPaused(download: Download) {
            updateUI(download)
        }

        override fun onResumed(download: Download) {
            updateUI(download)
        }

        override fun onCancelled(download: Download) {
            updateUI(download)
            context.notificationManager.cancel(download.id)
        }

        override fun onRemoved(download: Download) {
            downloadState.removeAll { it.id == download.id }
        }

        override fun onDeleted(download: Download) {
            downloadState.removeAll { it.id == download.id }
            context.notificationManager.cancel(download.id)
        }
    }

    override fun onPauseDownload(id: Int) {
        fetch.pause(id)
    }

    override fun onResumeDownload(id: Int) {
        fetch.resume(id)
    }

    override fun onRemoveDownload(id: Int) {
        fetch.remove(id)
    }

    override fun onRetryDownload(id: Int) {
        fetch.retry(id)
    }
}