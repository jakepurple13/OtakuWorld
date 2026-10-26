package com.programmersbox.anime.shared.videoplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.cast.SubtitleFormat
import com.programmersbox.anime.shared.cast.VttCue
import com.programmersbox.anime.shared.cast.cueAt
import com.programmersbox.anime.shared.cast.parseVttCues
import com.programmersbox.anime.shared.cast.toWebVtt
import com.programmersbox.anime.shared.subtitles.CaptionCue
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.anime.shared.subtitles.toCaptionSpec
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.kmpmodels.cleanSubtitleLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Locale

sealed interface SubtitleTrack {
    val label: String

    /** A subtitle file from the source or a download sidecar; drawn by Compose from its cues. */
    data class External(val subtitle: KmpSubtitle, override val label: String) : SubtitleTrack

    /** A track inside the media; libVLC draws it into the frames. */
    data class Embedded(val spuId: Int, override val label: String) : SubtitleTrack
}

/**
 * External tracks first (labels via [cleanSubtitleLabel]), then embedded ones, with the index of the track to show
 * by default: null when [SubtitleStyle.autoEnable] is off.
 *
 * The default comes from [defaultSubtitleIndex] over all tracks, so an embedded track in a preferred language beats
 * an external one that matches no language; on a tie the external track wins.
 */
fun buildSubtitleTracks(
    external: List<KmpSubtitle>,
    embedded: List<SpuTrack>,
    style: SubtitleStyle,
    deviceLanguage: String,
): Pair<List<SubtitleTrack>, Int?> {
    val tracks = external.map { SubtitleTrack.External(it, cleanSubtitleLabel(it.lang)) } +
        embedded.map { SubtitleTrack.Embedded(it.id, it.label) }
    if (!style.autoEnable) return tracks to null

    // defaultSubtitleIndex matches on KmpSubtitle.lang, so give embedded tracks their language as a code and a name.
    val candidates = tracks.mapIndexed { i, track ->
        when (track) {
            is SubtitleTrack.External -> KmpSubtitle(track.subtitle.url, track.label)
            is SubtitleTrack.Embedded -> KmpSubtitle("", embeddedLanguageText(track.label, embedded[i - external.size].language))
        }
    }
    val default = defaultSubtitleIndex(
        subtitles = candidates,
        deviceLanguage = deviceLanguage,
        deviceLanguageName = Locale.forLanguageTag(deviceLanguage).getDisplayLanguage(Locale.ENGLISH),
        preferredLanguages = style.preferredLanguages,
        skipSignsAndSongs = style.skipSignsAndSongs,
    )
    return tracks to default
}

/** libVLC reports ISO 639-2 codes ("eng"); preferred languages are two-letter codes. */
private val twoLetterByIso3: Map<String, String> by lazy {
    Locale.getISOLanguages().associateBy { runCatching { Locale.forLanguageTag(it).isO3Language }.getOrDefault(it) }
}

private fun embeddedLanguageText(label: String, language: String?): String {
    val code = language?.trim()?.lowercase().orEmpty()
    val twoLetter = twoLetterByIso3[code] ?: code
    val name = if (twoLetter.isEmpty()) "" else Locale.forLanguageTag(twoLetter).getDisplayLanguage(Locale.ENGLISH)
    return listOf(label, twoLetter, name).filter { it.isNotBlank() }.joinToString(" ")
}

/** Fetches a subtitle file and converts it to cues; ASS styling is dropped. */
class SubtitleCueLoader(private val client: OkHttpClient) {
    suspend fun load(sub: KmpSubtitle, referer: String?): List<VttCue> = withContext(Dispatchers.IO) {
        val text = if (sub.url.startsWith("file:")) {
            File(URI(sub.url)).readText()
        } else {
            val request = Request.Builder().url(sub.url).apply {
                playerHeaders(sub.headers, referer).forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for ${sub.url}")
                response.body.string()
            }
        }
        parseVttCues(toWebVtt(text, SubtitleFormat.fromUrl(sub.url)))
    }
}

/** Subtitle tracks of what is playing, which one is shown, and the cues of a shown external track. */
@Stable
internal class DesktopSubtitles(
    val tracks: List<SubtitleTrack>,
    val selected: SubtitleTrack?,
    val cues: List<VttCue>,
    /** Picks a track; null turns subtitles off. */
    val select: (SubtitleTrack?) -> Unit,
)

/**
 * Follows the default track (which can change once embedded tracks appear) until the user picks one.
 * An external track hides libVLC's subtitles; an embedded one is handed to [DesktopPlayer.selectSpu].
 * [style] is null while it loads.
 */
@Composable
internal fun rememberDesktopSubtitles(
    player: DesktopPlayer,
    external: List<KmpSubtitle>,
    referer: String?,
    style: SubtitleStyle?,
    loader: SubtitleCueLoader,
): DesktopSubtitles {
    val embedded by player.spuTracks.collectAsState()
    val (tracks, default) = remember(external, embedded, style) {
        if (style == null) buildSubtitleTracks(external, embedded, SubtitleStyle(autoEnable = false), "")
        else buildSubtitleTracks(external, embedded, style, Locale.getDefault().language)
    }
    var chosen by remember(player) { mutableStateOf<Choice?>(null) }
    val selected = chosen.let { if (it != null) it.track else default?.let(tracks::getOrNull) }

    LaunchedEffect(player, selected, embedded) {
        player.selectSpu((selected as? SubtitleTrack.Embedded)?.spuId)
    }
    val cues by produceState(emptyList(), selected, referer) {
        value = emptyList()
        val track = selected as? SubtitleTrack.External ?: return@produceState
        value = try {
            loader.load(track.subtitle, referer)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[DesktopSubtitles] couldn't load ${track.subtitle.url}: $e")
            emptyList()
        }
    }
    return DesktopSubtitles(tracks, selected, cues) { chosen = Choice(it) }
}

/** The user's pick; a null [track] means subtitles off. */
private class Choice(val track: SubtitleTrack?)

/** The cue at [positionMs], styled from [style] and raised by its bottom padding. */
@Composable
internal fun SubtitleOverlay(cues: List<VttCue>, positionMs: Long, style: SubtitleStyle, modifier: Modifier = Modifier) {
    val text = cues.cueAt(positionMs) ?: return
    val spec = style.toCaptionSpec()
    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
        // Same base size as Media3's SubtitleView: 5.33% of the player height.
        val fontSize = with(LocalDensity.current) { (maxHeight * CAPTION_HEIGHT_FRACTION).toSp() }
        Box(
            Modifier
                .padding(bottom = 8.dp + maxHeight * spec.bottomPaddingFraction)
                .fillMaxWidth(0.9f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            CaptionCue(text, spec, baseFontSize = fontSize)
        }
    }
}

private const val CAPTION_HEIGHT_FRACTION = 0.0533f
