package com.programmersbox.kmpuiviews.presentation.history

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.programmersbox.favoritesdatabase.HistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn

class HistoryViewModel(private val dao: HistoryDao) : ViewModel() {
    val historyItems = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = true
        )
    ) { dao.getRecentlyViewedPaging() }
        .flow
        .flowOn(Dispatchers.IO)

    val historyCount = dao.getAllRecentHistoryCount()
}