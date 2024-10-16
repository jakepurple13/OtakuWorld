package com.programmersbox.uiviews.utils

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDao
import kotlinx.coroutines.flow.combine
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
inline fun <T> MutableList<T>.addNewList(@BuilderInference builderAction: MutableList<T>.() -> Unit): Boolean =
    addAll(buildList(builderAction))

fun recordFirebaseException(throwable: Throwable) = runCatching {
    Firebase.crashlytics.recordException(throwable)
}

fun logFirebaseMessage(message: String) = runCatching {
    Firebase.crashlytics.log(message)
}

fun combineSources(
    sourceRepository: SourceRepository,
    dao: ItemDao,
) = combine(
    sourceRepository.sources,
    dao.getSourceOrder()
) { list, order ->
    list.sortedBy { order.find { o -> o.source == it.packageName }?.order ?: 0 }
}