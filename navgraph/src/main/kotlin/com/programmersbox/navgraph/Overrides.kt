package com.programmersbox.navgraph

data class OverrideEdge(val from: String, val to: String, val note: String?)

/**
 * Hand-written corrections to the scan. Line format (`#` starts a comment):
 * ```
 * entry @NavBar "Nav bar"                       # pseudo entry point
 * file HomeNav.kt -> @NavBar, RecentScreen      # calls in this file come from these screens
 * edge Settings -> GeneralSettings : note       # explicit edge
 * ignore DebugFragment.kt                       # no call sites from this file
 * allow-orphan ColorHelper                      # ok to have no inbound edge
 * ```
 */
data class Overrides(
    val entries: Map<String, String> = emptyMap(),
    val fileOwners: Map<String, List<String>> = emptyMap(),
    val edges: List<OverrideEdge> = emptyList(),
    val ignored: Set<String> = emptySet(),
    val allowOrphans: Set<String> = emptySet(),
) {
    operator fun plus(other: Overrides) = Overrides(
        entries = entries + other.entries,
        fileOwners = (fileOwners.keys + other.fileOwners.keys).associateWith {
            fileOwners[it].orEmpty() + other.fileOwners[it].orEmpty()
        },
        edges = edges + other.edges,
        ignored = ignored + other.ignored,
        allowOrphans = allowOrphans + other.allowOrphans,
    )
}

private val entryLine = Regex("""^entry\s+(@\w+)\s+"([^"]*)"$""")
private val fileLine = Regex("""^file\s+(\S+)\s*->\s*(.+)$""")
private val edgeLine = Regex("""^edge\s+(\S+)\s*->\s*([^:\s]+)\s*(?::\s*(.+))?$""")
private val ignoreLine = Regex("""^ignore\s+(\S+)$""")
private val orphanLine = Regex("""^allow-orphan\s+(\S+)$""")

fun parseOverrides(text: String): Overrides {
    val entries = linkedMapOf<String, String>()
    val owners = linkedMapOf<String, List<String>>()
    val edges = mutableListOf<OverrideEdge>()
    val ignored = linkedSetOf<String>()
    val orphans = linkedSetOf<String>()
    text.lines().forEachIndexed { index, raw ->
        val line = raw.substringBefore('#').trim()
        if (line.isEmpty()) return@forEachIndexed
        entryLine.matchEntire(line)?.let { entries[it.groupValues[1]] = it.groupValues[2]; return@forEachIndexed }
        fileLine.matchEntire(line)?.let { m ->
            owners[m.groupValues[1]] = owners[m.groupValues[1]].orEmpty() + m.groupValues[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            return@forEachIndexed
        }
        edgeLine.matchEntire(line)?.let { m ->
            edges += OverrideEdge(m.groupValues[1], m.groupValues[2], m.groupValues[3].trim().ifEmpty { null })
            return@forEachIndexed
        }
        ignoreLine.matchEntire(line)?.let { ignored += it.groupValues[1]; return@forEachIndexed }
        orphanLine.matchEntire(line)?.let { orphans += it.groupValues[1]; return@forEachIndexed }
        throw IllegalArgumentException("line ${index + 1}: cannot parse '$line'")
    }
    return Overrides(entries, owners, edges, ignored, orphans)
}
