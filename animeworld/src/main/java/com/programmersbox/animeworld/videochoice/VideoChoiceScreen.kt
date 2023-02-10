package com.programmersbox.animeworld.videochoice

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.anime_sources.utilities.Qualities
import com.programmersbox.anime_sources.utilities.getQualityFromName
import com.programmersbox.animeworld.GenericAnime
import com.programmersbox.animeworld.MainActivity
import com.programmersbox.animeworld.R
import com.programmersbox.animeworld.navigateToVideoPlayer
import com.programmersbox.models.Storage
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel

@Composable
fun VideoChoiceScreen(
    context: Context = LocalContext.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    vm: VideoSourceViewModel = viewModel { VideoSourceViewModel(createSavedStateHandle(), genericInfo) }
) {
    val navController = LocalNavController.current
    val activity = LocalActivity.current

    val onAction: (Storage) -> Unit = {
        navController.popBackStack()
        if (vm.isStream) {
            if (MainActivity.cast.isCastActive()) {
                MainActivity.cast.loadUrl(
                    it.link,
                    vm.infoModel!!.title,
                    vm.name,
                    vm.infoModel.imageUrl,
                    it.headers
                )
            } else {
                context.navigateToVideoPlayer(
                    navController,
                    it.link,
                    vm.name,
                    false,
                    it.headers["referer"] ?: it.source.orEmpty()
                )
            }
        } else {
            (genericInfo as GenericAnime).fetchIt(it, vm.model!!, activity)
        }
    }

    ListBottomScreen(
        includeInsetPadding = false,
        title = stringResource(R.string.choose_quality_for, vm.name),
        list = vm.items,
        onClick = { onAction(it) }
    ) {
        ListBottomSheetItemModel(
            primaryText = it.quality.orEmpty(),
            icon = when (getQualityFromName(it.quality.orEmpty())) {
                Qualities.Unknown -> Icons.Default.DeviceUnknown
                Qualities.P360 -> Icons.Default._360
                Qualities.P480 -> Icons.Default._4mp
                Qualities.P720 -> Icons.Default._7mp
                Qualities.P1080 -> Icons.Default._10mp
                Qualities.P1440 -> Icons.Default._1k
                Qualities.P2160 -> Icons.Default._4k
                else -> Icons.Default.DeviceUnknown
            }
        )
    }
}