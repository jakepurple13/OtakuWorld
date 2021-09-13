package com.programmersbox.sharedutils

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.jakewharton.picnic.table
import com.lordcodes.turtle.shellRun
import com.programmersbox.helpfulutils.containsDuplicates
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private fun getFreqMap(chars: String): Map<Char, Int> {
        val freq: MutableMap<Char, Int> = HashMap()
        for (c in chars) {
            freq.putIfAbsent(c, 0)
            freq[c] = freq[c]!! + 1
        }
        return freq
    }

    @Test
    fun letterCountTest() = runBlocking {
        val root = File(File("..").absolutePath.removeSuffix("/sharedutils/.."))
        val f = shellRun(root) { git.gitCommand(listOf("ls-files")) }
            .split("\n")
            .filter { it.endsWith(".kt") }
            .fastMap { it to File("$root/$it") }
            .fastMap { it.second.readText() }
            .joinToString("")
            .let { getFreqMap(it) }
            .toList()
            .sortedByDescending { it.second }

        val tableList = table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            header { row("Count", "Path") }
            f.fastForEach { row(it.first, it.second) }
            footer { row("Total: ${f.sumOf { it.second }}") }
        }

        println(tableList)

    }

    @Test
    fun lineCountTest() = runBlocking {
        val root = File(File("..").absolutePath.removeSuffix("/sharedutils/.."))
        val q = shellRun(root) { git.gitCommand(listOf("ls-files")) }
            .split("\n")
            .filter { it.endsWith(".kt") || it.endsWith(".xml") }

        val f = q
            .fastMap { it to File("$root/$it") }
            .fastMap { it.second.absolutePath to it.second.readLines().size }
            .sortedByDescending { it.second }

        val tableList = table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            header { row("Size", "Path") }
            f.fastForEach { row(it.second, it.first) }
            footer { row(f.sumOf { it.second }, "Files: ${q.size}") }
        }

        println(tableList)
    }

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
                    .filter { i -> i.contains("implementation", true) }
                    .map { i -> i.replace("implementation", "").trim() }
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
                    row(u.size, u.containsDuplicates(), t, u.map { i -> i.from })
                }
        }

        println(tableList)

    }
}