package com.programmersbox.anime.shared.downloads

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.kmpmodels.KmpChapterModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

class AnimeDownloadManager(
    private val scope: CoroutineScope,
    private val animeDesktopSettings: AnimeDesktopSettings,
    private val trayState: TrayState,
) {

    private val httpClient = HttpClient()

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion { httpClient.close() }
    }

    fun downloadChapter(chapter: KmpChapterModel, animeTitle: String) {
        scope.launch {
            val storage = chapter.getChapterInfo().firstOrNull()?.firstOrNull { it.link != null }
            val link = storage?.link
            if (storage == null || link == null) {
                notify("Download Failed", "${chapter.name}: no downloadable stream found", isError = true)
                return@launch
            }

            val rootDir = animeDesktopSettings.downloadsDirectory.get()
            val destDir = File(rootDir, animeTitle.sanitizeForPath()).also { it.mkdirs() }
            val destFile = File(destDir, "${chapter.name.sanitizeForPath()}.mp4")

            try {
                val bytes: ByteArray = httpClient.get(link) {
                    headers { storage.headers.forEach { (key, value) -> append(key, value) } }
                }.body()
                destFile.writeBytes(bytes)
                notify("Downloaded", "$animeTitle — ${chapter.name}", isError = false)
            } catch (e: Exception) {
                notify("Download Failed", "${chapter.name}: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun notify(title: String, message: String, isError: Boolean) {
        withContext(Dispatchers.Main) {
            trayState.sendNotification(
                Notification(
                    title = title,
                    message = message,
                    type = if (isError) Notification.Type.Error else Notification.Type.Info
                )
            )
        }
    }
}

private fun String.sanitizeForPath(): String = replace(Regex("[\\\\/:*?\"<>|]"), "_")
