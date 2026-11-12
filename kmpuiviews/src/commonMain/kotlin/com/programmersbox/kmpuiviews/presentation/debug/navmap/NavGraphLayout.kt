package com.programmersbox.kmpuiviews.presentation.debug.navmap

data class NodePlacement(val id: String, val column: Int, val row: Int)

data class GraphLayout(
    val placements: Map<String, NodePlacement>,
    val columnCount: Int,
    /** Column holding screens no entry point reaches, or null when every screen is reachable. */
    val unreachableColumn: Int?,
)

/**
 * Columns are BFS depth from the entry points (pseudo nodes, column 0). Screens no entry point
 * reaches share one last column. Within a column, nodes sit near the mean row of their placed parents.
 */
fun layoutGraph(graph: NavGraph): GraphLayout {
    val depth = mutableMapOf<String, Int>()
    val queue = ArrayDeque<String>()
    graph.nodes.filter { it.pseudo }.forEach { depth[it.id] = 0; queue += it.id }
    val outgoing = graph.edges.groupBy({ it.from }, { it.to })
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        outgoing[id].orEmpty().forEach { to ->
            if (to !in depth) {
                depth[to] = depth.getValue(id) + 1
                queue += to
            }
        }
    }

    val nodeIds = graph.nodes.map { it.id }.toSet()
    val maxDepth = depth.filterKeys { it in nodeIds }.values.maxOrNull() ?: -1
    val unreached = graph.nodes.filter { it.id !in depth }
    val unreachableColumn = if (unreached.isEmpty()) null else maxDepth + 1
    unreached.forEach { depth[it.id] = maxDepth + 1 }

    val incoming = graph.edges.groupBy({ it.to }, { it.from })
    val placements = mutableMapOf<String, NodePlacement>()
    graph.nodes.groupBy { depth.getValue(it.id) }.toSortedMap().forEach { (column, nodes) ->
        nodes.map { n ->
            val parentRows = incoming[n.id].orEmpty().mapNotNull { placements[it]?.row }
            val key = if (column == unreachableColumn || parentRows.isEmpty()) Double.MAX_VALUE else parentRows.average()
            n.id to key
        }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .forEachIndexed { row, (id, _) -> placements[id] = NodePlacement(id, column, row) }
    }
    return GraphLayout(placements, (placements.values.maxOfOrNull { it.column } ?: -1) + 1, unreachableColumn)
}
