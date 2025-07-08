package com.programmersbox.kmpuiviews.workers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.ListenableWorker
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.withTimeout

fun <T> Result<T>.workerReturn() = fold(
    onSuccess = { ListenableWorker.Result.success() },
    onFailure = { ListenableWorker.Result.failure() }
)

suspend fun HttpClient.getBitmapFromURL(strURL: String?, headers: Map<String, Any> = emptyMap()): Bitmap? {
    return runCatching {
        withTimeout(10000) {
            get(requireNotNull(strURL)) { headers.forEach { (key, value) -> header(key, value) } }
                .bodyAsBytes()
                .inputStream()
        }
    }
        .mapCatching { BitmapFactory.decodeStream(it) }
        .onFailure {
            logFirebaseMessage("Getting bitmap from $strURL")
            recordFirebaseException(it)
            it.printStackTrace()
        }
        .getOrNull()
}