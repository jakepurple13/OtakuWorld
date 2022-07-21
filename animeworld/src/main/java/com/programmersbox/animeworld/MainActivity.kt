package com.programmersbox.animeworld

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.programmersbox.anime_sources.Sources
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.animeworld.databinding.MiniControllerBinding
import com.programmersbox.animeworld.downloads.DownloaderViewModel
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService

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

        if (currentService == null) {
            val s = Sources.values().filterNot(Sources::notWorking).random()
            sourcePublish.onNext(s)
            currentService = s.serviceName
        }

    }

    @Composable
    override fun BottomBarAdditions() {
        //TODO: get it working with the minicontroller fragment in play
        AndroidViewBinding(MiniControllerBinding::inflate)
    }

    private fun openDownloads(intent: Intent?) {
        goToScreen(Screen.SETTINGS)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    private fun openVideos(intent: Intent?) {
        goToScreen(Screen.SETTINGS)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        when (intent?.data) {
            Uri.parse(VIEW_DOWNLOADS) -> openDownloads(intent)
            Uri.parse(VIEW_VIDEOS) -> openVideos(intent)
        }
    }

}
