package com.programmersbox.navgraph

import kotlinx.serialization.Serializable

// Contract with kmpuiviews' navmap/NavGraph.kt — keep field names in sync.

@Serializable
data class NavGraphJson(
    val app: String,
    val nodes: List<NodeJson>,
    val edges: List<EdgeJson>,
    val unresolved: List<UnresolvedJson>,
)

@Serializable
data class NodeJson(
    val id: String,
    val label: String,
    val fqcn: String?,
    val args: List<Arg>,
    val pseudo: Boolean,
    val allowOrphan: Boolean,
)

@Serializable
data class SiteJson(val file: String, val line: Int)

@Serializable
data class EdgeJson(
    val from: String,
    val to: String,
    /** "scan" or "override". */
    val origin: String,
    val sites: List<SiteJson>,
    val note: String?,
)

@Serializable
data class UnresolvedJson(val file: String, val line: Int, val text: String, val reason: String)
