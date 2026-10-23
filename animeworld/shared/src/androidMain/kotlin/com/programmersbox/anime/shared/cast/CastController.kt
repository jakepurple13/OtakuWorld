package com.programmersbox.anime.shared.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.TextTrackStyle
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

/**
 * [headers] are everything the proxy sends upstream (player defaults included); [sourceHeaders] are only the
 * source's own, and decide whether a non-HLS stream needs the proxy at all ([castSourceHeaders]).
 */
data class CastRequest(
    val showTitle: String,
    val episodeName: String,
    val posterUrl: String?,
    val url: String,
    val mimeType: String?,
    val headers: Map<String, String>,
    val sourceHeaders: Map<String, String>,
    val subtitles: List<KmpSubtitle>,
    val selectedSubtitle: Int?,
    val startPositionMs: Long,
    val localFile: File? = null,
    val subtitleOrigin: String = "source",
)

sealed interface CastResult {
    data object Started : CastResult
    data object Unsupported : CastResult
    data object NoWifi : CastResult
    data class Failed(val message: String) : CastResult
}

/** Toast text for a result that did not start casting; null for [CastResult.Started]. */
fun CastResult.errorMessage(): String? = when (this) {
    CastResult.Started -> null
    CastResult.Unsupported -> "This episode can't be cast"
    CastResult.NoWifi -> "Connect to the same Wi-Fi as your TV"
    is CastResult.Failed -> "Couldn't cast: $message"
}

/** Drives a Google Cast session through [RemoteMediaClient]. All Cast SDK calls happen on the main thread. */
class CastController(
    context: Context,
    private val server: CastMediaServer,
    private val subtitleSettings: SubtitleSettingsRepository,
    private val scope: CoroutineScope,
) {
    private class LoadedTrack(val track: CastTrack, val cues: List<VttCue>)

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(CastUiState())

    /** Reading this lazily initializes the [CastContext] (on the main thread). */
    val state: StateFlow<CastUiState>
        get() {
            if (!initialized) scope.launch { castContext() }
            return _state
        }

    private var initialized = false
    private var castContext: CastContext? = null
    private var session: CastSession? = null
    private var loadedTracks: List<LoadedTrack> = emptyList()
    private var styleJob: Job? = null
    private var errorReported = false
    private var loadInFlight = false

    private val _playbackErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** One message each time the receiver stops on a playback error after the load succeeded. */
    val playbackErrors: SharedFlow<String> = _playbackErrors

    private val mediaCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() = publishStatus()
    }
    private val progressListener = RemoteMediaClient.ProgressListener { _, _ -> publishStatus() }

    private val castStateListener = CastStateListener { castState ->
        when (castState) {
            CastState.NO_DEVICES_AVAILABLE -> _state.update { it.copy(connection = CastConnection.NoDevices) }
            CastState.NOT_CONNECTED -> _state.update { reduce(it, CastEvent.Disconnected) }
            CastState.CONNECTING -> _state.update { it.copy(connection = CastConnection.Connecting) }
            CastState.CONNECTED -> Unit // handled by the session listener, which knows the device
        }
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = attach(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = attach(session)
        override fun onSessionEnded(session: CastSession, error: Int) = detach()
        override fun onSessionStartFailed(session: CastSession, error: Int) = detach()
        override fun onSessionResumeFailed(session: CastSession, error: Int) = detach()
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
    }

    /** Returns the shared [CastContext], or null when Google Play services cannot provide one. Main thread only. */
    private suspend fun castContext(): CastContext? = withContext(Dispatchers.Main.immediate) {
        if (!initialized) {
            initialized = true
            castContext = try {
                CastContext.getSharedInstance(appContext)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Cast unavailable (Google Play services missing or outdated)", e)
                null
            }
            castContext?.let { ctx ->
                ctx.addCastStateListener(castStateListener)
                castStateListener.onCastStateChanged(ctx.castState)
                ctx.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
                ctx.sessionManager.currentCastSession?.takeIf { it.isConnected }?.let(::attach)
            }
        }
        castContext
    }

    private fun attach(newSession: CastSession) {
        if (session !== newSession) {
            session?.remoteMediaClient?.let(::unregister)
            loadedTracks = emptyList()
        }
        session = newSession
        _state.update { reduce(it, CastEvent.Connected(newSession.castDevice?.friendlyName.orEmpty())) }
        newSession.remoteMediaClient?.let { client ->
            unregister(client)
            client.registerCallback(mediaCallback)
            client.addProgressListener(progressListener, PROGRESS_INTERVAL_MS)
        }
        styleJob?.cancel()
        styleJob = scope.launch {
            subtitleSettings.style.collect { style ->
                session?.remoteMediaClient?.takeIf { it.mediaInfo != null }
                    ?.setTextTrackStyle(style.toCastTextStyle().toTextTrackStyle())
            }
        }
        publishStatus()
    }

    private fun detach() {
        session?.remoteMediaClient?.let(::unregister)
        session = null
        loadedTracks = emptyList()
        styleJob?.cancel()
        styleJob = null
        server.stop()
        CastSessionService.stop(appContext)
        _state.update { reduce(it, CastEvent.Disconnected) }
    }

    private fun unregister(client: RemoteMediaClient) {
        client.unregisterCallback(mediaCallback)
        client.removeProgressListener(progressListener)
    }

    suspend fun cast(request: CastRequest): CastResult {
        castContext() ?: return CastResult.Failed("Google Play services unavailable")
        if (castCompatibility(request.url, request.mimeType) == CastCompat.Blocked) return CastResult.Unsupported
        val current = session?.takeIf { it.isConnected } ?: return CastResult.Failed("No cast device connected")
        server.start().takeIf { it != CastResult.Started }?.let { return it }

        val mediaUrl = when {
            request.localFile != null -> server.registerFile(request.localFile)
            needsProxy(request.url, request.mimeType, request.sourceHeaders) -> server.registerStream(request.url, request.headers)
            else -> request.url
        }
        val registered = coroutineScope {
            request.subtitles.map { sub -> async { sub to server.registerSubtitle(sub) } }.awaitAll()
        }.withIndex().mapNotNull { (index, pair) -> pair.second?.let { Triple(index, pair.first, it) } }

        val tracks = registered.mapIndexed { i, (_, sub, reg) ->
            LoadedTrack(CastTrack(OWN_TRACK_BASE + i, sub.lang, null, request.subtitleOrigin), reg.cues)
        }
        val mediaTracks = registered.mapIndexed { i, (_, sub, reg) ->
            MediaTrack.Builder(OWN_TRACK_BASE + i, MediaTrack.TYPE_TEXT)
                .setName(sub.lang)
                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                .setContentId(reg.url)
                .setContentType("text/vtt")
                .build()
        }
        val selectedId = registered.indexOfFirst { it.first == request.selectedSubtitle }
            .takeIf { it >= 0 }?.let { OWN_TRACK_BASE + it }

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW).apply {
            if (request.showTitle.isNotBlank()) putString(MediaMetadata.KEY_SERIES_TITLE, request.showTitle)
            putString(MediaMetadata.KEY_TITLE, request.episodeName)
            request.posterUrl?.let { addImage(WebImage(Uri.parse(it))) }
        }
        val mediaInfo = MediaInfo.Builder(mediaUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(castContentType(request.url, request.mimeType))
            .setMetadata(metadata)
            .setMediaTracks(mediaTracks)
            .build()
        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(request.startPositionMs)
            .apply { selectedId?.let { setActiveTrackIds(longArrayOf(it)) } }
            .build()

        val style = subtitleSettings.style.first().toCastTextStyle().toTextTrackStyle()
        return withContext(Dispatchers.Main.immediate) {
            val client = current.remoteMediaClient ?: return@withContext CastResult.Failed("Receiver not ready")
            loadedTracks = tracks
            loadInFlight = true
            val error = try {
                suspendCancellableCoroutine { cont ->
                    client.load(loadRequest).setResultCallback { result ->
                        val status = result.status
                        cont.resume(if (status.isSuccess) null else status.statusMessage ?: "Load failed (${status.statusCode})")
                    }
                }
            } finally {
                loadInFlight = false
            }
            if (error != null) {
                loadedTracks = emptyList()
                // The caller reports a failed load; don't also report the receiver's idle-error status.
                errorReported = true
                return@withContext CastResult.Failed(error)
            }
            client.setTextTrackStyle(style)
            CastSessionService.start(appContext, current.castDevice?.friendlyName.orEmpty())
            publishStatus()
            CastResult.Started
        }
    }

    private fun client(): RemoteMediaClient? = session?.remoteMediaClient

    fun togglePlay() {
        client()?.togglePlayback()
    }

    fun seekBy(deltaMs: Long) {
        val client = client() ?: return
        seekTo(client.approximateStreamPosition + deltaMs)
    }

    fun seekTo(ms: Long) {
        val client = client() ?: return
        val max = client.streamDuration.takeIf { it > 0 } ?: Long.MAX_VALUE
        client.seek(MediaSeekOptions.Builder().setPosition(ms.coerceIn(0, max)).build())
    }

    /** Selects subtitle track [id]; null turns subtitles off. */
    fun selectTrack(id: Long?) {
        client()?.setActiveMediaTracks(id?.let { longArrayOf(it) } ?: LongArray(0))
    }

    fun setVolume(v: Float) {
        val current = session ?: return
        try {
            current.volume = v.coerceIn(0f, 1f).toDouble()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to set cast volume", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to set cast volume", e)
        }
    }

    /** Stops playback on the receiver and ends the session; teardown happens in the session listener. */
    fun stop() {
        castContext?.sessionManager?.endCurrentSession(true)
    }

    private fun publishStatus() {
        val current = session ?: return
        val client = current.remoteMediaClient ?: return
        val status: MediaStatus = client.mediaStatus ?: return
        val failed = status.playerState == MediaStatus.PLAYER_STATE_IDLE && status.idleReason == MediaStatus.IDLE_REASON_ERROR
        // Only errors after a successful load; a failing load is reported by cast()'s result.
        if (failed && !errorReported && !loadInFlight) _playbackErrors.tryEmit(PLAYBACK_ERROR)
        errorReported = failed
        val info = client.mediaInfo
        val position = client.approximateStreamPosition
        val baseUrl = server.baseUrl
        val (ownMedia, streamMedia) = info?.mediaTracks.orEmpty()
            .filter { it.type == MediaTrack.TYPE_TEXT }
            .partition { isOwnTrack(it.id, it.contentId, baseUrl) }
        val loadedById = loadedTracks.associateBy { it.track.id }
        val ownTracks = ownMedia.map { loadedById[it.id]?.track ?: CastTrack(it.id, it.name ?: "Track ${it.id}", it.language, "source") }
        val streamTracks = streamMedia.map { CastTrack(it.id, it.name ?: it.language ?: "Track ${it.id}", it.language, "stream") }
        val activeId = status.activeTrackIds?.firstOrNull { id ->
            ownTracks.any { it.id == id } || streamTracks.any { it.id == id }
        }
        val metadata = info?.metadata
        _state.update {
            reduce(
                it,
                CastEvent.Status(
                    showTitle = metadata?.getString(MediaMetadata.KEY_SERIES_TITLE),
                    episodeName = metadata?.getString(MediaMetadata.KEY_TITLE),
                    posterUrl = metadata?.images?.firstOrNull()?.url?.toString(),
                    positionMs = position,
                    durationMs = client.streamDuration.coerceAtLeast(0),
                    playing = status.playerState == MediaStatus.PLAYER_STATE_PLAYING,
                    tracks = ownTracks + streamTracks,
                    activeTrackId = activeId,
                    volume = current.volume.toFloat(),
                    currentCue = activeId?.let(loadedById::get)?.cues?.cueAt(position),
                ),
            )
        }
    }

    private companion object {
        const val TAG = "CastController"
        const val PROGRESS_INTERVAL_MS = 500L
        const val PLAYBACK_ERROR = "Couldn't play on TV"
    }
}

private fun CastTextStyle.toTextTrackStyle(): TextTrackStyle = TextTrackStyle().also {
    it.fontScale = fontScale
    it.foregroundColor = foregroundArgb.toInt()
    it.backgroundColor = backgroundArgb.toInt()
    it.edgeType = when (edge) {
        CastEdge.None -> TextTrackStyle.EDGE_TYPE_NONE
        CastEdge.Outline -> TextTrackStyle.EDGE_TYPE_OUTLINE
        CastEdge.DropShadow -> TextTrackStyle.EDGE_TYPE_DROP_SHADOW
        CastEdge.Raised -> TextTrackStyle.EDGE_TYPE_RAISED
    }
    it.edgeColor = edgeArgb.toInt()
    it.fontGenericFamily = when (font) {
        CastFont.SansSerif -> TextTrackStyle.FONT_FAMILY_SANS_SERIF
        CastFont.Serif -> TextTrackStyle.FONT_FAMILY_SERIF
        CastFont.Monospace -> TextTrackStyle.FONT_FAMILY_MONOSPACED_SANS_SERIF
    }
}
