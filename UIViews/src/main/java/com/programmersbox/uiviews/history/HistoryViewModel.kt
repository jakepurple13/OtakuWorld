package com.programmersbox.uiviews.history

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.programmersbox.favoritesdatabase.HistoryDao
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class HistoryViewModel(private val dao: HistoryDao) : ViewModel() {
    val disposable = CompositeDisposable()
    val historyItems = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = true
        )
    ) { dao.getRecentlyViewedPaging() }
        .flow
        .flowOn(Dispatchers.IO)

    val historyCount = dao.getAllRecentHistoryCount()

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}