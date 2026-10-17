package com.programmersbox.anime.shared.downloads

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private const val FAILED_SUFFIX = ".failed.json"

/** A download that failed and is kept (request file + this marker) so the screen can offer Retry/Remove. */
@Serializable
data class DownloadFailure(val key: String, val title: String, val error: String, val atMs: Long)

/** Writes `<request name without .json>.failed.json` beside [requestFile]. */
fun writeFailure(requestFile: File, failure: DownloadFailure) {
    File(requestFile.parentFile, requestFile.nameWithoutExtension + FAILED_SUFFIX)
        .writeText(Json.encodeToString(failure))
}

/** Failures in [dir], newest first. Corrupt markers and markers without a request file are skipped. */
fun listFailures(dir: File): List<DownloadFailure> =
    dir.listFiles { f -> f.name.endsWith(FAILED_SUFFIX) }.orEmpty()
        .mapNotNull { marker ->
            val request = File(dir, marker.name.removeSuffix(FAILED_SUFFIX) + ".json")
            if (!request.exists()) return@mapNotNull null
            runCatching { Json.decodeFromString<DownloadFailure>(marker.readText()) }.getOrNull()
        }
        .sortedByDescending { it.atMs }

/** Deletes the failure marker for [key]; the request file is kept. */
fun clearFailure(dir: File, key: String) {
    File(dir, requestFileFor(dir, key).nameWithoutExtension + FAILED_SUFFIX).delete()
}

/** The request file [writeRequestFile] uses for [key]. */
fun requestFileFor(dir: File, key: String): File =
    File(dir, UUID.nameUUIDFromBytes(key.encodeToByteArray()).toString() + ".json")

/** What the worker does with the request file and failure marker at the end of a run. */
internal data class RequestOutcome(val deleteRequest: Boolean, val writeFailure: Boolean)

internal fun requestOutcome(succeeded: Boolean, failed: Boolean, isStopped: Boolean, cancelledByApp: Boolean): RequestOutcome =
    when {
        succeeded -> RequestOutcome(deleteRequest = true, writeFailure = false)
        failed -> RequestOutcome(deleteRequest = false, writeFailure = true)
        isStopped && !cancelledByApp -> RequestOutcome(deleteRequest = false, writeFailure = false)
        else -> RequestOutcome(deleteRequest = true, writeFailure = false)
    }
