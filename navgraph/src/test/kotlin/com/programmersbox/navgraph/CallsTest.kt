package com.programmersbox.navgraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallsTest {

    @Test
    fun registrationsSkipPlainEntry() {
        val text = """
            fun buildKmpGraph() = module {
                navigation<Screen.A> { AScreen() }
                detailEntry<Screen.B.C>(metadata = x()) {
                    HideNavBarWhileOnScreen()
                    CScreen(x)
                }
                entry<Screen.D> { DScreen() }
            }
            private inline fun <reified T : NavKey> Module.detailEntry(content: () -> Unit) = navigation<T>(metadata = y) { content(it) }
        """.trimIndent()
        val src = KotlinSource("Graph.kt", text)
        val regs = findRegistrations(src)

        assertEquals(listOf("Screen.A", "Screen.B.C", "T"), regs.map { it.keyText })
        assertEquals(listOf("HideNavBarWhileOnScreen", "CScreen"), regs[1].composables)
        val cScreen = text.indexOf("CScreen(x)")
        assertTrue(cScreen in regs[1].bodyStart..regs[1].bodyEnd)
        assertTrue(text.indexOf("DScreen") !in regs[1].bodyStart..regs[1].bodyEnd)
    }

    @Test
    fun methodMap() {
        val text = """
            class X : NavigationActions, Navigator {
                override fun recent() {
                    navBackStack.addTopLevel(Screen.RecentScreen)
                }
                override fun details(model: KmpItemModel) {
                    navBackStack.add(Screen.DetailsScreen.Details(title = model.title))
                }
                override fun library() = navigate(Screen.Settings.Library)
                override fun popBackStack() { navBackStack.removeLast() }
            }
        """.trimIndent()

        assertEquals(
            mapOf(
                "recent" to "Screen.RecentScreen",
                "details" to "Screen.DetailsScreen.Details",
                "library" to "Screen.Settings.Library",
            ),
            buildActionMethodMap(KotlinSource("Navigation3Actions.kt", text)),
        )
    }

    @Test
    fun callSites() {
        val text = """
            fun Foo() {
                navActions.details(m)
                Button(onClick = navigationActions::about)
                navController.navigate(Screen.ColorHelper)
                navActions.navigate(SubtitleSettingsRoute)
                navActions.navigate(item.videoScreen())
                LocalNavActions.current.notes()
                navActions.popBackStack()
                // navActions.notes()
                navActions.navigate(
                    Screen.ExtensionListScreen.Catalog(name)
                )
            }
        """.trimIndent()
        val map = mapOf(
            "details" to "Screen.DetailsScreen.Details",
            "about" to "Screen.AboutScreen",
            "notes" to "Screen.NotesScreen",
        )
        val sites = findCallSites(KotlinSource("Foo.kt", text), map)

        assertEquals(
            listOf(
                CallTarget.Key("Screen.DetailsScreen.Details"),
                CallTarget.Key("Screen.AboutScreen"),
                CallTarget.Key("Screen.ColorHelper"),
                CallTarget.Key("SubtitleSettingsRoute"),
                CallTarget.Dynamic("item.videoScreen()"),
                CallTarget.Key("Screen.NotesScreen"),
                CallTarget.Key("Screen.ExtensionListScreen.Catalog"),
            ),
            sites.map { it.target },
        )
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 10), sites.map { it.line })
    }
}
