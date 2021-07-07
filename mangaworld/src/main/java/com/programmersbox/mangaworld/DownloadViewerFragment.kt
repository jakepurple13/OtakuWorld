package com.programmersbox.mangaworld

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.dragswipe.*
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.mangaworld.databinding.DownloadedItemBinding
import com.programmersbox.mangaworld.databinding.FragmentDownloadViewerBinding
import com.programmersbox.uiviews.utils.BaseBottomSheetDialogFragment
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.io.File

class DownloadViewerNavFragment : BaseBottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.download_navigation_fragment, container, false)
    }
}

class DownloadViewerFragment : BaseBottomSheetDialogFragment() {

    private lateinit var binding: FragmentDownloadViewerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDownloadViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val disposable = CompositeDisposable()

    private val defaultPathname
        get() =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/MangaWorld/")

    private val navArgs: DownloadViewerFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = DownloadedManga(requireContext())

        binding.chapterRv.adapter = adapter

        /*val c = ChaptersGet.getInstance(requireContext())

        c?.chapters
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe {
                println(it.joinToString("\n"))
            }
            ?.addTo(disposable)

        c?.loadChapters(lifecycleScope, (navArgs.pathname ?: defaultPathname).toUri())*/

        activity?.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
            if (p.isGranted) {
                Single.create<List<File>> {
                    (navArgs.pathname ?: defaultPathname)
                        .listFiles()
                        .also { println(it?.joinToString("\n") { it.name }) }
                        .orEmpty()
                        .toList()
                        .let(it::onSuccess)
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(adapter::addItems)
                    .addTo(disposable)
            }
        }

        DragSwipeUtils.setDragSwipeUp(
            recyclerView = binding.chapterRv,
            dragSwipeAdapter = adapter,
            swipeDirs = listOf(Direction.START, Direction.END),
            dragDirs = listOf(Direction.NOTHING),
            dragSwipeActions = object : DragSwipeActions<File> {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Direction, dragSwipeAdapter: DragSwipeAdapter<File, *>) {

                    val item = dragSwipeAdapter[viewHolder.absoluteAdapterPosition]

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.delete_title, item.name))
                        .setPositiveButton(R.string.delete) { d, _ ->
                            d.dismiss()
                            dragSwipeAdapter.removeItem(viewHolder.absoluteAdapterPosition)
                            Single.create<Boolean> { it.onSuccess(item.deleteRecursively()) }
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy { Toast.makeText(requireContext(), R.string.finished_deleting, Toast.LENGTH_SHORT).show() }
                                .addTo(disposable)
                        }
                        .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                        .show()

                }
            }
        )

        binding.multipleMangaDelete.setOnClickListener {
            val list = mutableListOf<File>()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_multiple)
                .setMultiChoiceItems(adapter.dataList.map(File::getName).toTypedArray(), null) { _, i, b ->
                    val action = if (b) list::add else list::remove
                    adapter.dataList[i].let(action)
                }
                .setPositiveButton(R.string.delete) { d, _ ->
                    d.dismiss()
                    list.forEach { adapter.removeItem(it) }
                    Completable.create {
                        list.forEach { it.deleteRecursively() }
                        it.onComplete()
                    }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy { Toast.makeText(requireContext(), R.string.finished_deleting, Toast.LENGTH_SHORT).show() }
                        .addTo(disposable)
                }
                .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    inner class DownloadedManga(private val context: Context) : DragSwipeAdapter<File, DownloadedHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadedHolder =
            DownloadedHolder(DownloadedItemBinding.inflate(context.layoutInflater, parent, false))

        override fun DownloadedHolder.onBind(item: File, position: Int) = bind(item)

        fun removeItem(item: File) {
            val position = dataList.indexOf(item)
            dataList.remove(item)
            notifyItemRemoved(position)
        }
    }

    inner class DownloadedHolder(private val binding: DownloadedItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: File) {
            binding.root.setOnClickListener {
                if (item.listFiles()?.all(File::isFile) == true)
                    activity?.startActivity(
                        Intent(it.context, ReadActivity::class.java).apply {
                            putExtra("downloaded", true)
                            putExtra("filePath", item)
                        }
                    )
                else
                    findNavController().navigate(DownloadViewerFragmentDirections.actionDownloadViewerFragmentSelf(item))
            }
            binding.file = item
            binding.executePendingBindings()
        }
    }

}