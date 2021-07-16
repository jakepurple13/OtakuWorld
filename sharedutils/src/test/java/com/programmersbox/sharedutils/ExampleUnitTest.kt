package com.programmersbox.sharedutils

import com.jakewharton.picnic.table
import com.programmersbox.helpfulutils.containsDuplicates
import org.junit.Test
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    class GradleHolder(val from: String, val library: String)

    @Test
    fun addition_isCorrect() {
        val toLibrary = GradleHolder::library
        val rootFolder = File("..").absolutePath.removeSuffix("/sharedutils/..")
        val l = listOf(
            "UIViews",
            "sharedutils",
            "novelworld", "animeworld", "animeworldtv", "mangaworld",
            "anime_sources", "manga_sources", "novel_sources",
            "Models", "favoritesdatabase"
        )
            .map { it to File("$rootFolder/$it/build.gradle") }
            .map {
                it.first to it.second
                    .readLines()
                    .filter { it.contains("implementation", true) }
                    .map { it.replace("implementation", "").trim() }
                    .map { l -> GradleHolder(it.first, l) }
            }
            .sortedByDescending { it.second.size }

        val gradles = l.joinToString("\n") {
            "${it.first} - ${it.second.size} - ${it.second.map(toLibrary).containsDuplicates()} - ${it.second.map(toLibrary)}"
        }

        println(gradles)
        println("Total libraries: ${l.flatMap { it.second }.distinctBy(toLibrary).size} - Combined: ${l.sumOf { it.second.size }}")

        val group = l.flatMap { it.second }.groupBy { it.library }

        val tableList = table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            header {
                row("Size", "Duplicates?", "Library", "From")
            }

            group
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, List<GradleHolder>>> { it.value.size }.thenBy { it.key })
                .forEach {
                    val (t, u) = it
                    row(u.size, u.containsDuplicates(), t, u.map { it.from })
                }
        }

        println(tableList)

    }
}