package com.programmersbox.animeworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.dragswipe.Direction
import com.programmersbox.dragswipe.DragSwipeActionBuilder
import com.programmersbox.dragswipe.DragSwipeUtils
import com.programmersbox.helpfulutils.notificationManager
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch

class DownloadViewerFragment : BaseBottomSheetDialogFragment(), ActionListener {

    companion object {
        private const val UNKNOWN_REMAINING_TIME: Long = -1
        private const val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0
    }

    private var fetch: Fetch = Fetch.getDefaultInstance()
    private var fileAdapter: FileAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.download_viewer_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch = Fetch.getDefaultInstance()
        fileAdapter = FileAdapter(this, requireContext())
        val downloadList = view.findViewById<RecyclerView>(R.id.download_list)
        downloadList.adapter = fileAdapter

        DragSwipeUtils.setDragSwipeUp(
            fileAdapter!!,
            downloadList,
            listOf(Direction.NOTHING),
            listOf(Direction.START, Direction.END),
            DragSwipeActionBuilder {
                onSwiped { viewHolder, direction, dragSwipeAdapter ->
                    (dragSwipeAdapter as? FileAdapter)?.onItemDismiss(viewHolder.adapterPosition, direction.value)
                }
            }
        )

        view.findViewById<MaterialButton>(R.id.multiple_download_delete).setOnClickListener {

            val downloadItems = mutableListOf<FileAdapter.DownloadData>()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete")
                .setMultiChoiceItems(fileAdapter!!.dataList.map { it.download?.file }.toTypedArray(), null) { _, i, b ->
                    if (b) downloadItems.add(fileAdapter!!.dataList[i]) else downloadItems.remove(fileAdapter!!.dataList[i])
                }
                .setPositiveButton("Delete") { d, _ ->
                    Fetch.getDefaultInstance().delete(downloadItems.map { it.id })
                    d.dismiss()
                }
                .show()
        }

    }

    override fun onResume() {
        super.onResume()
        fetch.getDownloads { downloads ->
            val list = ArrayList(downloads)
            list.sortWith { first, second -> first.created.compareTo(second.created) }
            for (download in list) {
                fileAdapter!!.addDownload(download)
            }
        }.addListener(fetchListener)
    }

    override fun onPause() {
        super.onPause()
        fetch.removeListener(fetchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        //fetch!!.close()
    }

    private val fetchListener = object : AbstractFetchListener() {
        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            fileAdapter!!.addDownload(download)
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onCompleted(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
            requireContext().notificationManager.cancel(download.id)
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            super.onError(download, error, throwable)
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            fileAdapter!!.update(download, etaInMilliSeconds, downloadedBytesPerSecond)
        }

        override fun onPaused(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onResumed(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onCancelled(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
            requireContext().notificationManager.cancel(download.id)
        }

        override fun onRemoved(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onDeleted(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
            requireContext().notificationManager.cancel(download.id)
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