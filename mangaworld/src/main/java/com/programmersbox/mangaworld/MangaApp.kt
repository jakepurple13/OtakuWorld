package com.programmersbox.mangaworld

import com.programmersbox.manga_sources.Sources
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker

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
    }
}