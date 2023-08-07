package com.programmersbox.animeworld

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.animeworld.databinding.MiniControllerBinding
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.uiviews.BaseMainActivity

class MainActivity : BaseMainActivity() {

    companion object {
        const val VIEW_VIDEOS = "animeworld://${ViewVideoViewModel.VideoViewerRoute}"
        val cast: CastHelper = CastHelper()
    }

    override fun onCreate() {
        try {
            cast.init(this)
        } catch (e: Exception) {

        }
    }

    @Composable
    override fun BottomBarAdditions() {
        //TODO: get it working with the minicontroller fragment in play
        if (cast.isCastActive()) AndroidViewBinding(MiniControllerBinding::inflate)
    }

}
