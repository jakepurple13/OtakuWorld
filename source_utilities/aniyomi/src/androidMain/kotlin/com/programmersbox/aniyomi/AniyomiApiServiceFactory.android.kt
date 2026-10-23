package com.programmersbox.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource

fun aniyomiApiService(source: AnimeCatalogueSource, lib: Double): AniyomiApiService =
    if (source is ConfigurableAnimeSource) ConfigurableAniyomiApiService(source, lib) else AniyomiApiService(source, lib)
