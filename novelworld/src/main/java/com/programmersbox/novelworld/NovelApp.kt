package com.programmersbox.novelworld

import com.programmersbox.uiviews.OtakuApp
import org.koin.core.module.Module
import org.koin.dsl.module

class NovelApp : OtakuApp() {
    override val buildModules: Module = module { includes(appModule) }

    override fun createFirebaseIds(): FirebaseIds = FirebaseIds(
        documentId = "favoriteNovels",
        chaptersId = "novelsChaptersRead",
        collectionId = "novelworld",
        itemId = "novelUrl",
        readOrWatchedId = "novelNumChapters",
    )
}