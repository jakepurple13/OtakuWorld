package com.programmersbox.uiviews

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel

interface GenericInfo {

    fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder>

    fun createLayoutManager(context: Context): RecyclerView.LayoutManager

    fun chapterOnClick(model: ChapterModel, context: Context)

    fun sourceList(): List<ApiService>

    fun toSource(s: String): ApiService?

}