package com.programmersbox.kmpuiviews.utils

import com.programmersbox.kmpuiviews.recordFirebaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

fun <T> Flow<T>.dispatchIo() = this.flowOn(Dispatchers.IO)

fun <T> Flow<List<T>>.dispatchIoAndCatchList(action: suspend (Throwable) -> Unit = {}) = this
    .dispatchIo()
    .catch {
        recordFirebaseException(it)
        it.printStackTrace()
        emit(emptyList())
        action(it)
    }