package com.programmersbox.mihon

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource

fun mihonApiService(source: CatalogueSource): MihonApiService =
    if (source is ConfigurableSource) ConfigurableMihonApiService(source) else MihonApiService(source)
