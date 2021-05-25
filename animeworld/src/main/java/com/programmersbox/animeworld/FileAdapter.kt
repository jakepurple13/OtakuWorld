package com.programmersbox.animeworld

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.Status.*
import java.text.DecimalFormat

class FileAdapter internal constructor(private val actionListener: ActionListener, private val context: Context) :
    DragSwipeAdapter<FileAdapter.DownloadData, FileAdapter.ViewHolder>() {

    override fun ViewHolder.onBind(item: DownloadData, position: Int) = Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.download_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.actionButton.setOnClickListener(null)
        holder.actionButton.isEnabled = true
        val downloadData = dataList[position]
        var url = ""
        if (downloadData.download != null) {
            url = downloadData.download!!.url
        }
        val uri: Uri = Uri.parse(url)
        val status = downloadData.download!!.status
        val context: Context = holder.itemView.context
        holder.titleTextView.text = uri.lastPathSegment
        holder.statusTextView.text = context.getString(getStatusString(status))
        var progress = downloadData.download!!.progress
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0
        }
        holder.progressBar.progress = progress
        holder.progressTextView.text = context.getString(R.string.percent_progress, progress)
        if (downloadData.eta == -1L) {
            holder.timeRemainingTextView.text = ""
        } else {
            holder.timeRemainingTextView.text = getETAString(downloadData.eta, true)
        }
        if (downloadData.downloadedBytesPerSecond == 0L) {
            holder.downloadedBytesPerSecondTextView.text = ""
        } else {
            holder.downloadedBytesPerSecondTextView.text = getDownloadSpeedString(downloadData.downloadedBytesPerSecond)
        }
        when (status) {
            COMPLETED -> {
                holder.actionButton.setText(R.string.view)
                /*holder.actionButton.setOnClickListener { view ->
                    Toast.makeText(context, "Downloaded Path:" + downloadData.download!!.file, Toast.LENGTH_LONG).show()
                    val file = File(downloadData.download!!.file)
                    val uri1: Uri = Uri.fromFile(file)
                    val share = Intent(Intent.ACTION_VIEW)
                    share.setDataAndType(uri1, getMimeType(context, uri1))
                    context.startActivity(share)
                }*/
            }
            FAILED -> {
                holder.actionButton.setText(R.string.retry)
                holder.actionButton.setOnClickListener { view ->
                    holder.actionButton.isEnabled = false
                    actionListener.onRetryDownload(downloadData.download!!.id)
                }
            }
            PAUSED -> {
                holder.actionButton.setText(R.string.resume)
                holder.actionButton.setOnClickListener { view ->
                    holder.actionButton.isEnabled = false
                    actionListener.onResumeDownload(downloadData.download!!.id)
                }
            }
            DOWNLOADING, QUEUED -> {
                holder.actionButton.setText(R.string.pause)
                holder.actionButton.setOnClickListener { view ->
                    holder.actionButton.isEnabled = false
                    actionListener.onPauseDownload(downloadData.download!!.id)
                }
            }
            ADDED -> {
                holder.actionButton.setText(R.string.download)
                holder.actionButton.setOnClickListener { view ->
                    holder.actionButton.isEnabled = false
                    actionListener.onResumeDownload(downloadData.download!!.id)
                }
            }
            else -> {
            }
        }
        holder.actionButton.setOnLongClickListener { view ->
            holder.itemView.performClick()
            true
        }

        //Set delete action
        holder.itemView.setOnLongClickListener { v: View? ->
            context.deleteDialog(downloadData.download!!) {}
            true
        }
    }

    fun addDownload(download: Download) {
        var found = false
        var data: DownloadData? = null
        var dataPosition = -1
        for (i in dataList.indices) {
            val downloadData = dataList[i]
            if (downloadData.id == download.id) {
                data = downloadData
                dataPosition = i
                found = true
                break
            }
        }
        if (!found) {
            val downloadData = DownloadData()
            downloadData.id = download.id
            downloadData.download = download
            dataList.add(downloadData)
            notifyItemInserted(dataList.size - 1)
        } else {
            data!!.download = download
            notifyItemChanged(dataPosition)
        }
    }

    fun update(download: Download, eta: Long, downloadedBytesPerSecond: Long) {
        for (position in dataList.indices) {
            val downloadData = dataList[position]
            if (downloadData.id == download.id) {
                when (download.status) {
                    Status.REMOVED, Status.DELETED -> {
                        dataList.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    else -> {
                        downloadData.download = download
                        downloadData.eta = eta
                        downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond
                        notifyItemChanged(position)
                    }
                }
                return
            }
        }
    }

    private fun getStatusString(status: Status): Int = when (status) {
        COMPLETED -> R.string.done
        DOWNLOADING -> R.string.downloading_no_dots
        FAILED -> R.string.error_text
        PAUSED -> R.string.paused_text
        QUEUED -> R.string.waiting_in_queue
        REMOVED -> R.string.removed_text
        NONE -> R.string.not_queued
        else -> R.string.unknown
    }

    fun onItemDismiss(position: Int, direction: Int) {
        context.deleteDialog(removeItem(position).download!!) { notifyDataSetChanged() }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        return false
    }

    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.status_TextView)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val progressTextView: TextView = itemView.findViewById(R.id.progress_TextView)
        val actionButton: Button = itemView.findViewById(R.id.actionButton)
        val timeRemainingTextView: TextView = itemView.findViewById(R.id.remaining_TextView)
        val downloadedBytesPerSecondTextView: TextView = itemView.findViewById(R.id.downloadSpeedTextView)

    }

    class DownloadData {
        var id = 0
        var download: Download? = null
        var eta: Long = -1
        var downloadedBytesPerSecond: Long = 0
        override fun hashCode(): Int {
            return id
        }

        override fun toString(): String {
            return if (download == null) {
                ""
            } else download.toString()
        }

        override fun equals(obj: Any?): Boolean {
            return obj === this || obj is DownloadData && obj.id == id
        }
    }

    private fun getETAString(etaInMilliSeconds: Long, needLeft: Boolean = true): String {
        if (etaInMilliSeconds < 0) {
            return ""
        }
        var seconds = (etaInMilliSeconds / 1000).toInt()
        val hours = (seconds / 3600).toLong()
        seconds -= (hours * 3600).toInt()
        val minutes = (seconds / 60).toLong()
        seconds -= (minutes * 60).toInt()
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d hours", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d mins", minutes, seconds)
            else -> "$seconds secs"
        } + (if (needLeft) " left" else "")
    }

    private fun getDownloadSpeedString(downloadedBytesPerSecond: Long): String {
        if (downloadedBytesPerSecond < 0) {
            return ""
        }
        val kb = downloadedBytesPerSecond.toDouble() / 1000.toDouble()
        val mb = kb / 1000.toDouble()
        val gb = mb / 1000
        val tb = gb / 1000
        val decimalFormat = DecimalFormat(".##")
        return when {
            tb >= 1 -> "${decimalFormat.format(tb)} tb/s"
            gb >= 1 -> "${decimalFormat.format(gb)} gb/s"
            mb >= 1 -> "${decimalFormat.format(mb)} mb/s"
            kb >= 1 -> "${decimalFormat.format(kb)} kb/s"
            else -> "$downloadedBytesPerSecond b/s"
        }
    }

    private fun getMimeType(context: Context, uri: Uri): String {
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = mime.getExtensionFromMimeType(cR.getType(uri))
        if (type == null) {
            type = "*/*"
        }
        return type
    }

}

interface ActionListener {
    fun onPauseDownload(id: Int)
    fun onResumeDownload(id: Int)
    fun onRemoveDownload(id: Int)
    fun onRetryDownload(id: Int)
}