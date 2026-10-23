package com.programmersbox.anime.shared.cast

enum class CastConnection { Unavailable, NoDevices, Disconnected, Connecting, Connected }

/** A subtitle track on the receiver. [origin] is `"source"`, `"downloaded"`, or `"stream"` (in-manifest HLS track). */
data class CastTrack(val id: Long, val label: String, val language: String?, val origin: String)

data class CastUiState(
    val connection: CastConnection = CastConnection.Unavailable,
    val deviceName: String? = null,
    val showTitle: String? = null,
    val episodeName: String? = null,
    val posterUrl: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playing: Boolean = false,
    val tracks: List<CastTrack> = emptyList(),
    val activeTrackId: Long? = null,
    val volume: Float = 0f,
    val currentCue: String? = null,
)

sealed interface CastEvent {
    data class Connected(val deviceName: String) : CastEvent
    data object Disconnected : CastEvent

    /** Snapshot of the receiver's media status plus the cue the phone resolved for [positionMs]. */
    data class Status(
        val showTitle: String?,
        val episodeName: String?,
        val posterUrl: String?,
        val positionMs: Long,
        val durationMs: Long,
        val playing: Boolean,
        val tracks: List<CastTrack>,
        val activeTrackId: Long?,
        val volume: Float,
        val currentCue: String?,
    ) : CastEvent
}

fun reduce(state: CastUiState, event: CastEvent): CastUiState = when (event) {
    is CastEvent.Connected -> state.copy(connection = CastConnection.Connected, deviceName = event.deviceName)
    CastEvent.Disconnected -> CastUiState(connection = CastConnection.Disconnected)
    is CastEvent.Status -> state.copy(
        showTitle = event.showTitle,
        episodeName = event.episodeName,
        posterUrl = event.posterUrl,
        positionMs = event.positionMs,
        durationMs = event.durationMs,
        playing = event.playing,
        tracks = event.tracks,
        activeTrackId = event.activeTrackId,
        volume = event.volume,
        currentCue = event.currentCue,
    )
}
