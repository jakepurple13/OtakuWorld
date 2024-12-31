package com.programmersbox.animeworld.videochoice

import android.content.Context
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.programmersbox.animeworld.GenericAnime
import com.programmersbox.animeworld.MainActivity
import com.programmersbox.animeworld.Qualities
import com.programmersbox.animeworld.R
import com.programmersbox.animeworld.getQualityFromName
import com.programmersbox.animeworld.navigateToVideoPlayer
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.Storage
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.components.ListBottomScreen
import com.programmersbox.uiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoChoiceScreen(
    context: Context = LocalContext.current,
    items: List<Storage>,
    infoModel: InfoModel,
    isStreaming: Boolean,
    model: ChapterModel,
    genericInfo: GenericInfo = LocalGenericInfo.current,
) {
    val navController = LocalNavController.current
    val activity = LocalActivity.current

    val onAction: (Storage) -> Unit = {
        VideoSourceModel.showVideoSources = null
        if (isStreaming) {
            if (MainActivity.cast.isCastActive()) {
                MainActivity.cast.loadUrl(
                    it.link,
                    infoModel.title,
                    model.name,
                    infoModel.imageUrl,
                    it.headers
                )
            } else {
                context.navigateToVideoPlayer(
                    navController,
                    it.link,
                    model.name,
                    false,
                    it.headers["referer"] ?: it.source.orEmpty()
                )
            }
        } else {
            activity?.let { it1 -> (genericInfo as GenericAnime).downloadVideo(it1, model, it) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { VideoSourceModel.showVideoSources = null },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ListBottomScreen(
            includeInsetPadding = false,
            title = stringResource(R.string.choose_quality_for, model.name),
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