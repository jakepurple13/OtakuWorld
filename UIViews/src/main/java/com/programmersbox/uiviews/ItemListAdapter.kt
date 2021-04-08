package com.programmersbox.uiviews

import android.content.Context
import android.view.View
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.dragswipe.CheckAdapter
import com.programmersbox.dragswipe.CheckAdapterInterface
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.models.ItemModel

abstract class ItemListAdapter<VH : RecyclerView.ViewHolder>(
    protected val context: Context,
    private val baseListFragment: BaseListFragment,
    check: CheckAdapter<ItemModel, DbModel> = CheckAdapter()
) : DragSwipeAdapter<ItemModel, VH>(), CheckAdapterInterface<ItemModel, DbModel> by check {
    init {
        check.adapter = this
    }

    protected fun onClick(v: View, itemModel: ItemModel) {
        val direction = if (baseListFragment is RecentFragment) RecentFragmentDirections.actionRecentFragment2ToDetailsFragment2(itemModel)
        else AllFragmentDirections.actionAllFragment2ToDetailsFragment3(itemModel)
        v.findNavController().navigate(direction)
    }
}