package com.programmersbox.animeworld

import com.programmersbox.anime_sources.Sources
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.UpdateWorker

class AnimeApp : OtakuApp() {
    override fun onCreated() {
        UpdateWorker.sourcesList = Sources.values().toList()
    }
}