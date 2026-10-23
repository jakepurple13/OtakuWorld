package com.programmersbox.mihon.loader

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

object MihonSourceInstantiator {
    fun classNames(pkg: String, metadataValue: String?): List<String> = metadataValue.orEmpty()
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { if (it.startsWith(".")) pkg + it else it }

    /** Instantiates every class; any failure (including LinkageError) fails the whole package, like Mihon. */
    fun instantiate(classNames: List<String>, classLoader: ClassLoader): Result<List<CatalogueSource>> = try {
        Result.success(
            classNames.flatMap { name ->
                when (val obj = Class.forName(name, false, classLoader).getDeclaredConstructor().newInstance()) {
                    is SourceFactory -> obj.createSources()
                    is Source -> listOf(obj)
                    else -> error("Unknown source class type: ${obj.javaClass}")
                }
            }.filterIsInstance<CatalogueSource>()
        )
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
