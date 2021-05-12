package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView

abstract class BaseListFragment : BaseFragment(), GenericInfo by BaseMainActivity.genericInfo {
    protected lateinit var adapter: ItemListAdapter<RecyclerView.ViewHolder>

    @CallSuper
    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = createAdapter(this@BaseListFragment.requireContext(), this)
    }
}