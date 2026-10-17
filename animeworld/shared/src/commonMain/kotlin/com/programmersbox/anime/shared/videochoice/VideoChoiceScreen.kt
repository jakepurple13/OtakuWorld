package com.programmersbox.anime.shared.videochoice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled._10mp
import androidx.compose.material.icons.filled._1k
import androidx.compose.material.icons.filled._4k
import androidx.compose.material.icons.filled._4mp
import androidx.compose.material.icons.filled._7mp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import kotlinx.coroutines.launch

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
    items: List<KmpStorage>,
    infoModel: KmpInfoModel,
    model: KmpChapterModel,
    genericInfo: KmpGenericInfo,
    navController: NavigationActions,
    onPlay: suspend (KmpStorage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val groups = remember(items) { items.groupedByServer() }
    val ordered = remember(groups) { groups.flatMap { it.second } }
    val serverStarts = remember(groups) { groups.serverLabels() }

    var playing by remember { mutableStateOf(false) }

    val onAction: (KmpStorage) -> Unit = {
        // Keep the sheet (and this scope) alive until resolve/play (or resolve/enqueue) finishes.
        if (!playing) {
            playing = true
            scope.launch {
                try {
                    onPlay(it)
                } finally {
                    VideoSourceModel.showVideoSources = null
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { VideoSourceModel.showVideoSources = null },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ListBottomScreen(
            includeInsetPadding = false,
            title = "Choose quality for ${model.name}",
            list = ordered,
            onClick = { onAction(it) },
            lazyListContent = {
                if (playing) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
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
