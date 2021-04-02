package com.programmersbox.animeworld

import com.programmersbox.anime_sources.Sources
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker
import com.programmersbox.uiviews.utils.FirebaseDb

class AnimeApp : OtakuApp() {
    override fun onCreated() {
        UpdateWorker.sourcesList = Sources.values().toList()

        FirebaseDb.DOCUMENT_ID = "favoriteShows"
        FirebaseDb.CHAPTERS_ID = "episodesWatched"
        FirebaseDb.COLLECTION_ID = "animeworld"
        FirebaseDb.ITEM_ID = "showUrl"

        //FirebaseDb.CHAPTER_ID = ""
    }
}