package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.flow.combine

fun combineSources(
    sourceRepository: SourceRepository,
    dao: ItemDao,
) = combine(
    sourceRepository.sources,
    dao.getSourceOrder()
) { list, order ->
    list
        .filterNot { it.apiService.notWorking }
        .sortedBy { order.find { o -> o.source == it.packageName }?.order ?: 0 }
}