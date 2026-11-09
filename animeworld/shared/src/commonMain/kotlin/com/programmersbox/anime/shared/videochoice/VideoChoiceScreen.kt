package com.programmersbox.anime.shared.videochoice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled._10mp
import androidx.compose.material.icons.filled._1k
import androidx.compose.material.icons.filled._4k
import androidx.compose.material.icons.filled._4mp
import androidx.compose.material.icons.filled._7mp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.programmersbox.anime.shared.episodeload.EpisodeAction
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel

enum class Qualities(var value: Int) {
    Unknown(0),
    P360(-2), // 360p
    P480(-1), // 480p
    P720(1), // 720p
    P1080(2), // 1080p
    P1440(3), // 1440p
    P2160(4) // 4k or 2160p
}

fun getQualityFromName(qualityName: String): Qualities {
    return when (qualityName.replace("p", "").replace("P", "")) {
        "360" -> Qualities.P360
        "480" -> Qualities.P480
        "720" -> Qualities.P720
        "1080" -> Qualities.P1080
        "1440" -> Qualities.P1440
        "2160" -> Qualities.P2160
        "4k" -> Qualities.P2160
        "4K" -> Qualities.P2160
        else -> Qualities.Unknown
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoChoiceScreen(
    sources: List<KmpStorage>,
    episodeName: String,
    action: EpisodeAction,
    onPick: (KmpStorage) -> Unit,
    onDismiss: () -> Unit,
) {
    val groups = remember(sources) { sources.groupedByServer() }
    val ordered = remember(groups) { groups.flatMap { it.second } }
    val serverStarts = remember(groups) { groups.serverLabels() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ListBottomScreen(
            includeInsetPadding = false,
            title = "${if (action == EpisodeAction.Play) "Play" else "Download"} $episodeName",
            list = ordered,
            onClick = onPick,
            navigationIcon = {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            },
        ) {
            ListBottomSheetItemModel(
                primaryText = it.quality.orEmpty(),
                overlineText = serverStarts[it],
                icon = when (getQualityFromName(it.quality.orEmpty())) {
                    Qualities.Unknown -> Icons.Default.DeviceUnknown
                    Qualities.P360 -> Icons.AutoMirrored.Filled._360
                    Qualities.P480 -> Icons.Default._4mp
                    Qualities.P720 -> Icons.Default._7mp
                    Qualities.P1080 -> Icons.Default._10mp
                    Qualities.P1440 -> Icons.Default._1k
                    Qualities.P2160 -> Icons.Default._4k
                }
            )
        }
    }
}
