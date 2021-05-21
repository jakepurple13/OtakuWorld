package com.programmersbox.animeworld

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.dragswipe.*
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.stringForTime
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

class ViewVideosFragment : BaseBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_videos, container, false)
    }

    private val adapter by lazy { VideoAdapter(requireContext(), MainActivity.cast) }

    private val disposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivity.cast.setMediaRouteMenu(requireContext(), view.findViewById<MaterialToolbar>(R.id.toolbarmenu).menu)
        val videoRv = view.findViewById<RecyclerView>(R.id.videoRv)
        videoRv.adapter = adapter
        DragSwipeUtils.setDragSwipeUp(
            adapter,
            videoRv,
            listOf(Direction.NOTHING),
            listOf(Direction.START, Direction.END),
            DragSwipeActionBuilder {
                onSwiped { viewHolder, _, dragSwipeAdapter ->
                    val listener: DeleteDialog.DeleteDialogListener = object : DeleteDialog.DeleteDialogListener {
                        override fun onDelete() {
                            val file = File(dragSwipeAdapter.removeItem(viewHolder.absoluteAdapterPosition).path!!)
                            if (file.exists()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    dragSwipeAdapter.removeItem(viewHolder.absoluteAdapterPosition).let {
                                        it.assetFileStringUri?.toUri()?.let { it1 ->
                                            context?.contentResolver?.delete(
                                                it1,
                                                "${MediaStore.Video.Media._ID} = ?",
                                                arrayOf(it.videoId.toString())
                                            )
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        if (file.delete()) R.string.fileDeleted else R.string.fileNotDeleted,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        }

                        override fun onCancel() {
                            dragSwipeAdapter.notifyDataSetChanged()
                        }
                    }
                    DeleteDialog(
                        context,
                        dragSwipeAdapter[viewHolder.absoluteAdapterPosition].videoName.orEmpty(),
                        null,
                        File(dragSwipeAdapter.dataList[viewHolder.absoluteAdapterPosition].path!!),
                        listener
                    ).show()
                }
            }
        )

        loadVideos()
        view.findViewById<SwipeRefreshLayout>(R.id.view_video_refresh).isEnabled = false//setOnRefreshListener { loadVideos() }

        view.findViewById<MaterialButton>(R.id.multiple_video_delete).setOnClickListener {
            val downloadItems = mutableListOf<String>()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMultiChoiceItems(adapter.dataList.map { it.videoName }.toTypedArray(), null) { _, i, b ->
                    if (b) downloadItems.add(adapter.dataList[i].path!!) else downloadItems.remove(adapter.dataList[i].path!!)
                }
                .setPositiveButton(R.string.delete) { d, _ ->
                    downloadItems.forEach { f ->
                        adapter.dataList.indexOfFirst { it.path == f }
                            .let { if (it != -1) adapter.removeItem(it) else null }
                            ?.let {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    it.let {
                                        it.assetFileStringUri?.toUri()?.let { it1 ->
                                            context?.contentResolver?.delete(
                                                it1,
                                                "${MediaStore.Video.Media._ID} = ?",
                                                arrayOf(it.videoId.toString())
                                            )
                                        }
                                    }
                                } else {
                                    File(it.path!!).delete()
                                }
                            }
                    }
                    //MainActivity.cast.stopCast()
                    //adapter.notifyDataSetChanged()
                    d.dismiss()
                }
                .show()
        }
    }

    private fun loadVideos() {
        view?.findViewById<SwipeRefreshLayout>(R.id.view_video_refresh)?.isRefreshing = true
        val permissions = listOfNotNull(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        activity?.requestPermissions(*permissions.toTypedArray()) {
            if (it.isGranted) getStuff()
        }
    }

    private fun getStuff() {

        val v = VideoGet.getInstance(requireContext())
        v
            ?.videos
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe {

                val prefs = requireContext().getSharedPreferences("videos", Context.MODE_PRIVATE).all.keys
                val fileRegex = "(\\/[^*|\"<>?\\n]*)|(\\\\\\\\.*?\\\\.*)".toRegex()
                val filePrefs = prefs.filter { fileRegex.containsMatchIn(it) }
                for (p in filePrefs) {
                    //Loged.i(p)
                    if (!it.any { it.path == p }) {
                        requireContext().getSharedPreferences("videos", Context.MODE_PRIVATE).edit().remove(p).apply()
                    }
                }
                adapter.setListNotify(it)
                view?.findViewById<SwipeRefreshLayout>(R.id.view_video_refresh)?.isRefreshing = false
            }
            ?.addTo(disposable)

        v?.loadVideos(lifecycleScope, VideoGet.externalContentUri)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        VideoGet.getInstance(requireContext())?.unregister()
    }

    class VideoAdapter(private val context: Context, private val cast: CastHelper) : DragSwipeDiffUtilAdapter<VideoContent, VideoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder =
            VideoHolder(context.layoutInflater.inflate(R.layout.video_layout, parent, false))

        override val dragSwipeDiffUtil: (oldList: List<VideoContent>, newList: Collection<VideoContent>) -> DragSwipeDiffUtil<VideoContent>
            get() = { oldList, newList -> DragSwipeDiffUtil(oldList, newList.toList()) }

        @SuppressLint("SetTextI18n")
        override fun VideoHolder.onBind(item: VideoContent, position: Int) {
            val duration = item.videoDuration

            /*convert millis to appropriate time*/
            val runTimeString = if (duration > TimeUnit.HOURS.toMillis(1)) {
                String.format(
                    "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(duration),
                    TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                    TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
                )
            } else {
                String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
                )
            }

            videoRuntime.text = runTimeString
            videoName.text = "${item.videoName} ${
                if (context.getSharedPreferences("videos", Context.MODE_PRIVATE).contains(item.path)) "\nat ${
                    context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(item.path, 0).stringForTime()
                }" else ""
            }"

            itemView.setOnClickListener {
                if (cast.isCastActive()) {
                    cast.loadMedia(
                        File(item.path!!),
                        context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(item.path, 0),
                        null, null
                    )
                } else {
                    context.startActivity(
                        Intent(context, VideoPlayerActivity::class.java).apply {
                            putExtra("showPath", item.assetFileStringUri)
                            putExtra("showName", item.videoName)
                            putExtra("downloadOrStream", true)
                        }
                    )
                }
            }

            Glide.with(context)
                .load(item.assetFileStringUri)
                .override(360, 270)
                .thumbnail(0.5f)
                .transform(GranularRoundedCorners(0f, 15f, 15f, 0f))
                .into(thumbnail)

        }

    }

    class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView by lazy { itemView.findViewById(R.id.video_thumbnail) }
        val videoName: TextView by lazy { itemView.findViewById(R.id.video_name) }
        val videoRuntime: TextView by lazy { itemView.findViewById(R.id.video_runtime) }
    }

}