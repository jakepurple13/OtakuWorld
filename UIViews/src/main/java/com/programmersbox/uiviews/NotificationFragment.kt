package com.programmersbox.uiviews

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.dragswipe.*
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.databinding.FragmentNotificationBinding
import com.programmersbox.uiviews.databinding.NotificationItemBinding
import com.programmersbox.uiviews.utils.BaseBottomSheetDialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class NotificationFragment : BaseBottomSheetDialogFragment() {

    private val genericInfo: GenericInfo by inject()

    private val db by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()

    private val adapter by lazy { NotificationAdapter(requireContext(), genericInfo, disposable) }

    private val notificationManager by lazy { requireContext().notificationManager }

    private lateinit var binding: FragmentNotificationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationRv.adapter = adapter

        DragSwipeUtils.setDragSwipeUp(
            adapter,
            binding.notificationRv,
            listOf(Direction.NOTHING),
            listOf(Direction.START, Direction.END),
            object : DragSwipeActions<NotificationItem> {
                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Direction,
                    dragSwipeAdapter: DragSwipeAdapter<NotificationItem, *>
                ) {
                    val item = dragSwipeAdapter[viewHolder.absoluteAdapterPosition]
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.removeNoti, item.notiTitle))
                        .setPositiveButton(R.string.yes) { d, _ ->
                            db.deleteNotification(dragSwipeAdapter.removeItem(viewHolder.absoluteAdapterPosition))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { d.dismiss() }
                                .addTo(disposable)
                            cancelNotification(item)
                        }
                        .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                        .setOnDismissListener { dragSwipeAdapter.notifyDataSetChanged() }
                        .show()
                }
            }
        )

        db.getAllNotificationCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it == 0 }
            .subscribeBy { findNavController().popBackStack() }
            .addTo(disposable)

        db.getAllNotificationsFlowable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { adapter.setListNotify(it) }
            .addTo(disposable)

        binding.multipleDeleteNotifications.setOnClickListener {

            val items = mutableListOf<NotificationItem>()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.removeNotification)
                .setCancelable(false)
                .setMultiChoiceItems(
                    adapter.dataList.map(NotificationItem::notiTitle).toTypedArray(),
                    null
                ) { _, i, b ->
                    val item = adapter.dataList[i]
                    if (b) items.add(item) else items.remove(item)
                }
                .setPositiveButton(R.string.yes) { d, _ ->
                    items.forEach {
                        db.deleteNotification(it)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                        cancelNotification(it)
                    }
                    d.dismiss()
                }
                .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                .show()
        }
    }

    private fun cancelNotification(item: NotificationItem) {
        notificationManager.cancel(item.id)
        val g = notificationManager.activeNotifications.map { it.notification }.filter { it.group == "otakuGroup" }
        if (g.size == 1) notificationManager.cancel(42)
    }

    class NotificationAdapter(
        private val context: Context,
        private val genericInfo: GenericInfo,
        private val disposable: CompositeDisposable
    ) : DragSwipeDiffUtilAdapter<NotificationItem, NotificationHolder>() {

        override val dragSwipeDiffUtil: (oldList: List<NotificationItem>, newList: Collection<NotificationItem>) -> DragSwipeDiffUtil<NotificationItem>
            get() = { old, new -> DragSwipeDiffUtil(old, new.toList()) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationHolder =
            NotificationHolder(NotificationItemBinding.inflate(context.layoutInflater, parent, false))

        override fun NotificationHolder.onBind(item: NotificationItem, position: Int) = bind(item, genericInfo, disposable)

    }

    class NotificationHolder(private val binding: NotificationItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem, info: GenericInfo, disposable: CompositeDisposable) {
            binding.item = item
            binding.root.setOnClickListener { v ->
                info.toSource(item.source)?.getSourceByUrl(item.url)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribeBy {
                        v.findNavController().navigate(NotificationFragmentDirections.actionNotificationFragmentToDetailsFragment(it))
                    }
                    ?.addTo(disposable)
            }
            binding.sendNotification.setOnClickListener {
                GlobalScope.launch {
                    SavedNotifications.viewNotificationFromDb(binding.root.context, item)
                }
            }
            binding.executePendingBindings()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

}