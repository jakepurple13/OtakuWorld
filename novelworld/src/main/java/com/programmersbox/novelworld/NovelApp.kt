package com.programmersbox.novelworld

import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.OtakuApp
import org.koin.core.module.Module

class NovelApp : OtakuApp() {
    override fun Module.buildModules() {
        includes(appModule)
    }

    override fun onCreated() {
        FirebaseDb.DOCUMENT_ID = "favoriteNovels"
        FirebaseDb.CHAPTERS_ID = "novelsChaptersRead"
        FirebaseDb.COLLECTION_ID = "novelworld"
        FirebaseDb.ITEM_ID = "novelUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "novelNumChapters"
    }
}