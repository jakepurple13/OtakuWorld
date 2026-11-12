package com.programmersbox.navgraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OverridesTest {

    @Test
    fun parsesAllKinds() {
        val o = parseOverrides(
            """
            # comment
            entry @NavBar "Nav bar"

            file HomeNav.kt -> @NavBar
            file OptionsSheet.kt -> RecentScreen, AllScreen
            edge Settings -> GeneralSettings : settings rows in Nav3Graph.kt
            edge A -> B
            ignore DebugFragment.kt
            allow-orphan ColorHelper   # dev only
            """.trimIndent()
        )

        assertEquals(mapOf("@NavBar" to "Nav bar"), o.entries)
        assertEquals(mapOf("HomeNav.kt" to listOf("@NavBar"), "OptionsSheet.kt" to listOf("RecentScreen", "AllScreen")), o.fileOwners)
        assertEquals(
            listOf(OverrideEdge("Settings", "GeneralSettings", "settings rows in Nav3Graph.kt"), OverrideEdge("A", "B", null)),
            o.edges,
        )
        assertEquals(setOf("DebugFragment.kt"), o.ignored)
        assertEquals(setOf("ColorHelper"), o.allowOrphans)
    }

    @Test
    fun badLineThrows() {
        val e = assertFailsWith<IllegalArgumentException> { parseOverrides("edge A B") }
        assertTrue(e.message!!.startsWith("line 1:"), e.message)
    }

    @Test
    fun mergesMultipleFiles() {
        val o = parseOverrides("allow-orphan A") + parseOverrides("allow-orphan B\nentry @X \"x\"")
        assertEquals(setOf("A", "B"), o.allowOrphans)
        assertEquals(mapOf("@X" to "x"), o.entries)
    }
}
