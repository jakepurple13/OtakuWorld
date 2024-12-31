package com.programmersbox.novelworld

import com.programmersbox.uiviews.OtakuApp
import org.koin.core.module.Module

class NovelApp : OtakuApp() {
    override fun Module.buildModules() {
        includes(appModule)
    }

    override fun createFirebaseIds(): FirebaseIds = FirebaseIds(
        documentId = "favoriteNovels",
        chaptersId = "novelsChaptersRead",
        collectionId = "novelworld",
        itemId = "novelUrl",
        readOrWatchedId = "novelNumChapters",
    )
}