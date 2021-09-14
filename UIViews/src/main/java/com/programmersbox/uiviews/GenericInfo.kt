package com.programmersbox.uiviews

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppUpdate

interface GenericInfo {

    val apkString: AppUpdate.AppUpdates.() -> String?

    fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, context: Context)
    fun sourceList(): List<ApiService>
    fun searchList(): List<ApiService> = sourceList()
    fun toSource(s: String): ApiService?
    fun customPreferences(preferenceScreen: SettingsDsl) = Unit
    fun downloadChapter(chapterModel: ChapterModel, title: String)

    @Composable
    fun ComposeShimmerItem()

    @Composable
    fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyListState,
        onClick: (ItemModel) -> Unit
    )

}