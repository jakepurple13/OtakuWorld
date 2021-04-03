package com.programmersbox.mangaworld

import com.programmersbox.manga_sources.Sources
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker
import com.programmersbox.uiviews.utils.FirebaseDb

class MangaApp : OtakuApp() {
    override fun onCreated() {

        UpdateWorker.sourcesList = Sources.values().toList()
        UpdateWorker.sourceFromString = {
            try {
                Sources.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        FirebaseDb.DOCUMENT_ID = "favoriteManga"
        FirebaseDb.CHAPTERS_ID = "chaptersRead"
        FirebaseDb.COLLECTION_ID = "mangaworld"
        FirebaseDb.ITEM_ID = "mangaUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "chapters"

    }
}