package com.programmersbox.animeworld

import android.content.Context
import android.net.Uri
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.anime.WcoStream
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.SettingsDsl
import com.programmersbox.uiviews.utils.currentService

class MainActivity : BaseMainActivity() {

    companion object {
        const val VIEW_DOWNLOADS = "animeworld://view_downloads"
        const val VIEW_VIDEOS = "animeworld://view_videos"
        lateinit var activity: MainActivity
        val cast: CastHelper = CastHelper()
    }

    private var Context.wcoRecent by sharedPrefNotNullDelegate(true)

    override fun onCreate() {

        activity = this

        cast.init(this)

        WcoStream.RECENT_TYPE = wcoRecent

        when (intent.data) {
            Uri.parse(VIEW_DOWNLOADS) -> openDownloads()
            Uri.parse(VIEW_VIDEOS) -> openVideos()
        }

        if (currentService == null) {
            val s = Sources.values().random()
            sourcePublish.onNext(s)
            currentService = s.serviceName
        }

    }

    private fun openDownloads() {
        goToScreen(Screen.SETTINGS)
        currentNavController?.value
            ?.navigate(DownloadViewerFragment::class.java.hashCode(), null, SettingsDsl.customAnimationOptions)
    }

    private fun openVideos() {
        goToScreen(Screen.SETTINGS)
        currentNavController?.value
            ?.navigate(ViewVideosFragment::class.java.hashCode(), null, SettingsDsl.customAnimationOptions)
    }

}
