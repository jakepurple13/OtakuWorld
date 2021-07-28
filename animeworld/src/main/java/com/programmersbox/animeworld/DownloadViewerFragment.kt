package com.programmersbox.animeworld

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.se_bastiaan.torrentstream.utils.FileUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.animeworld.adapters.JobQueueAdapter
import com.programmersbox.animeworld.adapters.PauseAdapter
import com.programmersbox.animeworld.databinding.DownloadViewerFragmentBinding
import com.programmersbox.animeworld.ytsdatabase.*
import com.programmersbox.dragswipe.*
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.utils.BaseBottomSheetDialogFragment
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewerFragment : BaseBottomSheetDialogFragment(), ActionListener {

    companion object {
        private const val UNKNOWN_REMAINING_TIME: Long = -1
        private const val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0
    }

    private val fetch: Fetch = Fetch.getDefaultInstance()
    private var fileAdapter: FileAdapter? = null

    private val pauseRepository: PauseRepository by lazy { PauseRepository(DownloadDatabase.getInstance(requireContext()).getPauseDao()) }

    private var jobQueueAdapter: JobQueueAdapter? = null
    private var pauseAdapter: PauseAdapter? = null

    private lateinit var binding: DownloadViewerFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DownloadViewerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileAdapter = FileAdapter(this, requireContext())

        jobQueueAdapter = JobQueueAdapter(requireContext())
        pauseAdapter = PauseAdapter(requireContext())

        pauseRepository.getAllPauseJob().observe(this) { pauseAdapter?.updateModels(it) }

        pauseAdapter?.setOnMoreListener = { v, model, i ->
            val popupMenu = PopupMenu(v.context, v)
            popupMenu.inflate(R.menu.torrent_pause_menu)
            popupMenu.setOnMenuItemClickListener {
                handlePauseMenu(it, model, i)
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        jobQueueAdapter?.setCloseClickListener { _, pos -> jobQueueAdapter?.removeItem(pos) }

        registerLocalBroadcast()

        val downloadsAdapter = ConcatAdapter(fileAdapter!!, jobQueueAdapter!!, pauseAdapter!!)

        binding.downloadList.adapter = downloadsAdapter

        ItemTouchHelper(itemCallback).attachToRecyclerView(binding.downloadList)

        /*DragSwipeUtils.setDragSwipeUp(
            fileAdapter!!,
            downloadList,
            listOf(Direction.NOTHING),
            listOf(Direction.START, Direction.END),
            DragSwipeActionBuilder {
                onSwiped { viewHolder, direction, dragSwipeAdapter ->
                    (dragSwipeAdapter as? FileAdapter)?.onItemDismiss(viewHolder.absoluteAdapterPosition, direction.value)
                }
            }
        )*/

        binding.multipleDownloadDelete.setOnClickListener {
            val downloadItems = mutableListOf<FileAdapter.DownloadData>()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMultiChoiceItems(fileAdapter!!.dataList.map { it.download?.file }.toTypedArray(), null) { _, i, b ->
                    if (b) downloadItems.add(fileAdapter!!.dataList[i]) else downloadItems.remove(fileAdapter!!.dataList[i])
                }
                .setPositiveButton(R.string.delete) { d, _ ->
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
        //LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private val itemCallback = object : ItemTouchHelper.SimpleCallback(Direction.NOTHING.value, Direction.START + Direction.END) {

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            when (viewHolder.bindingAdapter) {
                is FileAdapter -> {
                    fileAdapter?.onItemDismiss(viewHolder.absoluteAdapterPosition, direction)
                }
                is PauseAdapter -> {
                    lifecycleScope.launch {
                        pauseAdapter?.get(viewHolder.absoluteAdapterPosition)?.let {
                            it.saveLocation?.let { FileUtils.recursiveDelete(File(it)) }
                            pauseRepository.deletePause(it.hash)
                        }
                    }
                }
                is JobQueueAdapter -> {
                    jobQueueAdapter?.removeItem(viewHolder.absoluteAdapterPosition)
                    //t?.saveLocation?.let { FileUtils.recursiveDelete(File(it)) }
                    //t?.hash?.let { pauseRepository.deletePause(it) }
                }
            }
            //dragSwipeActions.onSwiped(viewHolder, Direction.getDirectionFromValue(direction), dragSwipeAdapter)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return true
        }

        override fun isLongPressDragEnabled(): Boolean = false//dragSwipeActions.isLongPressDragEnabled()
        override fun isItemViewSwipeEnabled(): Boolean = true//dragSwipeActions.isItemViewSwipeEnabled()

    }

    private val fetchListener = object : AbstractFetchListener() {
        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            fileAdapter!!.addDownload(download)
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onCompleted(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
            context?.notificationManager?.cancel(download.id)
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
            context?.notificationManager?.cancel(download.id)
        }

        override fun onRemoved(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onDeleted(download: Download) {
            fileAdapter!!.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
            context?.notificationManager?.cancel(download.id)
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

    private fun handlePauseMenu(it: MenuItem, model: Model.response_pause, i: Int) {
        when (it.itemId) {
            R.id.action_unpause -> {
                pauseRepository.deletePause(model.hash)

                val intent = Intent(this.requireActivity(), CommonBroadCast::class.java)
                intent.action = UNPAUSE_JOB
                intent.putExtra("model", model.torrent!!)
                activity?.sendBroadcast(intent)
            }
            R.id.action_deleteJob -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.do_you_want_to_delete)
                    .setMessage(R.string.delete)
                    .setPositiveButton(getString(R.string.yes)) { d, _ ->
                        model.saveLocation?.let { FileUtils.recursiveDelete(File(it)) }
                        pauseRepository.deletePause(model.hash)
                        d.dismiss()
                    }
                    .setNegativeButton(getString(R.string.no)) { d, _ ->
                        pauseRepository.deletePause(model.hash)
                        d.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun registerLocalBroadcast() {
        val filter = IntentFilter(MODEL_UPDATE)
        filter.addAction(PENDING_JOB_UPDATE)
        filter.addAction(EMPTY_QUEUE)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleReceiver(intent?.action, intent)
        }
    }

    private fun handleReceiver(action: String?, intent: Intent?) {
        when (action) {
            MODEL_UPDATE -> {
                try {
                    jobQueueAdapter?.setListNotify(emptyList())
                    val currentModels = intent?.getSerializableExtra("currentModel")
                    if (currentModels is Torrent) jobQueueAdapter?.addItem(currentModels)
                    /** Update pending models */
                    val torrents = intent?.getSerializableExtra("pendingModels")
                    if (torrents is ArrayList<*>) jobQueueAdapter?.addItems(torrents as ArrayList<Torrent>)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            PENDING_JOB_UPDATE -> {

                //DA_LOG("--- PENDING_JOB_UPDATE ---")
                jobQueueAdapter?.setListNotify(emptyList())
                pendingJobUpdate(intent)
            }
            EMPTY_QUEUE -> {

                //DA_LOG("--- EMPTY_QUEUE ---")

                //emptyQueue()
                jobQueueAdapter?.setListNotify(emptyList())
            }
        }
    }

    private fun pendingJobUpdate(intent: Intent?) {
        val jobs = intent?.getSerializableExtra("models") ?: return
        val models: ArrayList<Torrent> = jobs as ArrayList<Torrent>
        pendingJobUpdate(models)
    }

    private fun pendingJobUpdate(models: ArrayList<Torrent>) {
        jobQueueAdapter?.setListNotify(models)
    }

}