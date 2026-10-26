package com.programmersbox.anime.shared.downloads

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.roundToLong

private const val SEGMENT_RETRIES = 3
private const val BUFFER_SIZE = 64 * 1024

/** Share of the progress bar that the merge step fills, after both tracks are downloaded. */
private const val MERGE_SHARE = 0.05f

/**
 * Downloads a static DASH stream into one playable file, sending the given headers on every request.
 *
 * The representations picked by [choose] are fetched segment by segment (each retried up to
 * [SEGMENT_RETRIES] times). Separate video and audio tracks go to `<out>.tracks/` and are then merged
 * into a fragmented MP4 with both tracks at `out`; a muxed or video-only representation is written
 * to `out` directly. The `.tracks` directory is deleted whatever happens, and `out` too on failure
 * or cancellation.
 */
class DashDownloader(
    private val client: OkHttpClient,
    private val retryDelayMillis: Long = 1_000,
) {

    /** Downloads the chosen representation(s) with [headers] on every request and writes ONE playable file at [out]. */
    suspend fun download(manifestUrl: String, headers: Map<String, String>, out: File, onProgress: (Float) -> Unit) =
        downloadCountingBytes(manifestUrl, headers, out) { fraction, _ -> onProgress(fraction) }

    /** [download], also reporting the bytes downloaded so far. */
    internal suspend fun downloadCountingBytes(
        manifestUrl: String,
        headers: Map<String, String>,
        out: File,
        onProgress: (fraction: Float, bytes: Long) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        val tracks = File(out.path + ".tracks")
        try {
            val (xml, finalUrl) = client.execute(manifestUrl, headers).use { it.body.string() to it.request.url.toString() }
            val (video, audio) = parseDashManifest(xml, finalUrl).choose()
            if (audio == null) {
                val progress = Progress(video.requestCount, downloadShare = 1f, onProgress)
                fetchTrack(video, headers, out, progress)
            } else {
                listOf(video, audio).forEach {
                    if (it.mimeType.substringAfter('/') == "webm") {
                        throw UnsupportedOperationException("WebM DASH tracks cannot be merged")
                    }
                }
                val progress = Progress(video.requestCount + audio.requestCount, downloadShare = 1f - MERGE_SHARE, onProgress)
                tracks.mkdirs()
                val videoFile = File(tracks, "video.mp4")
                val audioFile = File(tracks, "audio.mp4")
                fetchTrack(video, headers, videoFile, progress)
                fetchTrack(audio, headers, audioFile, progress)
                mergeFragmentedMp4(videoFile, audioFile, out, progress::merged)
            }
            onProgress(1f, out.length())
        } catch (t: Throwable) {
            out.delete()
            throw t
        } finally {
            tracks.deleteRecursively()
        }
    }

    private val DashRepresentation.requestCount get() = segmentUrls.size + if (initUrl != null) 1 else 0

    /** Download progress by requests (partial requests by bytes when the length is known), then the merge. */
    private class Progress(
        private val requests: Int,
        private val downloadShare: Float,
        private val report: (Float, Long) -> Unit,
    ) {
        var bytes = 0L
        private var done = 0

        fun request(partial: Float) = report(downloadShare * (done + partial) / requests, bytes)

        fun requestDone() {
            done++
            request(0f)
        }

        fun merged(fraction: Float) = report(downloadShare + (1 - downloadShare) * fraction, bytes)
    }

    private suspend fun fetchTrack(rep: DashRepresentation, headers: Map<String, String>, target: File, progress: Progress) {
        FileOutputStream(target).use { file ->
            for (url in listOfNotNull(rep.initUrl) + rep.segmentUrls) {
                fetchWithRetry(url, headers, file, progress)
                progress.requestDone()
            }
        }
    }

    /** Appends [url] to [file]; a failed attempt is truncated away and retried. */
    private suspend fun fetchWithRetry(url: String, headers: Map<String, String>, file: FileOutputStream, progress: Progress) {
        val start = file.channel.position()
        val bytesBefore = progress.bytes
        var attempt = 0
        while (true) {
            try {
                client.execute(url, headers).use { response ->
                    val length = response.body.contentLength()
                    response.body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            file.write(buffer, 0, n)
                            read += n
                            progress.bytes = bytesBefore + read
                            if (length > 0) progress.request((read.toFloat() / length).coerceAtMost(1f))
                        }
                    }
                }
                return
            } catch (e: IOException) {
                if (attempt >= SEGMENT_RETRIES) throw e
                attempt++
                file.channel.truncate(start)
                file.channel.position(start)
                progress.bytes = bytesBefore
                delay(retryDelayMillis * attempt)
            }
        }
    }
}

// region fragmented MP4 merge

/** An MP4 box at [offset] (in a file or a byte array); [size] includes the [header]. */
private class Box(val type: String, val offset: Long, val size: Long, val header: Int) {
    val body get() = offset + header
    val end get() = offset + size
}

/** A `moof` box and its media data, which ends at [end]; [time] orders it against the other track. */
private class Fragment(val moof: Box, val end: Long, val time: Double)

private class Track(val file: RandomAccessFile, val ftyp: Box?, val moov: ByteArray, val fragments: List<Fragment>)

/**
 * Merges two single-track fragmented MP4 files into [out]: one `moov` with the video track (ID 1) and
 * the audio track (ID 2), then both tracks' `moof`/`mdat` pairs interleaved by decode time. Segment
 * boxes that only make sense per track (`styp`, `sidx`) are dropped. Media data is copied file to file.
 */
private suspend fun mergeFragmentedMp4(videoFile: File, audioFile: File, out: File, onProgress: (Float) -> Unit) {
    RandomAccessFile(videoFile, "r").use { videoRaf ->
        RandomAccessFile(audioFile, "r").use { audioRaf ->
            val video = readTrack(videoRaf)
            val audio = readTrack(audioRaf)
            val order = (video.fragments.map { video to it } + audio.fragments.map { audio to it })
                .sortedBy { (_, fragment) -> fragment.time }
            FileOutputStream(out).use { stream ->
                val channel = stream.channel
                video.ftyp?.let { stream.write(video.file.read(it)) }
                stream.write(mergeMoov(video.moov, audio.moov))
                order.forEachIndexed { index, (track, fragment) ->
                    currentCoroutineContext().ensureActive()
                    val moof = track.file.read(fragment.moof)
                    rewriteMoof(moof, sequence = index + 1, trackId = if (track === video) 1 else 2, shift = channel.position() - fragment.moof.offset)
                    stream.write(moof)
                    track.file.channel.copyTo(channel, fragment.moof.end, fragment.end - fragment.moof.end)
                    onProgress((index + 1).toFloat() / order.size)
                }
            }
        }
    }
}

private fun readTrack(file: RandomAccessFile): Track {
    val boxes = topLevelBoxes(file)
    val moov = file.read(boxes.firstOrNull { it.type == "moov" } ?: throw IOException("DASH track has no moov box"))
    if (moov.childrenOf(moov.root()).count { it.type == "trak" } != 1) throw IOException("DASH track must hold exactly one track")
    val timescale = moov.find(moov.root(), "trak", "mdia", "mdhd")?.let { moov.timescaleOf(it) }
        ?: throw IOException("DASH track has no mdhd box")

    val moofs = boxes.withIndex().filter { it.value.type == "moof" }
    if (moofs.isEmpty()) throw UnsupportedOperationException("Only fragmented MP4 DASH tracks can be merged")
    val decodeTimes = moofs.map { (_, moof) ->
        val bytes = file.read(moof)
        bytes.find(bytes.root(), "traf", "tfdt")?.let { tfdt ->
            val body = tfdt.body.toInt()
            if (bytes[body].toInt() == 1) bytes.long(body + 4) else bytes.uint(body + 4)
        }
    }
    val fragments = moofs.mapIndexed { i, (index, moof) ->
        val mdat = boxes.drop(index + 1).firstOrNull { it.type == "mdat" || it.type == "moof" }
            ?.takeIf { it.type == "mdat" } ?: throw IOException("DASH fragment without mdat")
        // Without decode times, interleave by position in the track instead.
        val time = if (decodeTimes.all { it != null }) decodeTimes[i]!!.toDouble() / timescale else i.toDouble() / moofs.size
        Fragment(moof, mdat.end, time)
    }
    return Track(file, boxes.firstOrNull { it.type == "ftyp" }, moov, fragments)
}

/** The video `moov` with the audio `trak` added after the video one, track IDs 1 and 2, and a `trex` for each. */
private fun mergeMoov(videoMoov: ByteArray, audioMoov: ByteArray): ByteArray {
    val videoChildren = videoMoov.childrenOf(videoMoov.root())
    val videoMvhd = videoChildren.single { it.type == "mvhd" }
    val audioMvhd = audioMoov.childrenOf(audioMoov.root()).single { it.type == "mvhd" }

    val mvhd = videoMoov.copy(videoMvhd).also { it.putUint(it.size - 4, 3) } // next_track_ID
    val videoTrak = videoMoov.copy(videoChildren.single { it.type == "trak" }).also { setTkhdTrackId(it, 1) }
    val audioTrak = audioMoov.copy(audioMoov.find(audioMoov.root(), "trak")!!).also { trak ->
        setTkhdTrackId(trak, 2)
        rescaleToMovie(trak, from = audioMoov.timescaleOf(audioMvhd), to = videoMoov.timescaleOf(videoMvhd))
    }
    val mehd = videoMoov.find(videoMoov.root(), "mvex", "mehd")?.let(videoMoov::copy)
    val trexes = listOf(videoMoov, audioMoov).mapIndexed { i, moov ->
        val trex = moov.find(moov.root(), "mvex", "trex") ?: throw IOException("DASH track has no trex box")
        moov.copy(trex).also { it.putUint(trex.header + 4, i + 1L) }
    }
    val mvex = box("mvex", listOfNotNull(mehd) + trexes)
    val rest = videoChildren.filter { it.type != "mvhd" && it.type != "trak" && it.type != "mvex" }.map(videoMoov::copy)
    return box("moov", listOf(mvhd, videoTrak, audioTrak, mvex) + rest)
}

private fun setTkhdTrackId(trak: ByteArray, id: Long) {
    val tkhd = trak.find(trak.root(), "tkhd") ?: throw IOException("DASH track has no tkhd box")
    val body = tkhd.body.toInt()
    trak.putUint(body + if (trak[body].toInt() == 1) 20 else 12, id)
}

/** Converts the movie-timescale durations of a `trak` (in `tkhd` and `elst`) from [from] to [to]. */
private fun rescaleToMovie(trak: ByteArray, from: Long, to: Long) {
    if (from == to || from == 0L) return
    fun scale(value: Long) = (value.toDouble() * to / from).roundToLong()
    trak.find(trak.root(), "tkhd")?.let { tkhd ->
        val body = tkhd.body.toInt()
        if (trak[body].toInt() == 1) trak.putLong(body + 28, scale(trak.long(body + 28)))
        else trak.uint(body + 20).takeIf { it != 0xFFFFFFFFL }?.let { trak.putUint(body + 20, scale(it)) }
    }
    trak.find(trak.root(), "edts", "elst")?.let { elst ->
        val body = elst.body.toInt()
        val v1 = trak[body].toInt() == 1
        val entries = trak.uint(body + 4).toInt()
        repeat(entries) { i ->
            val at = body + 8 + i * (if (v1) 20 else 12)
            if (v1) trak.putLong(at, scale(trak.long(at))) else trak.putUint(at, scale(trak.uint(at)))
        }
    }
}

/**
 * Renumbers a `moof` copied to a new file: `mfhd` gets [sequence], every `tfhd` gets [trackId], and
 * absolute `base_data_offset`s move by [shift]. Offsets relative to the `moof` stay valid because its
 * media data is copied right after it.
 */
private fun rewriteMoof(moof: ByteArray, sequence: Int, trackId: Int, shift: Long) {
    val root = moof.root()
    moof.find(root, "mfhd")?.let { moof.putUint(it.body.toInt() + 4, sequence.toLong()) }
    for (traf in moof.childrenOf(root).filter { it.type == "traf" }) {
        val tfhd = moof.childrenOf(traf).firstOrNull { it.type == "tfhd" } ?: throw IOException("DASH fragment without tfhd")
        val body = tfhd.body.toInt()
        moof.putUint(body + 4, trackId.toLong())
        val baseDataOffsetPresent = moof.int(body) and 0x1 != 0
        if (baseDataOffsetPresent) moof.putLong(body + 8, moof.long(body + 8) + shift)
    }
}

private fun topLevelBoxes(file: RandomAccessFile): List<Box> {
    val length = file.length()
    val boxes = mutableListOf<Box>()
    var pos = 0L
    while (pos + 8 <= length) {
        file.seek(pos)
        var size = file.readInt().toLong() and 0xFFFFFFFFL
        val type = ByteArray(4).also(file::readFully).toString(Charsets.ISO_8859_1)
        var header = 8
        when (size) {
            1L -> { size = file.readLong(); header = 16 }
            0L -> size = length - pos
        }
        if (size < header || pos + size > length) throw IOException("Malformed MP4 box '$type' at $pos")
        boxes += Box(type, pos, size, header)
        pos += size
    }
    return boxes
}

private fun RandomAccessFile.read(box: Box): ByteArray {
    if (box.size > Int.MAX_VALUE) throw IOException("MP4 '${box.type}' box too large")
    seek(box.offset)
    return ByteArray(box.size.toInt()).also(::readFully)
}

private fun FileChannel.copyTo(target: FileChannel, position: Long, count: Long) {
    var copied = 0L
    while (copied < count) {
        val n = transferTo(position + copied, count - copied, target)
        if (n <= 0) throw IOException("Could not copy MP4 media data")
        copied += n
    }
}

// In-memory boxes: a whole box (header included) held in a ByteArray, with offsets into it.

private fun ByteArray.root(): Box {
    val size = uint(0)
    return if (size == 1L) Box(type(0), 0, size, 16) else Box(type(0), 0, size.takeIf { it != 0L } ?: this.size.toLong(), 8)
}

private fun ByteArray.type(at: Int) = String(this, at + 4, 4, Charsets.ISO_8859_1)

private fun ByteArray.childrenOf(parent: Box): List<Box> {
    val boxes = mutableListOf<Box>()
    var pos = parent.body.toInt()
    val end = parent.end.toInt()
    while (pos + 8 <= end) {
        var size = uint(pos)
        var header = 8
        when (size) {
            1L -> { size = long(pos + 8); header = 16 }
            0L -> size = (end - pos).toLong()
        }
        if (size < header || pos + size > end) throw IOException("Malformed MP4 box '${type(pos)}'")
        boxes += Box(type(pos), pos.toLong(), size, header)
        pos += size.toInt()
    }
    return boxes
}

/** The first box along [path] below [parent]. */
private fun ByteArray.find(parent: Box, vararg path: String): Box? =
    path.fold(parent as Box?) { box, type -> box?.let { b -> childrenOf(b).firstOrNull { it.type == type } } }

private fun ByteArray.copy(box: Box): ByteArray = copyOfRange(box.offset.toInt(), box.end.toInt())

/** Timescale of an `mvhd` or `mdhd` box. */
private fun ByteArray.timescaleOf(box: Box): Long {
    val body = box.body.toInt()
    return uint(body + if (this[body].toInt() == 1) 20 else 12)
}

private fun box(type: String, children: List<ByteArray>): ByteArray {
    val size = 8 + children.sumOf { it.size }
    return ByteBuffer.allocate(size).putInt(size).put(type.toByteArray(Charsets.ISO_8859_1))
        .apply { children.forEach { put(it) } }.array()
}

private fun ByteArray.int(at: Int) = ByteBuffer.wrap(this).getInt(at)
private fun ByteArray.uint(at: Int) = int(at).toLong() and 0xFFFFFFFFL
private fun ByteArray.long(at: Int) = ByteBuffer.wrap(this).getLong(at)
private fun ByteArray.putUint(at: Int, value: Long) {
    ByteBuffer.wrap(this).putInt(at, value.toInt())
}

private fun ByteArray.putLong(at: Int, value: Long) {
    ByteBuffer.wrap(this).putLong(at, value)
}

// endregion
