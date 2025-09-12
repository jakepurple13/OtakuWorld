package com.programmersbox.mangaworld

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.programmersbox.uiviews.OtakuApp
import org.koin.core.module.Module
import org.koin.dsl.module

class MangaApp : OtakuApp() {
    override val buildModules: Module = module { includes(appModule) }

    override fun onCreated() {
        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
    }

    override fun createFirebaseIds(): FirebaseIds = FirebaseIds(
        documentId = "favoriteManga",
        chaptersId = "chaptersRead",
        collectionId = "mangaworld",
        itemId = "mangaUrl",
        readOrWatchedId = "chapterCount",
    )
}

class GenericFileProvider : FileProvider()