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
private val overrideFun = Regex("""\boverride\s+fun\s+(?:<[^>]*>\s*)?(\w+)\s*\(\s*(\))?""")
private val actionTarget = Regex("""\bScreen(?:\.\w+)+|\bnavigate\(\s*([A-Z][\w.]*)""")

/**
 * For the class implementing `NavigationActions`, maps each overridden method to the key it pushes.
 * Overloads that take arguments are also stored as `name/args` (e.g. `customList(list)` → the list item screen).
 */
fun buildActionMethodMap(src: KotlinSource): Map<String, String> {
    val code = src.code
    if (!navigationActionsClass.containsMatchIn(code)) return emptyMap()
    val funs = overrideFun.findAll(code).toList()
    val map = linkedMapOf<String, String>()
    funs.forEachIndexed { index, m ->
        val end = funs.getOrNull(index + 1)?.range?.first ?: code.length
        val target = actionTarget.find(code.substring(m.range.last + 1, end)) ?: return@forEachIndexed
        val key = target.groupValues[1].ifEmpty { target.value }
        val name = m.groupValues[1]
        if (m.groupValues[2].isEmpty()) map.putIfAbsent("$name/args", key)
        map.putIfAbsent(name, key)
    }
    return map
}

private val receiverCall = Regex("""\b(LocalNavActions\.current|\w*(?:[Nn]av|[Aa]ctions)\w*)\s*(\.|::)\s*(\w+)""")
private val navigateMethods = setOf("navigate", "homeScreenNavigate", "clearBackStack", "navigateTo")

// Screens that take a navigation callback call it bare: `onNavigate(SomeRoute)`.
private val bareOnNavigate = Regex("""(?<![.\w])onNavigate(?=\s*\()""")
private val keyArgument = Regex("""^([A-Z]\w*(?:\.[A-Z]\w*)*)\s*(\(.*)?$""", RegexOption.DOT_MATCHES_ALL)

fun findCallSites(src: KotlinSource, methodMap: Map<String, String>): List<CallSite> {
    val code = src.code

    /** Target of the call whose `(` is at [open], or null if there are no parentheses. */
    fun navigateTarget(open: Int): CallTarget? {
        if (open >= code.length || code[open] != '(') return null
        val close = src.matchingParen(open)
        if (close < 0) return null
        val arg = firstArgument(code.substring(open + 1, close))
        return keyArgument.matchEntire(arg)?.let { CallTarget.Key(it.groupValues[1]) }
            ?: CallTarget.Dynamic(arg.replace(Regex("""\s+"""), " "))
    }

    val receiverSites = receiverCall.findAll(code).mapNotNull { m ->
        val method = m.groupValues[3]
        val isRef = m.groupValues[2] == "::"
        val after = code.skipSpaces(m.range.last + 1)
        val target: CallTarget = when {
            method in navigateMethods -> if (isRef) null else navigateTarget(after)
            method in methodMap -> when {
                isRef -> CallTarget.Key(methodMap.getValue(method))
                after < code.length && code[after] == '(' -> {
                    val noArgs = code.getOrNull(code.skipSpaces(after + 1)) == ')'
                    CallTarget.Key(if (noArgs) methodMap.getValue(method) else methodMap["$method/args"] ?: methodMap.getValue(method))
                }
                else -> null
            }
            else -> null
        } ?: return@mapNotNull null
        CallSite(src.path, m.range.first, src.lineOf(m.range.first), target)
    }
    val bareSites = bareOnNavigate.findAll(code).mapNotNull { m ->
        val target = navigateTarget(code.skipSpaces(m.range.last + 1)) as? CallTarget.Key ?: return@mapNotNull null
        CallSite(src.path, m.range.first, src.lineOf(m.range.first), target)
    }
    return (receiverSites + bareSites).sortedBy { it.offset }.toList()
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
