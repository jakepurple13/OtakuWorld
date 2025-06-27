package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import kotlinx.coroutines.flow.flow
import org.koin.core.definition.BeanDefinition
import org.koin.core.module.dsl.binds

const val USE_NAV3 = false

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

var shouldPrintLogs = false

inline fun printLogs(block: () -> Any?) {
    if (shouldPrintLogs) println(block())
}

fun <T : PlatformGenericInfo> BeanDefinition<T>.bindsGenericInfo() {
    binds(
        listOf(
            KmpGenericInfo::class,
            PlatformGenericInfo::class
        )
    )
}