package com.programmersbox.aniyomi.loader

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory

object AniyomiSourceInstantiator {
    /**
     * Instantiates every class in the `;`-separated [classNames]; any failure (including LinkageError)
     * fails the whole package, like Aniyomi.
     */
    fun instantiate(classNames: String, pkg: String, loader: ClassLoader): List<AnimeCatalogueSource> = classNames
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { if (it.startsWith(".")) pkg + it else it }
        .flatMap { name ->
            when (val obj = Class.forName(name, false, loader).getDeclaredConstructor().newInstance()) {
                is AnimeSourceFactory -> obj.createSources()
                is AnimeSource -> listOf(obj)
                else -> error("Unknown source class type: ${obj.javaClass}")
            }
        }
        .filterIsInstance<AnimeCatalogueSource>()
}
