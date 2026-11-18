package com.programmersbox.kmpuiviews.workers

import android.graphics.Bitmap
import com.programmersbox.kmpuiviews.telemetry.koinCrashReporter
import android.graphics.BitmapFactory
import androidx.work.ListenableWorker
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
            koinCrashReporter().log("worker bitmap load failed")
            koinCrashReporter().recordException(it)
            it.printStackTrace()
        }
        .getOrNull()
}