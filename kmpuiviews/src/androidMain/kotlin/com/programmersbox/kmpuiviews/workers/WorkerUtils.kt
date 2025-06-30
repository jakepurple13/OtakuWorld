package com.programmersbox.kmpuiviews.workers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.ListenableWorker
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import java.net.HttpURLConnection
import java.net.URL

fun <T> Result<T>.workerReturn() = fold(
    onSuccess = { ListenableWorker.Result.success() },
    onFailure = { ListenableWorker.Result.failure() }
)

fun getBitmapFromURL(strURL: String?, headers: Map<String, Any> = emptyMap()): Bitmap? = runCatching {
    val url = URL(strURL)
    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
    headers.forEach { connection.setRequestProperty(it.key, it.value.toString()) }
    connection.doInput = true
    connection.connect()
    BitmapFactory.decodeStream(connection.inputStream)
}
    .onFailure {
        logFirebaseMessage("Getting bitmap from $strURL")
        recordFirebaseException(it)
        it.printStackTrace()
    }
    .getOrNull()