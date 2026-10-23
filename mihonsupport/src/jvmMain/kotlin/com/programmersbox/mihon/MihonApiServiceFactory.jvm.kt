package com.programmersbox.mihon

import eu.kanade.tachiyomi.source.CatalogueSource

/** Desktop has no source settings UI yet, so sources are never exposed as configurable. */
fun mihonApiService(source: CatalogueSource): MihonApiService = MihonApiService(source)
