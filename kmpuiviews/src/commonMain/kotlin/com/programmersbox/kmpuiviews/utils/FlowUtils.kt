package com.programmersbox.kmpuiviews.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

fun <T> Flow<T>.dispatchIo() = this.flowOn(Dispatchers.IO)
