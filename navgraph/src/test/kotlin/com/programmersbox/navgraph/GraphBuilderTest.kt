package com.programmersbox.navgraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphBuilderTest {

    private val sources = listOf(
        KotlinSource(
            "p/Screen.kt",
            """
            package p
            sealed class Screen(val route: String) : NavKey {
                data object RecentScreen : Screen("r")
                data object DetailsScreen : Screen("d") {
                    data class Details(val title: String) : NavKey
                }
                data object NotesScreen : Screen("n") { data class Detail(val itemUrl: String) : Screen("nd") }
                data object DictionaryScreen : Screen("di") { data class Detail(val id: Long) : Screen("dd") }
                data object ColorHelper : Screen("c")
                data object Unregistered : Screen("u")
            }
            """.trimIndent(),
        ),
        KotlinSource(
            "p/graph/Graph.kt",
            """
            package p.graph
            fun graph() = module {
                navigation<Screen.RecentScreen> { RecentView() }
                navigation<Screen.DetailsScreen.Details> {
                    DetailsScreen(it, onSearch = { navActions.navigate(Screen.RecentScreen) })
                }
                navigation<Screen.NotesScreen.Detail> { NoteDetail() }
                navigation<Screen.DictionaryScreen.Detail> { DictDetail() }
                navigation<Screen.ColorHelper> { ColorHelperScreen() }
            }
            """.trimIndent(),
        ),
        KotlinSource(
            "p/recent/RecentView.kt",
            """
            package p.recent
            fun RecentView() {
                navActions.details(m)
                navActions.navigate(Screen.RecentScreen)
                navActions.navigate(Screen.Unregistered)
            }
            """.trimIndent(),
        ),
        KotlinSource(
            "p/recent/RecentUtils.kt",
            """
            package p.recent
            fun helper() {
                navActions.navigate(Screen.ColorHelper)
                navActions.navigate(Detail)
                navActions.navigate(route)
            }
            """.trimIndent(),
        ),
        KotlinSource(
            "p/shared/Sheet.kt",
            """
            package p.shared
            fun Sheet() { navActions.details(m) }
            """.trimIndent(),
        ),
        KotlinSource(
            "p/nav/Navigation3Actions.kt",
            """
            package p.nav
            class Navigation3Actions : NavigationActions {
                override fun details(m: M) { navBackStack.add(Screen.DetailsScreen.Details(m)) }
                fun x() { navActions.navigate(Screen.ColorHelper) }
            }
            """.trimIndent(),
        ),
    )

    private val overrides = parseOverrides(
        """
        entry @NavBar "Nav bar"
        edge @NavBar -> RecentScreen
        edge RecentScreen -> DetailsScreen.Details : duplicate of a scanned edge
        allow-orphan NotesScreen.Detail
        """.trimIndent()
    )

    private val graph = buildGraph("test", sources, overrides)

    private fun edge(from: String, to: String) = graph.edges.find { it.from == from && it.to == to }

    @Test
    fun nodesAreRegisteredKeysPlusEntryPoints() {
        assertEquals(
            listOf("@NavBar", "ColorHelper", "DetailsScreen.Details", "DictionaryScreen.Detail", "NotesScreen.Detail", "RecentScreen"),
            graph.nodes.map { it.id },
        )
        val details = graph.nodes.first { it.id == "DetailsScreen.Details" }
        assertTrue(details.fqcn!!.endsWith("Screen\$DetailsScreen\$Details"))
        assertEquals(listOf(Arg("title", "String", false)), details.args)
        assertEquals(true, graph.nodes.first { it.id == "NotesScreen.Detail" }.allowOrphan)
        val navBar = graph.nodes.first { it.id == "@NavBar" }
        assertEquals("Nav bar", navBar.label)
        assertEquals(true, navBar.pseudo)
    }

    @Test
    fun scannedEdgeFromComposableFile() {
        val e = assertNotNull(edge("RecentScreen", "DetailsScreen.Details"))
        assertEquals("scan", e.origin)
        assertEquals(listOf(SiteJson("p/recent/RecentView.kt", 3)), e.sites)
        assertEquals("duplicate of a scanned edge", e.note)
    }

    @Test
    fun edgeFromInsideRegistrationLambda() {
        assertEquals("scan", edge("DetailsScreen.Details", "RecentScreen")?.origin)
    }

    @Test
    fun directoryFallback() {
        assertEquals(listOf(SiteJson("p/recent/RecentUtils.kt", 3)), edge("RecentScreen", "ColorHelper")?.sites)
    }

    @Test
    fun overrideEdge() {
        assertEquals("override", edge("@NavBar", "RecentScreen")?.origin)
    }

    @Test
    fun selfEdgesAndActionsClassIgnored() {
        assertEquals(null, edge("RecentScreen", "RecentScreen"))
        assertTrue(graph.edges.none { e -> e.sites.any { it.file.endsWith("Navigation3Actions.kt") } })
    }

    @Test
    fun unresolvedReasons() {
        val byText = graph.unresolved.associateBy { it.text }
        assertEquals("ambiguous key", byText.getValue("Detail").reason)
        assertEquals("dynamic target", byText.getValue("route").reason)
        assertEquals("unknown key", byText.getValue("Screen.Unregistered").reason)
        assertEquals("no source screen", byText.getValue("Screen.DetailsScreen.Details").reason)
        assertEquals("p/shared/Sheet.kt", byText.getValue("Screen.DetailsScreen.Details").file)
    }

    @Test
    fun fileOwnerOverride() {
        val g = buildGraph("test", sources, overrides + parseOverrides("file Sheet.kt -> ColorHelper"))
        assertEquals(listOf(SiteJson("p/shared/Sheet.kt", 2)), g.edges.find { it.from == "ColorHelper" && it.to == "DetailsScreen.Details" }?.sites)
    }

    @Test
    fun ignoredFileProducesNothing() {
        val g = buildGraph("test", sources, overrides + parseOverrides("ignore Sheet.kt"))
        assertTrue(g.unresolved.none { it.file == "p/shared/Sheet.kt" })
    }

    @Test
    fun unknownOverrideIdFails() {
        val e = assertFailsWith<IllegalStateException> {
            buildGraph("test", sources, parseOverrides("edge RecentScreen -> Nope"))
        }
        assertTrue(e.message!!.contains("Nope"), e.message)
    }
}
