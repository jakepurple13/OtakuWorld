package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.flow.flow

const val USE_NAV3 = true

fun SourceRepository.loadItem(
    source: String,
    url: String,
) = toSourceByApiServiceName(source)
    ?.apiService
    ?.let { apiSource ->
        Cached.cache[url]?.let {
            flow {
                emit(
                    it
                        .toDbModel()
                        .toItemModel(apiSource)
                )
            }
        } ?: apiSource.getSourceByUrlFlow(url)
    }
    ?.dispatchIo()