package com.programmersbox.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource

/** Desktop has no source settings UI yet, so sources are never exposed as configurable. */
fun aniyomiApiService(source: AnimeCatalogueSource, lib: Double): AniyomiApiService = AniyomiApiService(source, lib)
