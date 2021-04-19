package com.programmersbox.animeworld.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.programmersbox.animeworld.R
import com.programmersbox.animeworld.databinding.ItemTorrentDownload1Binding
import com.programmersbox.animeworld.ytsdatabase.Torrent
import com.programmersbox.dragswipe.DragSwipeAdapter

class JobQueueAdapter(private val context: Context) : DragSwipeAdapter<Torrent, JobQueueAdapter.JobHolder>() {

    private lateinit var listener: CloseClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobHolder {
        return JobHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_torrent_download_1, parent, false))
    }

    override fun JobHolder.onBind(item: Torrent, position: Int) {
        //binding.itemTitle.text = item.title
        binding.torrent = item
        Glide.with(context.applicationContext).load(item.banner_url).into(binding.itemImage)

        binding.itemClose.setOnClickListener { listener.onClick(item, position) }
    }

    fun interface CloseClickListener {
        fun onClick(model: Torrent, pos: Int)
    }

    fun setCloseClickListener(listener: CloseClickListener) {
        this.listener = listener
    }

    class JobHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemTorrentDownload1Binding.bind(view)
    }
}