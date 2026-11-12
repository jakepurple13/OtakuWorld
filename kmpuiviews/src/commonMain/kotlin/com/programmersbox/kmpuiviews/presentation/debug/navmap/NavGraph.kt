package com.programmersbox.kmpuiviews.presentation.debug.navmap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Mirrors navgraph/src/main/kotlin/.../NavGraphJson.kt — the JSON the :navgraph scanner writes into debug assets.

@Serializable
data class NavGraph(
    val app: String,
    val nodes: List<NavNode>,
    val edges: List<NavEdge>,
    val unresolved: List<NavUnresolved> = emptyList(),
)

@Serializable
data class NavNode(
    val id: String,
    val label: String,
    val fqcn: String? = null,
    val args: List<NavArg> = emptyList(),
    val pseudo: Boolean = false,
    val allowOrphan: Boolean = false,
)

@Serializable
data class NavArg(val name: String, val type: String, val nullable: Boolean)

@Serializable
data class NavSite(val file: String, val line: Int)

@Serializable
data class NavEdge(
    val from: String,
    val to: String,
    /** "scan" or "override". */
    val origin: String,
    val sites: List<NavSite> = emptyList(),
    val note: String? = null,
)

@Serializable
data class NavUnresolved(val file: String, val line: Int, val text: String, val reason: String)

val NavEdge.isOverride: Boolean get() = origin == "override"

fun NavGraph.inbound(id: String): List<NavEdge> = edges.filter { it.to == id }

fun NavGraph.outbound(id: String): List<NavEdge> = edges.filter { it.from == id }

/** Screens nothing navigates to (entry points excluded). */
val NavGraph.orphans: List<NavNode>
    get() = nodes.filter { n -> !n.pseudo && edges.none { it.to == n.id } }

private val navGraphJson = Json { ignoreUnknownKeys = true }

fun parseNavGraph(json: String): NavGraph = navGraphJson.decodeFromString(NavGraph.serializer(), json)
