package com.programmersbox.anime.shared.videoplayer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.sun.jna.Pointer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.TrackType
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

sealed interface PlayerSource {
    data class Stream(val url: String, val headers: Map<String, String>, val mimeType: String?) : PlayerSource
    data class LocalFile(val path: String) : PlayerSource
}

data class PlayerState(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val error: String? = null,
    /** 0..100; kept across [DesktopPlayer.open]. */
    val volume: Int = 100,
)

/** An embedded (in-container) subtitle track; [id] is the libVLC SPU id for [DesktopPlayer.selectSpu]. */
data class SpuTrack(val id: Int, val label: String, val language: String?)

/**
 * vlcj media player rendered into Skia bitmaps (see the desktop player spike) instead of a native view,
 * so Compose can draw controls and subtitles on top.
 *
 * Streams are played through [proxy] so every request carries the source's headers.
 * vlcj events arrive on native threads: they only update the [StateFlow]s here, and anything that calls back
 * into libVLC is handed to [MediaPlayer.submit].
 */
class DesktopPlayer(
    factory: MediaPlayerFactory,
    private val proxy: LocalStreamProxy,
    private val probeClient: OkHttpClient = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build(),
) : AutoCloseable {
    private val _frame = MutableStateFlow<ImageBitmap?>(null)

    /** Latest decoded frame, cropped to the display size (the decoder pads e.g. 1080 to 1088 lines). */
    val frame: StateFlow<ImageBitmap?> = _frame.asStateFlow()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _spuTracks = MutableStateFlow<List<SpuTrack>>(emptyList())

    /** Embedded subtitle tracks of the current media. */
    val spuTracks: StateFlow<List<SpuTrack>> = _spuTracks.asStateFlow()

    private val closed = AtomicBoolean(false)

    /** Serializes calls into the native player with its release. */
    private val apiLock = Any()

    /** Bumped by [open]; a failure probe for an older source must not report on the new one. */
    private val generation = AtomicInteger()

    @Volatile
    private var source: PlayerSource? = null

    @Volatile
    private var sawFrame = false

    /** Size of the picture without decoder padding, from libVLC once the video output exists. */
    @Volatile
    private var videoSize: Dimension? = null

    private val sink = FrameSink()

    private val player: EmbeddedMediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer().apply {
        videoSurface().set(factory.videoSurfaces().newVideoSurface(sink, sink, true))
        events().addMediaPlayerEventListener(Events())
    }

    fun open(source: PlayerSource, startMs: Long = 0) = api {
        val gen = generation.incrementAndGet()
        this.source = source
        sawFrame = false
        videoSize = null
        _spuTracks.value = emptyList()
        _state.value = PlayerState(positionMs = startMs, buffering = true, volume = _state.value.volume)
        val mrl = when (source) {
            is PlayerSource.LocalFile -> {
                val file = File(source.path)
                if (!file.isFile) return@api fail(gen, "File not found: ${source.path}")
                // A file URI (percent-encoded ASCII) keeps spaces and non-ASCII names intact on every platform.
                file.toPath().toUri().toASCIIString()
            }

            is PlayerSource.Stream -> try {
                proxy.start()
                proxy.register(source.url, source.headers, source.mimeType)
            } catch (e: Exception) {
                return@api fail(gen, "Can't play this link: ${e.message}")
            }
        }
        // Download sidecars (`<name>.<n>.<lang>.vtt`) are drawn by Compose, so libVLC must not autoload them. As
        // slaves they would also break MPEG-TS: their ES make libVLC select program 0, the TS program is never
        // selected, and the input ends without a frame.
        val options = listOfNotNull(":no-sub-autodetect-file", startMs.takeIf { it > 0 }?.let { ":start-time=${it / 1000.0}" })
        if (!player.media().play(mrl, *options.toTypedArray())) fail(gen, "The player couldn't open this video.")
    }

    fun playPause() = api {
        when {
            player.status().isPlaying -> player.controls().setPause(true)
            player.status().state() == State.ENDED -> {
                player.controls().stop()
                player.controls().play()
            }

            else -> player.controls().play()
        }
    }

    /**
     * Pauses while something covers the video (e.g. subtitle settings); calling the returned function resumes
     * playback only if it was playing, so a paused video stays paused.
     */
    fun pauseForOverlay(): () -> Unit {
        val wasPlaying = _state.value.playing
        api { player.controls().setPause(true) }
        return { if (wasPlaying) api { player.controls().setPause(false) } }
    }

    fun seekTo(ms: Long) = api {
        val duration = _state.value.durationMs
        val target = if (duration > 0) ms.coerceIn(0, duration) else ms.coerceAtLeast(0)
        player.controls().setTime(target)
        // libVLC may not report the new time until playback resumes; show the target meanwhile.
        _state.update { it.copy(positionMs = target) }
    }

    fun seekBy(deltaMs: Long) = seekTo(_state.value.positionMs + deltaMs)

    fun setVolume(percent: Int) = api {
        val volume = percent.coerceIn(0, 100)
        _state.update { it.copy(volume = volume) }
        player.audio().setVolume(volume)
    }

    /** Shows embedded subtitle track [id]; null hides embedded subtitles. */
    fun selectSpu(id: Int?) = api { player.subpictures().setTrack(id ?: -1) }

    /** Stops and releases the native player off the calling thread; safe to call twice and from any thread. */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        // vlcj: never release from a native callback thread, so release on a thread of our own.
        val releaser = thread(start = false, name = "DesktopPlayer-release") {
            synchronized(apiLock) {
                runCatching { player.controls().stop() }
                runCatching { player.release() }
            }
            _frame.value = null
            _state.update { it.copy(playing = false, buffering = false) }
            pendingReleases -= Thread.currentThread()
        }
        pendingReleases += releaser
        releaser.start()
    }

    /** Runs [block] against the native player unless it is closed or being released. */
    private inline fun api(crossinline block: () -> Unit) {
        if (closed.get()) return
        synchronized(apiLock) {
            if (!closed.get()) block()
        }
    }

    private fun fail(gen: Int, message: String) {
        if (gen == generation.get() && !closed.get()) {
            _state.update { it.copy(playing = false, buffering = false, error = message) }
        }
    }

    /** libVLC reports failures without a reason, so work out a message the user can act on. */
    private fun diagnoseFailure() {
        val gen = generation.get()
        val current = source ?: return
        thread(isDaemon = true, name = "DesktopPlayer-diagnose") {
            fail(gen, failureMessage(current))
        }
    }

    private fun failureMessage(source: PlayerSource): String = when (source) {
        is PlayerSource.LocalFile ->
            if (File(source.path).isFile) "This video format isn't supported, or the file is damaged."
            else "File not found: ${source.path}"

        is PlayerSource.Stream -> {
            val request = Request.Builder().url(source.url).apply {
                source.headers.forEach { (k, v) -> header(k, v) }
                header("Range", "bytes=0-0")
            }.build()
            try {
                probeClient.newCall(request).execute().use { response ->
                    when {
                        response.code >= 400 && isCloudflareChallenge(response) ->
                            "This source is protected by Cloudflare and needs a browser check (WebView), which the desktop app can't do."

                        response.code >= 400 -> "The video server refused the request (HTTP ${response.code})."
                        else -> "This video format isn't supported."
                    }
                }
            } catch (e: IOException) {
                "Couldn't reach the video server: ${e.message}"
            } catch (e: IllegalArgumentException) {
                "Can't play this link: ${e.message}"
            }
        }
    }

    private fun isCloudflareChallenge(response: okhttp3.Response) =
        response.header("cf-mitigated") != null ||
            (response.code in setOf(403, 429, 503) && response.header("Server").orEmpty().contains("cloudflare", true))

    private fun refreshVideoSize(mp: MediaPlayer) {
        videoSize = mp.video().videoDimension()
    }

    private fun refreshSpuTracks(mp: MediaPlayer) {
        val info = mp.media().info()?.textTracks().orEmpty().associateBy { it.id() }
        _spuTracks.value = mp.subpictures().trackDescriptions()
            .filter { it.id() >= 0 }
            .map { d ->
                val track = info[d.id()]
                val label = if (track != null) embeddedTrackLabel(track.description(), track.language()) else d.description()
                SpuTrack(d.id(), label, track?.language())
            }
    }

    private inner class Events : MediaPlayerEventAdapter() {
        private fun MediaPlayer.later(block: (MediaPlayer) -> Unit) {
            if (closed.get()) return
            runCatching { submit { if (!closed.get()) block(this) } }
        }

        override fun opening(mediaPlayer: MediaPlayer) {
            if (!closed.get()) _state.update { it.copy(buffering = true) }
        }

        override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
            if (closed.get()) return
            _state.update { it.copy(buffering = newCache < 100f, bufferedMs = buffered(it.positionMs, it.durationMs, newCache)) }
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            if (closed.get()) return
            _state.update { it.copy(playing = true, buffering = false) }
            mediaPlayer.later {
                // The audio output only exists once playing, so a volume set earlier is applied now.
                it.audio().setVolume(_state.value.volume)
                refreshSpuTracks(it)
            }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            if (!closed.get()) _state.update { it.copy(playing = false) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            if (!closed.get()) _state.update { it.copy(playing = false, buffering = false) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            if (closed.get()) return
            if (!sawFrame && _state.value.durationMs <= 0) return diagnoseFailure()
            _state.update { it.copy(playing = false, buffering = false, positionMs = it.durationMs) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            if (closed.get()) return
            _state.update { it.copy(positionMs = newTime, bufferedMs = maxOf(it.bufferedMs, newTime)) }
        }

        override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
            if (closed.get()) return
            _state.update {
                it.copy(durationMs = newLength, bufferedMs = if (source is PlayerSource.LocalFile) newLength else it.bufferedMs)
            }
        }

        override fun elementaryStreamAdded(mediaPlayer: MediaPlayer, type: TrackType, id: Int) {
            if (type == TrackType.TEXT) mediaPlayer.later(::refreshSpuTracks)
        }

        override fun videoOutput(mediaPlayer: MediaPlayer, newCount: Int) {
            mediaPlayer.later(::refreshVideoSize)
        }

        override fun elementaryStreamDeleted(mediaPlayer: MediaPlayer, type: TrackType, id: Int) {
            if (type == TrackType.TEXT) mediaPlayer.later(::refreshSpuTracks)
        }

        override fun error(mediaPlayer: MediaPlayer) {
            if (!closed.get()) diagnoseFailure()
        }

        /**
         * libVLC has no buffered-range API: a local file is fully available, and for a stream the fill level of
         * its network cache is the best estimate of what is buffered past the playhead.
         */
        private fun buffered(positionMs: Long, durationMs: Long, cacheFill: Float): Long {
            if (source is PlayerSource.LocalFile) return durationMs
            val ahead = positionMs + (NETWORK_CACHING_MS * cacheFill / 100f).toLong()
            return if (durationMs > 0) ahead.coerceAtMost(durationMs) else ahead
        }
    }

    /** Receives decoded frames from libVLC and publishes them, cropped to [videoSize]. */
    private inner class FrameSink : BufferFormatCallback, RenderCallback {
        private val frames = FrameBuffers(FRAME_BUFFERS)

        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            // The source size here includes decoder padding (1920x1088 for 1080p). The real size can't be asked
            // for from this native thread, so look it up on vlcj's executor.
            videoSize = null
            if (!closed.get()) runCatching { player.submit { if (!closed.get()) refreshVideoSize(player) } }
            return RV32BufferFormat(sourceWidth, sourceHeight)
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
            if (closed.get()) return
            // Crop: keep the top-left video-sized region of the padded buffer.
            val size = videoSize
            val width = size?.width?.takeIf { it in 1..bufferFormat.width } ?: bufferFormat.width
            val height = size?.height?.takeIf { it in 1..bufferFormat.height } ?: bufferFormat.height
            val image = frames.copy(nativeBuffers[0], rowBytes = bufferFormat.width * 4, width = width, height = height)
            sawFrame = true
            _frame.value = image
        }
    }

    private companion object {
        const val FRAME_BUFFERS = 3

        /** libVLC's default `--network-caching`. */
        const val NETWORK_CACHING_MS = 1_000L

        /** Releases still running; the JVM waits for them on exit so libVLC is not torn down mid-release. */
        val pendingReleases: MutableSet<Thread> = ConcurrentHashMap.newKeySet<Thread>().also { pending ->
            Runtime.getRuntime().addShutdownHook(
                thread(start = false, name = "DesktopPlayer-shutdown") {
                    pending.toList().forEach { it.join(3_000) }
                }
            )
        }
    }
}

/**
 * Copies BGRA frames into one of [count] reused Skia bitmaps and returns that bitmap as an [ImageBitmap].
 * Rotating bitmaps keeps the one Compose is drawing from being overwritten by the next frame.
 */
internal class FrameBuffers(private val count: Int) {
    private class Slot(val bitmap: Bitmap, val address: Long, val image: ImageBitmap)

    private data class Layout(val width: Int, val height: Int, val rowBytes: Int)

    private val lock = Any()
    private var slots: List<Slot> = emptyList()
    private var layout: Layout? = null
    private var next = 0

    /** Copies the top-left [width]x[height] pixels of [src], whose rows are [rowBytes] apart. */
    fun copy(src: ByteBuffer, rowBytes: Int, width: Int, height: Int): ImageBitmap = synchronized(lock) {
        // The bitmaps share the source's row stride, so a new stride needs new bitmaps even at the same size.
        val wanted = Layout(width, height, rowBytes)
        if (wanted != layout) allocate(wanted)
        val slot = slots[next]
        next = (next + 1) % slots.size

        val byteCount = rowBytes * (height - 1) + width * 4
        src.rewind()
        src.limit(byteCount)
        Pointer(slot.address).getByteBuffer(0, byteCount.toLong()).put(src)
        src.clear()
        slot.bitmap.notifyPixelsChanged()
        slot.image
    }

    private fun allocate(layout: Layout) {
        val info = ImageInfo(layout.width, layout.height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        // Old bitmaps are not closed: Compose may still be drawing one. Skia frees them once unreachable.
        slots = List(count) {
            val bitmap = Bitmap()
            check(bitmap.allocPixels(info, layout.rowBytes)) { "Couldn't allocate a ${layout.width}x${layout.height} frame" }
            Slot(bitmap, bitmap.peekPixels()!!.addr, bitmap.asComposeImageBitmap())
        }
        this.layout = layout
        next = 0
    }
}
