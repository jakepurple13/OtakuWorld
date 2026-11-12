package com.programmersbox.anime.shared.videos

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.videoplayer.VlcRuntime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.media.MediaParsedStatus
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import javax.imageio.ImageIO

/** Thumbnails are scaled to this width; height keeps the video's aspect ratio. */
private const val THUMBNAIL_WIDTH = 320

/** Skip the first decoded frames; right after a seek they're often grey or half-drawn. */
private const val FRAMES_TO_SKIP = 2

/** Grab the frame this far into the video, past most intros' black frames. */
private const val THUMBNAIL_POSITION = 0.1

private const val PARSE_TIMEOUT_MS = 5_000
private const val FRAME_TIMEOUT_SECONDS = 8L

/** Duration stored for a video libVLC couldn't open, so it isn't retried on every scan. */
private const val FAILED = -1L

/** Cache key for [file]: changes when the file is replaced or edited. */
internal fun thumbnailKey(file: File): String = MessageDigest.getInstance("SHA-1")
    .digest("${file.absolutePath}|${file.length()}|${file.lastModified()}".toByteArray())
    .joinToString("") { "%02x".format(it) }

/**
 * Reads the length of local videos and grabs a thumbnail frame with the bundled libVLC, one video at a
 * time, and caches both on disk. Desktop has no `MediaMetadataRetriever`, so this fills the gap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VideoThumbnailer(appDirs: AppDirs) {

    private val cacheDir = File(appDirs.getUserCacheDir(), "video-thumbnails")

    private val durationPrefs = Preferences.userNodeForPackage(VideoThumbnailer::class.java).node("durations")

    /** Keys libVLC gave no frame for (audio-only, too slow to decode), so they aren't grabbed again on every scan. */
    private val noThumbnailPrefs = Preferences.userNodeForPackage(VideoThumbnailer::class.java).node("no-thumbnail")

    /** One libVLC grab at a time; each one spins up a decoder. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    private val requests = ConcurrentHashMap<String, Deferred<File?>>()

    private val _durations = MutableStateFlow<Map<String, Long>>(emptyMap())

    private val _pending = MutableStateFlow(0)

    /** How many videos are waiting for libVLC to read them. */
    val pending: StateFlow<Int> = _pending.asStateFlow()

    /** Video lengths in ms by [thumbnailKey], as they are read. */
    val durations: StateFlow<Map<String, Long>> = _durations.asStateFlow()

    private fun thumbnailFile(key: String) = File(cacheDir, "$key.jpg")

    /** The cached length of [file] in ms, or null if it hasn't been read yet. */
    fun cachedDuration(key: String): Long? = _durations.value[key]
        ?: durationPrefs.getLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.also { cached ->
            _durations.update { it + (key to cached) }
        }

    /** The thumbnail for [file], grabbing it (and its length) first if it isn't cached. Null if libVLC can't. */
    suspend fun thumbnail(file: File): File? = request(file).await()

    /** Queues [file] so its length and thumbnail are ready before the row asks. */
    fun prefetch(file: File) {
        request(file)
    }

    private fun request(file: File): Deferred<File?> {
        val key = thumbnailKey(file)
        val cached = thumbnailFile(key)
        if (cached.isFile && cachedDuration(key) != null) return CompletableDeferred(cached)
        if (cachedDuration(key) == FAILED || noThumbnailPrefs.getBoolean(key, false)) return CompletableDeferred(null)
        return requests.computeIfAbsent(key) {
            val result = CompletableDeferred<File?>()
            _pending.update { it + 1 }
            scope.launch {
                try {
                    result.complete(runCatching { grab(file, key) }.onFailure { it.printStackTrace() }.getOrNull())
                } finally {
                    requests.remove(key)
                    _pending.update { it - 1 }
                }
            }
            result
        }
    }

    private fun grab(file: File, key: String): File? {
        val factory = VlcRuntime.factory().getOrNull() ?: return null
        val mrl = file.toPath().toUri().toASCIIString()

        var duration = factory.media().newMedia(mrl)?.let { media ->
            try {
                val parsed = CountDownLatch(1)
                media.events().addMediaEventListener(object : MediaEventAdapter() {
                    override fun mediaParsedChanged(media: Media, newStatus: MediaParsedStatus) = parsed.countDown()
                })
                if (media.parsing().parse(PARSE_TIMEOUT_MS)) parsed.await(PARSE_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                media.info().duration()
            } finally {
                media.release()
            }
        } ?: 0L

        val sink = FrameGrabber()
        val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
        try {
            player.videoSurface().set(factory.videoSurfaces().newVideoSurface(sink, sink, true))
            val startSeconds = if (duration > 0) duration * THUMBNAIL_POSITION / 1000.0 else 0.0
            player.media().play(mrl, ":no-audio", ":no-sub-autodetect-file", ":start-time=$startSeconds")
            sink.done.await(FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (duration <= 0) duration = player.status().length()
        } finally {
            player.controls().stop()
            player.release()
        }

        val image = sink.image
        durationPrefs.putLong(key, if (image == null && duration <= 0) FAILED else duration.coerceAtLeast(0))
        _durations.update { it + (key to duration.coerceAtLeast(0)) }
        if (image == null) {
            noThumbnailPrefs.putBoolean(key, true)
            return null
        }
        cacheDir.mkdirs()
        val out = thumbnailFile(key)
        ImageIO.write(image, "jpg", out)
        return out
    }

    /** Keeps the frame after [FRAMES_TO_SKIP], or the last one if the video ends first. */
    private class FrameGrabber : BufferFormatCallback, RenderCallback {
        val done = CountDownLatch(1)

        @Volatile
        var image: BufferedImage? = null
            private set

        private var frames = 0

        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            val height = (THUMBNAIL_WIDTH.toLong() * sourceHeight / sourceWidth.coerceAtLeast(1)).toInt().coerceAtLeast(1)
            return RV32BufferFormat(THUMBNAIL_WIDTH, height)
        }

        override fun newFormatSize(bufferWidth: Int, bufferHeight: Int, displayWidth: Int, displayHeight: Int) = Unit

        override fun allocatedBuffers(buffers: Array<out ByteBuffer>) = Unit

        override fun lock(mediaPlayer: MediaPlayer) = Unit

        override fun unlock(mediaPlayer: MediaPlayer) = Unit

        override fun display(
            mediaPlayer: MediaPlayer,
            nativeBuffers: Array<out ByteBuffer>,
            bufferFormat: BufferFormat,
            displayWidth: Int,
            displayHeight: Int,
        ) {
            if (done.count == 0L) return
            val width = bufferFormat.width
            val height = bufferFormat.height
            // RV32 is BGRA in memory; read little-endian it's 0xAARRGGBB, which TYPE_INT_RGB takes as-is.
            val pixels = IntArray(width * height)
            val source = nativeBuffers[0].duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
            source.get(pixels, 0, minOf(pixels.size, source.remaining()))
            image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply { setRGB(0, 0, width, height, pixels, 0, width) }
            if (++frames > FRAMES_TO_SKIP) done.countDown()
        }
    }
}
