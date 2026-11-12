package com.programmersbox.navgraph

private sealed interface Resolution {
    data class Found(val id: String) : Resolution
    data class Missing(val reason: String) : Resolution
}

/** Resolves [text] to a node id: exact match (leading `Screen.` ignored), else the unique id ending in `.text`. */
private fun resolve(text: String, ids: Collection<String>): Resolution {
    val t = text.removePrefix("Screen.")
    if (t in ids) return Resolution.Found(t)
    val matches = ids.filter { it.endsWith(".$t") }
    return when (matches.size) {
        1 -> Resolution.Found(matches.single())
        0 -> Resolution.Missing("unknown key")
        else -> Resolution.Missing("ambiguous key")
    }
}

private val funDecl = Regex("""\bfun\s+(?:<[^>]*>\s*)?(\w+)\s*\(""")
private val actionsInterface = Regex("""\binterface\s+NavigationActions\b|:[^{]*\bNavigationActions\b""")

fun buildGraph(app: String, sources: List<KotlinSource>, overrides: Overrides): NavGraphJson {
    val declarations = sources.flatMap(::findNavKeyDeclarations).associateBy { it.id }
    val registrations = sources.flatMap(::findRegistrations)
    val methodMap = sources.fold(emptyMap<String, String>()) { acc, s -> acc + buildActionMethodMap(s) }

    // Nodes: registered keys that resolve to a declaration, plus pseudo entry points.
    val keyRegs = registrations.mapNotNull { r ->
        (resolve(r.keyText, declarations.keys) as? Resolution.Found)?.let { r to it.id }
    }
    val keyIds = keyRegs.map { it.second }.toSortedSet()
    val allIds = keyIds + overrides.entries.keys

    // Every id an override names must exist.
    val problems = mutableListOf<String>()
    fun overrideId(text: String, where: String): String? = when {
        text.startsWith("@") -> text.takeIf { it in overrides.entries } ?: null.also { problems += "$text ($where)" }
        else -> (resolve(text, keyIds) as? Resolution.Found)?.id ?: null.also { problems += "$text ($where)" }
    }
    val fileOwners = overrides.fileOwners.mapValues { (file, owners) -> owners.mapNotNull { overrideId(it, "file $file") } }
    val overrideEdges = overrides.edges.mapNotNull { e ->
        val from = overrideId(e.from, "edge ${e.from} -> ${e.to}")
        val to = overrideId(e.to, "edge ${e.from} -> ${e.to}")
        if (from != null && to != null) Triple(from, to, e.note) else null
    }
    val allowOrphans = overrides.allowOrphans.mapNotNull { overrideId(it, "allow-orphan") }.toSet()
    check(problems.isEmpty()) { "Unresolved override ids: ${problems.joinToString()}" }

    // File → screen, from the files that declare each registration's composables.
    val registrationFiles = registrations.map { it.file }.toSet()
    val declaredIn = sources.filter { it.path !in registrationFiles }
        .flatMap { s -> funDecl.findAll(s.code).map { it.groupValues[1] to s.path } }
        .groupBy({ it.first }, { it.second })
    val claims = keyRegs.flatMap { (r, id) -> r.composables.flatMap { declaredIn[it].orEmpty() }.map { it to id } }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSet() }
    val fileScreen = claims.filterValues { it.size == 1 }.mapValues { it.value.single() }
    fun dirOf(path: String) = path.substringBeforeLast('/', "")
    val dirScreen = fileScreen.entries.groupBy({ dirOf(it.key) }, { it.value })
        .mapValues { it.value.toSet() }
        .filterValues { it.size == 1 }
        .mapValues { it.value.single() }

    data class Acc(var scanned: Boolean, val sites: MutableList<SiteJson>, var note: String?)
    val edges = linkedMapOf<Pair<String, String>, Acc>()
    val unresolved = mutableListOf<UnresolvedJson>()

    for (src in sources) {
        if (src.fileName in overrides.ignored || actionsInterface.containsMatchIn(src.code)) continue
        val fileRegs = keyRegs.filter { it.first.file == src.path }
        for (site in findCallSites(src, methodMap)) {
            val targetText = when (val t = site.target) {
                is CallTarget.Key -> t.text
                is CallTarget.Dynamic -> {
                    unresolved += UnresolvedJson(src.path, site.line, t.text, "dynamic target"); continue
                }
            }
            val to = when (val r = resolve(targetText, keyIds)) {
                is Resolution.Found -> r.id
                is Resolution.Missing -> {
                    unresolved += UnresolvedJson(src.path, site.line, targetText, r.reason); continue
                }
            }
            val from: List<String> = fileOwners[src.fileName]
                ?: fileRegs.filter { site.offset in it.first.bodyStart..it.first.bodyEnd }
                    .minByOrNull { it.first.bodyEnd - it.first.bodyStart }?.let { listOf(it.second) }
                ?: fileScreen[src.path]?.let(::listOf)
                ?: dirScreen[dirOf(src.path)]?.let(::listOf)
                ?: run {
                    unresolved += UnresolvedJson(src.path, site.line, targetText, "no source screen"); continue
                }
            for (f in from) {
                if (f == to) continue
                edges.getOrPut(f to to) { Acc(true, mutableListOf(), null) }.sites += SiteJson(src.path, site.line)
            }
        }
    }
    for ((from, to, note) in overrideEdges) {
        if (from == to) continue
        val acc = edges.getOrPut(from to to) { Acc(false, mutableListOf(), null) }
        if (acc.note == null) acc.note = note
    }

    val nodes = keyIds.map { id ->
        val d = declarations.getValue(id)
        NodeJson(id, id, d.fqcn, d.args, pseudo = false, allowOrphan = id in allowOrphans)
    } + overrides.entries.map { (id, label) -> NodeJson(id, label, null, emptyList(), pseudo = true, allowOrphan = false) }

    return NavGraphJson(
        app = app,
        nodes = nodes.sortedBy { it.id },
        edges = edges.map { (k, a) -> EdgeJson(k.first, k.second, if (a.scanned) "scan" else "override", a.sites, a.note) }
            .sortedWith(compareBy({ it.from }, { it.to })),
        unresolved = unresolved.sortedWith(compareBy({ it.file }, { it.line })),
    )
}
