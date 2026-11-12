package com.programmersbox.navgraph

/** A `navigation<Key> { ... }` style registration. [composables] are capitalised calls in the body, in order. */
data class Registration(
    val keyText: String,
    val file: String,
    val bodyStart: Int,
    val bodyEnd: Int,
    val composables: List<String>,
)

sealed interface CallTarget {
    data class Key(val text: String) : CallTarget
    data class Dynamic(val text: String) : CallTarget
}

data class CallSite(val file: String, val offset: Int, val line: Int, val target: CallTarget)

// `navigation<K>` plus helpers such as detailEntry/twoPaneEntry/dialogEntry; plain `entry<K>` is excluded.
private val registrationRegex = Regex("""\b(navigation|[a-z]\w*Entry)<([\w.]+)>""")
private val capitalisedCall = Regex("""\b([A-Z]\w*)\s*\(""")

fun findRegistrations(src: KotlinSource): List<Registration> {
    val code = src.code
    return registrationRegex.findAll(code).mapNotNull { m ->
        var i = code.skipSpaces(m.range.last + 1)
        if (i < code.length && code[i] == '(') {
            val close = src.matchingParen(i)
            if (close < 0) return@mapNotNull null
            i = code.skipSpaces(close + 1)
        }
        if (i >= code.length || code[i] != '{') return@mapNotNull null
        val end = src.matchingBrace(i)
        if (end < 0) return@mapNotNull null
        val composables = capitalisedCall.findAll(code.substring(i, end)).map { it.groupValues[1] }.distinct().toList()
        Registration(m.groupValues[2], src.path, i, end, composables)
    }.toList()
}

private val navigationActionsClass = Regex("""\bclass\s+\w+[^{]*:[^{]*\bNavigationActions\b""")
private val overrideFun = Regex("""\boverride\s+fun\s+(\w+)""")
private val actionTarget = Regex("""\bScreen(?:\.\w+)+|\bnavigate\(\s*([\w.]+)""")

/** For the class implementing `NavigationActions`, maps each overridden method to the key it pushes. */
fun buildActionMethodMap(src: KotlinSource): Map<String, String> {
    val code = src.code
    if (!navigationActionsClass.containsMatchIn(code)) return emptyMap()
    val funs = overrideFun.findAll(code).toList()
    val map = linkedMapOf<String, String>()
    funs.forEachIndexed { index, m ->
        val end = funs.getOrNull(index + 1)?.range?.first ?: code.length
        val target = actionTarget.find(code.substring(m.range.last + 1, end)) ?: return@forEachIndexed
        val key = target.groupValues[1].ifEmpty { target.value }
        map.putIfAbsent(m.groupValues[1], key)
    }
    return map
}

private val receiverCall = Regex("""\b(LocalNavActions\.current|\w*(?:[Nn]av|[Aa]ctions)\w*)\s*(\.|::)\s*(\w+)""")
private val navigateMethods = setOf("navigate", "homeScreenNavigate", "clearBackStack")
private val keyArgument = Regex("""^([A-Z]\w*(?:\.[A-Z]\w*)*)\s*(\(.*)?$""", RegexOption.DOT_MATCHES_ALL)

fun findCallSites(src: KotlinSource, methodMap: Map<String, String>): List<CallSite> {
    val code = src.code
    return receiverCall.findAll(code).mapNotNull { m ->
        val method = m.groupValues[3]
        val isRef = m.groupValues[2] == "::"
        val after = code.skipSpaces(m.range.last + 1)
        val target: CallTarget = when {
            method in navigateMethods -> {
                if (isRef || after >= code.length || code[after] != '(') return@mapNotNull null
                val close = src.matchingParen(after)
                if (close < 0) return@mapNotNull null
                val arg = firstArgument(code.substring(after + 1, close))
                keyArgument.matchEntire(arg)?.let { CallTarget.Key(it.groupValues[1]) }
                    ?: CallTarget.Dynamic(arg.replace(Regex("""\s+"""), " "))
            }

            method in methodMap -> {
                if (!isRef && (after >= code.length || code[after] != '(')) return@mapNotNull null
                CallTarget.Key(methodMap.getValue(method))
            }

            else -> return@mapNotNull null
        }
        CallSite(src.path, m.range.first, src.lineOf(m.range.first), target)
    }.toList()
}

private fun firstArgument(args: String): String {
    var depth = 0
    args.forEachIndexed { i, c ->
        when (c) {
            '(', '[', '{', '<' -> depth++
            ')', ']', '}', '>' -> depth--
            ',' -> if (depth == 0) return args.substring(0, i).trim()
        }
    }
    return args.trim()
}

internal fun String.skipSpaces(from: Int): Int {
    var i = from
    while (i < length && this[i].isWhitespace()) i++
    return i
}
