package com.programmersbox.uiviews.utils

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

fun <T> Flow<List<T>>.dispatchIoAndCatchList(action: (Throwable) -> Unit = {}) = this
    .dispatchIo()
    .catch {
        Firebase.crashlytics.recordException(it)
        it.printStackTrace()
        emit(emptyList())
        action(it)
    }

fun <T> Flow<T>.dispatchIo() = this.flowOn(Dispatchers.IO)