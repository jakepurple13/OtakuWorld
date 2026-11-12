package com.programmersbox.navgraph

import kotlinx.serialization.Serializable

@Serializable
data class Arg(val name: String, val type: String, val nullable: Boolean)

/**
 * A Navigation3 key declaration. [id] is the qualified name inside the package with a leading
 * `Screen.` removed; [fqcn] is the JVM class name (`$` between nested classes).
 */
data class NavKeyDecl(val id: String, val fqcn: String, val args: List<Arg>)

private val declRegex = Regex("""\b(class|object|interface)\s+(\w+)""")
private val navKeySuper = Regex("""\bNavKey\b|\bScreen\s*\(""")

private class RawDecl(val name: String, val start: Int, val bodyStart: Int, val bodyEnd: Int, val supertypes: String, val params: String?)

fun findNavKeyDeclarations(src: KotlinSource): List<NavKeyDecl> {
    val code = src.code
    val raws = declRegex.findAll(code).map { m ->
        var i = m.range.last + 1
        // Skip type parameters like `class Foo<T>`.
        while (i < code.length && code[i] == ' ') i++
        if (i < code.length && code[i] == '<') i = KotlinSource.matchingClose(code, i, '<', '>').let { if (it < 0) i else it + 1 }
        // Optional constructor, possibly after annotations / modifiers like `private constructor`.
        val parenStart = code.indexOf('(', i).takeIf { p -> p >= 0 && code.substring(i, p).isBlankOrConstructor() }
        var params: String? = null
        if (parenStart != null) {
            val close = src.matchingParen(parenStart)
            if (close > 0) {
                params = code.substring(parenStart + 1, close)
                i = close + 1
            }
        }
        // Supertypes run to the body brace or end of line.
        var j = i
        while (j < code.length && code[j] != '{' && code[j] != '\n' && code[j] != '}') j++
        val supertypes = code.substring(i, j)
        val (bodyStart, bodyEnd) = if (j < code.length && code[j] == '{') j to src.matchingBrace(j) else -1 to -1
        RawDecl(m.groupValues[2], m.range.first, bodyStart, bodyEnd, supertypes, params)
    }.toList()

    return raws.filter { navKeySuper.containsMatchIn(it.supertypes) }.map { d ->
        val chain = raws.filter { it.bodyStart in 0 until d.start && d.start < it.bodyEnd }
            .sortedBy { it.bodyStart }
            .map { it.name } + d.name
        val id = chain.joinToString(".").removePrefix("Screen.")
        val pkg = src.packageName.let { if (it.isEmpty()) "" else "$it." }
        NavKeyDecl(id = id, fqcn = pkg + chain.joinToString("$"), args = d.params?.let(::parseParams).orEmpty())
    }
}

private fun String.isBlankOrConstructor(): Boolean =
    replace(Regex("""@\w+|\b(private|internal|protected|public|constructor)\b"""), "").isBlank()

private val annotation = Regex("""@[\w.]+(\([^)]*\))?""")
private val modifiers = Regex("""\b(val|var|private|internal|protected|public|override|open|vararg)\b""")

private fun parseParams(params: String): List<Arg> = splitTopLevel(params)
    .map { it.replace(annotation, " ").replace(modifiers, " ").trim() }
    .filter { it.contains(':') }
    .map { p ->
        val name = p.substringBefore(':').trim()
        val type = splitTopLevel(p.substringAfter(':'), '=').first().trim()
        Arg(name, type.removeSuffix("?").trim(), type.endsWith("?"))
    }

/** Splits on [sep] outside of (), <>, [] and {}. */
private fun splitTopLevel(s: String, sep: Char = ','): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var last = 0
    s.forEachIndexed { i, c ->
        when (c) {
            '(', '<', '[', '{' -> depth++
            ')', '>', ']', '}' -> if (!(c == '>' && i > 0 && s[i - 1] == '-')) depth--
            sep -> if (depth == 0) { parts += s.substring(last, i); last = i + 1 }
        }
    }
    parts += s.substring(last)
    return parts.filter { it.isNotBlank() }
}
