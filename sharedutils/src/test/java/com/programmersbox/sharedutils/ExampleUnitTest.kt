package com.programmersbox.sharedutils

import androidx.compose.ui.util.*
import com.jakewharton.picnic.table
import com.lordcodes.turtle.shellRun
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
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

    @Test
    fun gsonTest() {

        val f = "asdf\nasdf\""
        println(f)
        println("---")
        val j = f.toJson()
        println(j)
        println("---")
        val s = j.fromJson<String>()
        println(s)
        println("---")
        println("""asdfadsfadsf \n adsfasdfasd"f""".trimIndent())
        println("---")
        println("""asdf\nasdf$f asdf""")

    }

    @Test
    fun newUpdateCheckTest() {

        val oldVersion = "1.5.1"
        val newVersion = "1.0.1"

        println(AppUpdate.checkForUpdate(oldVersion, newVersion))

        println("-".repeat(50))
        var newVersion2 = "1.0.0"
        println(AppUpdate.checkForUpdate(oldVersion, newVersion2))
        newVersion2 = "1.0.1"
        println(AppUpdate.checkForUpdate(oldVersion, newVersion2))
        newVersion2 = "1.1.0"
        println(AppUpdate.checkForUpdate(oldVersion, newVersion2))
        newVersion2 = "2.0.0"
        println(AppUpdate.checkForUpdate(oldVersion, newVersion2))
        newVersion2 = "1.1.1"
        println(AppUpdate.checkForUpdate(oldVersion, newVersion2))

    }

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
    fun importCount() = runBlocking {
        val root = File(File("..").absolutePath.removeSuffix("/sharedutils/.."))

        val q = shellRun(root) { git.gitCommand(listOf("ls-files")) }
            .split("\n")
            .filter { s -> s.endsWith(".kt") }
            .filter { !it.contains("ExampleInstrumentedTest.kt") && !it.contains("ExampleUnitTest.kt") }

        val f = q
            .fastMap { File("$root/$it") }
            .fastMap { it to it.readLines() }
            .filter { it.second.fastAny { s -> s.contains("import") } }
            .fastMap { it.first to it.second.filter { s -> s.contains("import") } }
            .sortedWith(
                compareByDescending<Pair<File, List<String>>> { it.second.size }
                    .thenBy { it.second.joinToString("\n").length }
                    .thenBy { it.first.name }
                    .thenBy { it.first.absolutePath }
            )

        val tableList = table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            header {
                row("import")
                row("Size", "Letters", "Name", "Path")
            }

            f.fastForEach {
                row(
                    it.second.size,
                    it.second.joinToString("\n").length,
                    it.first.name,
                    it.first.absolutePath
                )
            }

            f.groupBy { it.first.absolutePath.substring(it.first.absolutePath.lastIndexOf(".")) }
                .toList()
                .fastForEach { g ->
                    row(
                        g.second.fastSumBy { it.second.size },
                        g.second.fastSumBy { it.second.joinToString("\n").length },
                        "%s Files: %d".format(
                            g.first.removePrefix(".").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            g.second.size
                        ),
                        "Largest: ${g.second.fastMaxBy { it.second.size }!!.first.name} => ${g.second.maxOf { it.second.size }}",
                    )
                }

            footer {
                row(
                    f.fastSumBy { it.second.size },
                    f.fastSumBy { it.second.joinToString("\n").length },
                    "Total Files: ${q.size}",
                    root.absolutePath
                )
            }
        }

        println(tableList)
    }

    @Test
    fun fileInfoTest() = runBlocking {
        val grouped = true

        val root = File(File("..").absolutePath.removeSuffix("/sharedutils/.."))

        val fileType = listOf(
            ".kt",
            ".kts",
            ".xml",
            ".gradle",
            ".json",
            ".pro",
            ".properties",
        )

        val q = shellRun(root) { git.gitCommand(listOf("ls-files")) }
            .split("\n")
            .filter { s -> fileType.fastAny(s::endsWith) }
            .filter { !it.contains("ExampleInstrumentedTest.kt") && !it.contains("ExampleUnitTest.kt") }

        val f = q
            .fastMap { File("$root/$it") }
            .fastMap { it to it.readLines() }
            .sortedWith(
                compareByDescending<Pair<File, List<String>>> { it.second.size }
                    .thenBy { it.second.joinToString("\n").length }
                    .thenBy { it.first.name }
                    .thenBy { it.first.absolutePath }
            )

        val tableList = table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            header { row("Size", "Letters", "Name", "Path") }
            if (grouped) {
                f.groupBy { it.first.absolutePath.substring(it.first.absolutePath.lastIndexOf(".")) }.toList()
                    .fastForEach {
                        row(it.first.removePrefix(".").replaceFirstChar { s -> if (s.isLowerCase()) s.titlecase() else s.toString() })
                        it.second.fastForEach { s ->
                            row(
                                s.second.size,
                                s.second.joinToString("\n").length,
                                s.first.name,
                                s.first.absolutePath
                            )
                        }
                    }
            } else {
                f.fastForEach {
                    row(
                        it.second.size,
                        it.second.joinToString("\n").length,
                        it.first.name,
                        it.first.absolutePath
                    )
                }
            }

            row("Stats")

            f.groupBy { it.first.absolutePath.substring(it.first.absolutePath.lastIndexOf(".")) }
                .toList()
                .fastForEach { g ->
                    row(
                        g.second.fastSumBy { it.second.size },
                        g.second.fastSumBy { it.second.joinToString("\n").length },
                        "%s Files: %d".format(
                            g.first.removePrefix(".").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            g.second.size
                        ),
                        "Largest: ${g.second.fastMaxBy { it.second.size }!!.first.name} => ${g.second.maxOf { it.second.size }}",
                    )
                }

            footer {
                row(
                    f.fastSumBy { it.second.size },
                    f.fastSumBy { it.second.joinToString("\n").length },
                    "Total Files: ${q.size}",
                    root.absolutePath
                )
            }
        }

        println(tableList)
    }

    class GradleHolder(val from: String, val library: String)

    @Test
    fun dependencyInfo() {
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