package com.programmersbox.navgraph

import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarationsTest {

    private val screenFile = """
        package p

        sealed class Screen(val route: String) : NavKey {
            @Serializable
            data object RecentScreen : Screen("recent")

            data object DetailsScreen : Screen("details") {
                @Serializable
                data class Details(
                    val title: String,
                    val url: String,
                ) : NavKey
            }

            data object NotesScreen : Screen("notes") { data class Detail(val itemUrl: String) : Screen("notes_detail") }

            data class GlobalSearchScreen(val title: String? = null) : Screen("gs")
        }

        class NotAKey(val x: Int)

        @Serializable
        data class MangaReader(val downloaded: Boolean, val seedColor: Int? = null, val map: Map<String, Int> = emptyMap()) : NavKey
    """.trimIndent()

    private val decls = findNavKeyDeclarations(KotlinSource("Screen.kt", screenFile)).associateBy { it.id }

    @Test
    fun nestedScreenKeys() {
        assertEquals(
            setOf(
                "Screen", "RecentScreen", "DetailsScreen", "DetailsScreen.Details", "NotesScreen",
                "NotesScreen.Detail", "GlobalSearchScreen", "MangaReader",
            ),
            decls.keys,
        )
        assertEquals("p.Screen\$DetailsScreen\$Details", decls.getValue("DetailsScreen.Details").fqcn)
        assertEquals("p.MangaReader", decls.getValue("MangaReader").fqcn)
    }

    @Test
    fun args() {
        assertEquals(listOf(Arg("title", "String", true)), decls.getValue("GlobalSearchScreen").args)
        assertEquals(
            listOf(Arg("title", "String", false), Arg("url", "String", false)),
            decls.getValue("DetailsScreen.Details").args,
        )
        assertEquals(
            listOf(Arg("downloaded", "Boolean", false), Arg("seedColor", "Int", true), Arg("map", "Map<String, Int>", false)),
            decls.getValue("MangaReader").args,
        )
        assertEquals(emptyList(), decls.getValue("RecentScreen").args)
    }
}
