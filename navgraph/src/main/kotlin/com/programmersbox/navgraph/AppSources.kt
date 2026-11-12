package com.programmersbox.navgraph

import java.io.File

/** Which module directories make up each Android app, and which overrides apply. Paths are relative to the repo root. */
data class AppConfig(
    val dirs: List<String>,
    val excludedSourceSets: Set<String>,
    val overrides: List<String>,
)

object AppSources {
    // Source sets that never run in the Android apps.
    private val androidExcluded = setOf("jvmMain", "iosMain", "skikoMain", "httpMain")

    // Source sets that never run on desktop (deviceMain is Android + iOS).
    private val desktopExcluded = setOf("androidMain", "iosMain", "deviceMain", "main")

    private val shared = listOf(
        "kmpuiviews",
        "favoritesdatabase/supabase-integration",
    )

    private const val COMMON = "navgraph/overrides/common.navgraph"
    private const val ANDROID = "navgraph/overrides/android.navgraph"

    val apps: Map<String, AppConfig> = mapOf(
        "mangaworld" to AppConfig(
            dirs = shared + listOf("UIViews", "mihonsupport", "mangaworld/shared", "mangaworld/src"),
            excludedSourceSets = androidExcluded,
            overrides = listOf(COMMON, ANDROID, "navgraph/overrides/mangaworld.navgraph"),
        ),
        "animeworld" to AppConfig(
            dirs = shared + listOf("UIViews", "animeworld/shared", "animeworld/src"),
            excludedSourceSets = androidExcluded,
            overrides = listOf(COMMON, ANDROID, "navgraph/overrides/animeworld.navgraph"),
        ),
        "novelworld" to AppConfig(
            dirs = shared + listOf("UIViews", "novelworld/shared", "novelworld/src"),
            excludedSourceSets = androidExcluded,
            overrides = listOf(COMMON, ANDROID, "navgraph/overrides/novelworld.navgraph"),
        ),
        "mangaworld-desktop" to AppConfig(
            dirs = shared + listOf("mihonsupport", "mangaworld/shared", "mangaworld/desktop"),
            excludedSourceSets = desktopExcluded,
            overrides = listOf(COMMON, "navgraph/overrides/mangaworld.navgraph"),
        ),
    )
}

/** Source set name of a repo-relative path: the segment after the last `src/`, or null. */
internal fun sourceSetOf(relativePath: String): String? =
    relativePath.split('/').let { parts ->
        val i = parts.lastIndexOf("src")
        if (i >= 0 && i + 1 < parts.size - 1) parts[i + 1] else null
    }

private fun isTestSourceSet(name: String) = name == "test" || name.endsWith("Test")

fun scanApp(root: File, app: String): NavGraphJson {
    val config = requireNotNull(AppSources.apps[app]) { "Unknown app '$app'. Known: ${AppSources.apps.keys}" }
    val sources = config.dirs.asSequence()
        .flatMap { File(root, it).walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }
        .map { it.relativeTo(root).invariantSeparatorsPath }
        .distinct()
        .filter { "/build/" !in "/$it" }
        .filter { path ->
            val set = sourceSetOf(path)
            set == null || (!isTestSourceSet(set) && set !in config.excludedSourceSets)
        }
        .sorted()
        .map { KotlinSource(it, File(root, it).readText()) }
        .toList()
    val overrides = config.overrides
        .map { File(root, it) }
        .filter { it.exists() }
        .fold(Overrides()) { acc, f ->
            acc + runCatching { parseOverrides(f.readText()) }.getOrElse { throw IllegalArgumentException("${f.name} ${it.message}", it) }
        }
    return buildGraph(app, sources, overrides)
}
