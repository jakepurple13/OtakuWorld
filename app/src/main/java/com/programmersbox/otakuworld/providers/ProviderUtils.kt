package com.programmersbox.otakuworld.providers

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch


internal fun <T> ContentResolver.observeUri(
    uri: Uri,
    getData: suspend (ContentResolver) -> T?,
) = callbackFlow<T> {
    launch {
        getData(this@observeUri)?.let { send(it) }
    }

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            launch(Dispatchers.IO) {
                getData(this@observeUri)?.let { send(it) }
            }
        }
    }

    registerContentObserver(uri, true, observer)

    awaitClose {
        unregisterContentObserver(observer)
    }
}