package com.programmersbox.animeworld

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.lifecycleScope
import com.programmersbox.anime_sources.Sources
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.animeworld.databinding.MiniControllerBinding
import com.programmersbox.animeworld.downloads.DownloaderViewModel
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.models.sourceFlow
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService
import kotlinx.coroutines.launch

class MainActivity : BaseMainActivity() {

    companion object {
        const val VIEW_DOWNLOADS = "animeworld://${DownloaderViewModel.DownloadViewerRoute}"
        const val VIEW_VIDEOS = "animeworld://${ViewVideoViewModel.VideoViewerRoute}"
        lateinit var activity: MainActivity
        val cast: CastHelper = CastHelper()
    }

    override fun onCreate() {

        activity = this

        try {
            cast.init(this)
        } catch (e: Exception) {

        }

        lifecycleScope.launch {
            if (currentService == null) {
                val s = Sources.values().filterNot(Sources::notWorking).random()
                sourceFlow.emit(s)
                currentService = s.serviceName
            }
        }

    }

    @Composable
    override fun BottomBarAdditions() {
        //TODO: get it working with the minicontroller fragment in play
        if (cast.isCastActive()) AndroidViewBinding(MiniControllerBinding::inflate)
    }

}
