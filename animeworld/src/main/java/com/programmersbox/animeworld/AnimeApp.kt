package com.programmersbox.animeworld

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.icon
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.OtakuApp
import org.koin.core.module.Module

class AnimeApp : OtakuApp() {
    override fun Module.buildModules() {
        includes(appModule)
    }

    override fun onCreated() {
        FirebaseDb.DOCUMENT_ID = "favoriteShows"
        FirebaseDb.CHAPTERS_ID = "episodesWatched"
        FirebaseDb.COLLECTION_ID = "animeworld"
        FirebaseDb.ITEM_ID = "showUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "numEpisodes"
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun shortcuts(): List<ShortcutInfo> = listOf(
        //video viewer
        ShortcutInfo.Builder(this, ViewVideoViewModel.VideoViewerRoute)
            .setIcon(Icon.createWithBitmap(IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_video_library).toBitmap()))
            .setShortLabel(getString(R.string.view_videos))
            .setLongLabel(getString(R.string.view_videos))
            .setIntent(Intent(Intent.ACTION_MAIN, Uri.parse(MainActivity.VIEW_VIDEOS), this, MainActivity::class.java))
            .build()
    )

}

class GenericFileProvider : FileProvider()