package com.programmersbox.animeworld

import android.content.Intent
import android.net.Uri
import com.programmersbox.anime_sources.Sources
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService

class MainActivity : BaseMainActivity() {

    companion object {
        const val VIEW_DOWNLOADS = "animeworld://view_downloads"
        const val VIEW_VIDEOS = "animeworld://view_videos"
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

    private fun openDownloads(intent: Intent?) {
        goToScreen(Screen.SETTINGS)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    private fun openVideos(intent: Intent?) {
        goToScreen(Screen.SETTINGS)
        if (isNavInitialized()) navController.handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when (intent?.data) {
            Uri.parse(VIEW_DOWNLOADS) -> openDownloads(intent)
            Uri.parse(VIEW_VIDEOS) -> openVideos(intent)
        }
    }

}
