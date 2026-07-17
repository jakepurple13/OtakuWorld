package com.programmersbox.jsextensionloader

object TsTranspiler {

    private val interfaceStart = Regex("""interface\s+\w+[^{]*\{""")
    private val functionSignature = Regex(
        """(export\s+)?function\s+(\w+)\s*\(([^)]*)\)\s*(:\s*[^{]+)?\s*\{"""
    )
    private val asCast = Regex("""\s+as\s+[\w.]+(\[\])?""")
    private val leadingExport = Regex("""^export\s+(default\s+)?""")

    fun transpile(source: String): String {
        var result = stripInterfaceBlocks(source)
        result = stripTypeAliasLines(result)
        result = functionSignature.replace(result) { match ->
            val name = match.groupValues[2]
            val params = stripParamTypes(match.groupValues[3])
            "function $name($params) {"
        }
        result = asCast.replace(result, "")
        result = leadingExport.replace(result, "")
        return result
    }

    private fun stripParamTypes(params: String): String =
        params.split(",")
            .map { it.substringBefore(":").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

    private fun stripInterfaceBlocks(source: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < source.length) {
            val match = interfaceStart.find(source, index)
            if (match == null) {
                builder.append(source, index, source.length)
                break
            }
            builder.append(source, index, match.range.first)
            var depth = 1
            var cursor = match.range.last + 1
            while (cursor < source.length && depth > 0) {
                when (source[cursor]) {
                    '{' -> depth++
                    '}' -> depth--
                }
                cursor++
            }
            index = cursor
        }
        return builder.toString()
    }

    private fun stripTypeAliasLines(source: String): String =
        source.lineSequence()
            .filterNot { it.trimStart().startsWith("type ") || it.trimStart().startsWith("export type ") }
            .joinToString("\n")
}
