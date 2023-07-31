package com.programmersbox.defaultmangasources

import com.programmersbox.manga_sources.Sources
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog

object MangaSources : ApiServicesCatalog {
    override fun createSources(): List<ApiService> = Sources.entries.filterNot { it.notWorking }

    override val name: String get() = "Default Manga Sources"
}