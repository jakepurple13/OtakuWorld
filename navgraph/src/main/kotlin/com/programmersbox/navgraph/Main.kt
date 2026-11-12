package com.programmersbox.navgraph

import kotlinx.serialization.json.Json
import java.io.File

/** Usage: `--app <name> --root <repoDir> --out <file>` */
fun main(args: Array<String>) {
    val opts = args.toList().chunked(2).associate { (k, v) -> k.removePrefix("--") to v }
    val app = requireNotNull(opts["app"]) { "--app is required" }
    val root = File(requireNotNull(opts["root"]) { "--root is required" })
    val out = File(requireNotNull(opts["out"]) { "--out is required" })

    val graph = scanApp(root, app)
    out.parentFile?.mkdirs()
    out.writeText(Json { encodeDefaults = true }.encodeToString(NavGraphJson.serializer(), graph))
    println("navgraph: $app → ${graph.nodes.size} screens, ${graph.edges.size} edges, ${graph.unresolved.size} unresolved")
}
