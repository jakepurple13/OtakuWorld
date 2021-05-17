package com.programmersbox.novelworld

import com.programmersbox.novel_sources.Sources
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker
import com.programmersbox.uiviews.utils.FirebaseDb

class NovelApp : OtakuApp() {
    override fun onCreated() {

        logo = R.mipmap.ic_launcher
        notificationLogo = R.mipmap.ic_launcher_foreground

        FirebaseDb.DOCUMENT_ID = "favoriteNovels"
        FirebaseDb.CHAPTERS_ID = "novelsChaptersRead"
        FirebaseDb.COLLECTION_ID = "novelworld"
        FirebaseDb.ITEM_ID = "novelUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "novelNumChapters"

        UpdateWorker.sourcesList = Sources.values().toList()
        UpdateWorker.sourceFromString = {
            try {
                Sources.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}