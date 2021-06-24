package com.programmersbox.mangaworld

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.programmersbox.manga_sources.Sources
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker
import com.programmersbox.uiviews.utils.FirebaseDb
import org.koin.core.context.loadKoinModules

class MangaApp : OtakuApp() {
    override fun onCreated() {

        loadKoinModules(appModule)

        UpdateWorker.sourcesList = Sources.values().toList()
        UpdateWorker.sourceFromString = {
            try {
                Sources.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)

        FirebaseDb.DOCUMENT_ID = "favoriteManga"
        FirebaseDb.CHAPTERS_ID = "chaptersRead"
        FirebaseDb.COLLECTION_ID = "mangaworld"
        FirebaseDb.ITEM_ID = "mangaUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "chapterCount"

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(
                    listOf(
                        "06CDF88040C0DBAE35E0A89453F636C6",
                        "0F7FE45111A61A20D1EA4B517FA51744"
                    )
                )
                .build()
        )

    }
}

class GenericFileProvider : FileProvider()