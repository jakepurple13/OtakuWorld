package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavKeyFactoryTest {

    private val screen = "com.programmersbox.kmpuiviews.presentation.Screen"

    @Test
    fun dataObject() {
        assertEquals(Screen.RecentScreen, createNavKey("$screen\$RecentScreen", emptyList()))
    }

    @Test
    fun dataClassWithSampleStrings() {
        val args = listOf("title", "description", "url", "imageUrl", "source").map { NavArg(it, "String", false) }
        assertEquals(
            Screen.DetailsScreen.Details("sample", "sample", "sample", "sample", "sample"),
            createNavKey("$screen\$DetailsScreen\$Details", args),
        )
    }

    @Test
    fun nullableArgsAreNull() {
        assertEquals(Screen.DictionaryScreen.Form(null), createNavKey("$screen\$DictionaryScreen\$Form", listOf(NavArg("id", "Long", true))))
    }

    @Test
    fun numbersAndBooleans() {
        assertEquals(
            Sample(1L, 1, false, "sample"),
            createNavKey(Sample::class.java.name, listOf(NavArg("a", "Long", false), NavArg("b", "Int", false), NavArg("c", "Boolean", false), NavArg("d", "String", false))),
        )
    }

    @Test
    fun unsupportedArgTypeReturnsNull() {
        assertNull(createNavKey(Bad::class.java.name, listOf(NavArg("x", "List<String>", false))))
    }

    @Test
    fun unknownClassReturnsNull() {
        assertNull(createNavKey("com.example.Nope", emptyList()))
    }

    data class Sample(val a: Long, val b: Int, val c: Boolean, val d: String) : NavKey
    data class Bad(val x: List<String>) : NavKey
}
