package com.programmersbox.koogintegration

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

class ModelManager(
    private val client: HttpClient,
    private val cacheDirectoryPath: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {

    fun hasModelDownloaded(
        fileName: String,
    ): Boolean {
        val cacheDir = cacheDirectoryPath.toPath()

        // Ensure the cache directory exists
        if (!fileSystem.exists(cacheDir)) {
            fileSystem.createDirectories(cacheDir)
        }

        val targetFile = cacheDir / fileName

        // 1. Check if the model is already in the cache
        return fileSystem.exists(targetFile)
    }

    /**
     * Checks if the model exists. If not, downloads it with progress updates.
     *
     * @param modelUrl The URL to download the model from.
     * @param fileName The name to save the file as (e.g., "model.tflite").
     * @param onProgress Callback providing downloaded bytes and total bytes.
     * @return The absolute file path of the downloaded model.
     */
    suspend fun getOrDownloadModel(
        modelUrl: String,
        fileName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long?) -> Unit,
    ): String {
        val cacheDir = cacheDirectoryPath.toPath()

        // Ensure the cache directory exists
        if (!fileSystem.exists(cacheDir)) {
            fileSystem.createDirectories(cacheDir)
        }

        val targetFile = cacheDir / fileName

        // 1. Check if the model is already in the cache
        if (fileSystem.exists(targetFile)) {
            // Trigger 100% progress for consistency if the UI relies on it
            fileSystem.metadata(targetFile).size?.let { size ->
                onProgress(size, size)
            }
            return targetFile.toString()
        }

        // 2. Download the model if it doesn't exist
        client.prepareGet(modelUrl) {
            onDownload { bytesSentTotal, contentLength ->
                onProgress(bytesSentTotal, contentLength)
            }
        }.execute { response ->
            val channel: ByteReadChannel = response.bodyAsChannel()

            fileSystem.sink(targetFile).buffer().use { sink ->
                val buffer = ByteArray(8192) // 8KB buffer chunks
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        sink.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        return targetFile.toString()
    }

    fun listModels(): List<DownloadedModel> {
        val cacheDir = cacheDirectoryPath.toPath()

        if (!fileSystem.exists(cacheDir)) {
            return emptyList()
        }

        return fileSystem.list(cacheDir).mapNotNull { path ->
            val metadata = fileSystem.metadataOrNull(path)

            // Only return regular files (ignore subdirectories if any exist)
            if (metadata != null && metadata.isRegularFile) {
                DownloadedModel(
                    fileName = path.name,
                    path = path.toString(),
                    sizeBytes = metadata.size ?: 0L,
                    lastModifiedEpochMillis = metadata.lastModifiedAtMillis ?: 0L
                )
            } else {
                null
            }
        }
    }

    /**
     * Deletes a model from the cache by its file name.
     * @return true if successful, false otherwise.
     */
    fun deleteModel(fileName: String): Boolean {
        val targetFile = cacheDirectoryPath.toPath() / fileName
        return try {
            if (fileSystem.exists(targetFile)) {
                fileSystem.delete(targetFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class DownloadedModel(
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
)