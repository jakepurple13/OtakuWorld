package com.programmersbox.animeworld.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.programmersbox.animeworld.databinding.ItemTorrentDownloadBinding
import com.programmersbox.animeworld.ytsdatabase.Model
import com.programmersbox.animeworld.ytsdatabase.TorrentJob
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.helpfulutils.layoutInflater
import java.text.DecimalFormat

class PauseAdapter(private val context: Context) : DragSwipeAdapter<Model.response_pause, PauseAdapter.PauseHolder>() {

    lateinit var setOnMoreListener: (View, Model.response_pause, Int) -> Unit

    class PauseHolder(val binding: ItemTorrentDownloadBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PauseHolder(ItemTorrentDownloadBinding.inflate(parent.context.layoutInflater, parent, false))

    override fun PauseHolder.onBind(item: Model.response_pause, position: Int) {
        Glide.with(context).load(item.job.bannerUrl).into(binding.itemImage)

        binding.itemTitle.text = item.job.title
        binding.itemStatus.text = "Paused"
        binding.itemProgress.text = "${item.job.progress}%"
        binding.itemSeedsPeers.text = "0/0"
        binding.itemProgressBar.progress = item.job.progress
        binding.itemCurrentSize.text = calculateCurrentSize(item.job)
        binding.itemTotalSize.text = getSizePretty(item.job.totalSize)
        binding.itemDownloadSpeed.text = "0 KB/s"
        binding.itemMoreImageView.setOnClickListener { setOnMoreListener.invoke(it, item, position) }
    }

    fun updateModels(models: List<Model.response_pause>) {
        dataList.clear()
        dataList.addAll(models)
        notifyDataSetChanged()
    }

    private fun calculateCurrentSize(torrentJob: TorrentJob): String? {
        val currentSize =
            (torrentJob.progress.toLong() * (torrentJob.totalSize as Long)) / (100).toLong()
        return getSizePretty(currentSize)
    }

    private fun getSizePretty(size: Long?, addPrefix: Boolean = true): String? {
        val df = DecimalFormat("0.00")
        val sizeKb = 1024.0f
        val sizeMb = sizeKb * sizeKb
        val sizeGb = sizeMb * sizeKb
        val sizeTerra = sizeGb * sizeKb
        return if (size != null) {
            when {
                size < sizeMb -> df.format(size / sizeKb)
                    .toString() + if (addPrefix) " KB" else ""
                size < sizeGb -> df.format(
                    size / sizeMb
                ).toString() + " MB"
                size < sizeTerra -> df.format(size / sizeGb)
                    .toString() + if (addPrefix) " GB" else ""
                else -> ""
            }
        } else "0" + if (addPrefix) " B" else ""
    }

}