package com.programmersbox.kmpuiviews.presentation.debug.navmap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavGraphLayoutTest {

    private fun node(id: String, pseudo: Boolean = false) = NavNode(id = id, label = id, pseudo = pseudo)
    private fun edge(from: String, to: String) = NavEdge(from = from, to = to, origin = "scan")

    @Test
    fun columnsByDepth() {
        val graph = NavGraph(
            app = "t",
            nodes = listOf(node("@A", pseudo = true), node("R"), node("D"), node("Rd"), node("X")),
            edges = listOf(edge("@A", "R"), edge("R", "D"), edge("D", "Rd"), edge("Rd", "R")),
        )
        val layout = layoutGraph(graph)

        assertEquals(mapOf("@A" to 0, "R" to 1, "D" to 2, "Rd" to 3, "X" to 4), layout.placements.mapValues { it.value.column })
        assertEquals(4, layout.unreachableColumn)
        assertEquals(5, layout.columnCount)
    }

    @Test
    fun noUnreachableColumnWhenEverythingIsReachable() {
        val graph = NavGraph("t", listOf(node("@A", true), node("R")), listOf(edge("@A", "R")))
        assertEquals(null, layoutGraph(graph).unreachableColumn)
    }

    @Test
    fun orderByParents() {
        val graph = NavGraph(
            app = "t",
            nodes = listOf(node("@A", true), node("p1"), node("p2"), node("c0"), node("c1")),
            edges = listOf(edge("@A", "p1"), edge("@A", "p2"), edge("p2", "c1"), edge("p1", "c0")),
        )
        val p = layoutGraph(graph).placements
        assertTrue(p.getValue("p1").row < p.getValue("p2").row)
        assertTrue(p.getValue("c0").row < p.getValue("c1").row)
    }

    @Test
    fun inboundOutboundAndOrphans() {
        val graph = NavGraph(
            app = "t",
            nodes = listOf(node("@A", true), node("R"), node("D"), node("X"), NavNode("Y", "Y", allowOrphan = true)),
            edges = listOf(edge("@A", "R"), edge("R", "D")),
        )
        assertEquals(listOf("@A"), graph.inbound("R").map { it.from })
        assertEquals(listOf("D"), graph.outbound("R").map { it.to })
        assertEquals(listOf("X", "Y"), graph.orphans.map { it.id })
    }

    @Test
    fun parsesGeneratedJson() {
        val json = """
            {"app":"mangaworld","nodes":[{"id":"RecentScreen","label":"RecentScreen","fqcn":"p.Screen${'$'}RecentScreen","args":[],"pseudo":false,"allowOrphan":false},
            {"id":"@NavBar","label":"Nav bar","fqcn":null,"args":[],"pseudo":true,"allowOrphan":false}],
            "edges":[{"from":"@NavBar","to":"RecentScreen","origin":"override","sites":[],"note":"HomeNav.kt"}],
            "unresolved":[{"file":"a.kt","line":3,"text":"route","reason":"dynamic target"}],"extra":1}
        """.trimIndent()
        val graph = parseNavGraph(json)
        assertEquals("mangaworld", graph.app)
        assertEquals(2, graph.nodes.size)
        assertTrue(graph.edges.single().isOverride)
        assertEquals("dynamic target", graph.unresolved.single().reason)
    }
}
