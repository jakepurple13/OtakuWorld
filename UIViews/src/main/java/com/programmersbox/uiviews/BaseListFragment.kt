package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.inject

abstract class BaseListFragment : BaseFragment() {
    protected lateinit var adapter: ItemListAdapter<RecyclerView.ViewHolder>

    protected val info: GenericInfo by inject()

    @CallSuper
    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = info.createAdapter(this@BaseListFragment.requireContext(), this)
    }
}