package com.programmersbox.uiviews.recent

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import com.programmersbox.uiviews.utils.showErrorToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecentViewModel(dao: ItemDao, context: Context? = null) : ViewModel() {

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<ItemModel>()
    val favoriteList = mutableStateListOf<DbModel>()

    var count = 1

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val itemListener = FirebaseDb.FirebaseListener()

    init {
        viewModelScope.launch {
            combine(
                itemListener.getAllShowsFlow(),
                dao.getAllFavoritesFlow()
            ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
                .collect {
                    favoriteList.clear()
                    favoriteList.addAll(it)
                }
        }

        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                sourceList.clear()
                sourceLoadCompose(context, it)
            }
            .addTo(disposable)
    }

    fun reset(context: Context?, sources: ApiService) {
        count = 1
        sourceList.clear()
        sourceLoadCompose(context, sources)
    }

    fun loadMore(context: Context?, sources: ApiService) {
        count++
        sourceLoadCompose(context, sources)
    }

    private fun sourceLoadCompose(context: Context?, sources: ApiService) {
        viewModelScope.launch {
            sources
                .getRecentFlow(count)
                .dispatchIoAndCatchList()
                .catch {
                    context?.showErrorToast()
                    emit(emptyList())
                }
                .onStart { isRefreshing = true }
                .onCompletion { isRefreshing = false }
                .onEach { sourceList.addAll(it) }
                .collect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
        disposable.dispose()
    }

}