package com.programmersbox.navgraph

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Scans the real repo for every app. Fails when a screen has no way in (and isn't allow-listed)
 * or an override stops resolving. Writes a report of unresolved call sites to build/ for triage.
 */
class RepoGraphTest {

    private val root = File("..").canonicalFile

    @Test
    fun everyAppGraphIsConsistent() {
        val failures = AppSources.apps.keys.flatMap { app ->
            val graph = runCatching { scanApp(root, app) }.getOrElse { return@flatMap listOf("$app: ${it.message}") }
            File("build/navgraph-report-$app.txt").apply { parentFile.mkdirs() }.writeText(report(graph))
            graph.orphans().map { "$app: no way in to ${it.id} — add an edge override or allow-orphan" }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    @Test
    fun mangaworldHasCoreEdges() {
        val graph = scanApp(root, "mangaworld")
        fun has(from: String, to: String) = graph.edges.any { it.from == from && it.to == to }
        assertTrue(has("DetailsScreen.Details", "ReadViewModel.MangaReader"), "Details -> MangaReader")
        assertTrue(has("Settings", "Settings.Appearance"), "Settings -> Appearance")
        assertTrue(graph.nodes.any { it.pseudo }, "has entry points")
    }

    private fun NavGraphJson.orphans() = nodes.filter { n ->
        !n.pseudo && !n.allowOrphan && edges.none { it.to == n.id }
    }

    private fun report(graph: NavGraphJson) = buildString {
        appendLine("${graph.app}: ${graph.nodes.size} nodes, ${graph.edges.size} edges")
        appendLine("orphans: " + graph.orphans().joinToString { it.id })
        graph.unresolved.groupBy { it.reason }.forEach { (reason, items) ->
            appendLine("== $reason (${items.size})")
            items.forEach { appendLine("  ${it.file}:${it.line}  ${it.text}") }
        }
    }
}
