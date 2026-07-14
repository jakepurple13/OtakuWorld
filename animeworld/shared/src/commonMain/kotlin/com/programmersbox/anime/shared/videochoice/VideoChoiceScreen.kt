package com.programmersbox.anime.shared.videochoice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled._10mp
import androidx.compose.material.icons.filled._1k
import androidx.compose.material.icons.filled._4k
import androidx.compose.material.icons.filled._4mp
import androidx.compose.material.icons.filled._7mp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions

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
    isStreaming: Boolean,
    model: KmpChapterModel,
    genericInfo: KmpGenericInfo,
    navController: NavigationActions,
    isCastActive: () -> Boolean = { false },
    onCastLoad: (KmpStorage) -> Unit = {},
) {
    val onAction: (KmpStorage) -> Unit = {
        VideoSourceModel.showVideoSources = null
        if (isStreaming) {
            if (isCastActive()) {
                onCastLoad(it)
            } else {
                navController.navigate(
                    VideoScreen(
                        showPath = it.link.orEmpty(),
                        showName = model.name,
                        downloadOrStream = false,
                        referer = it.headers["referer"] ?: it.source.orEmpty()
                    )
                )
            }
        } else {
            genericInfo.downloadChapter(model, listOf(model), infoModel, navController)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { VideoSourceModel.showVideoSources = null },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ListBottomScreen(
            includeInsetPadding = false,
            title = "Choose quality for ${model.name}",
            list = items,
            onClick = { onAction(it) }
        ) {
            ListBottomSheetItemModel(
                primaryText = it.quality.orEmpty(),
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
