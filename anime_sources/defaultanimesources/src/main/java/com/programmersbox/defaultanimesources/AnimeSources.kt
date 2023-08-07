package com.programmersbox.defaultanimesources

import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog

object AnimeSources : ApiServicesCatalog {
    override fun createSources(): List<ApiService> = Sources.entries.filterNot { it.notWorking }.toList()

    override val name: String get() = "Default Anime Sources"
}