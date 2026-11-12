package com.programmersbox.navgraph

/**
 * A Kotlin file plus a [code] view of it where comments and string/char literals are blanked out
 * (same length, newlines kept), so regexes and brace matching only see real code.
 */
class KotlinSource(val path: String, val text: String) {

    val fileName: String get() = path.substringAfterLast('/')

    val code: String = blankCommentsAndStrings(text)

    val packageName: String = Regex("""^\s*package\s+([\w.]+)""", RegexOption.MULTILINE)
        .find(code)?.groupValues?.get(1).orEmpty()

    private val lineStarts: IntArray = buildList {
        add(0)
        text.forEachIndexed { i, c -> if (c == '\n') add(i + 1) }
    }.toIntArray()

    /** 1-based line number of [offset]. */
    fun lineOf(offset: Int): Int {
        val idx = lineStarts.binarySearch(offset)
        return if (idx >= 0) idx + 1 else -idx - 1
    }

    /** Offset of the `}` matching the `{` at [openOffset] in [code], or -1. */
    fun matchingBrace(openOffset: Int): Int = matchingClose(code, openOffset, '{', '}')

    /** Offset of the `)` matching the `(` at [openOffset] in [code], or -1. */
    fun matchingParen(openOffset: Int): Int = matchingClose(code, openOffset, '(', ')')

    companion object {
        fun matchingClose(code: String, openOffset: Int, open: Char, close: Char): Int {
            if (openOffset !in code.indices || code[openOffset] != open) return -1
            var depth = 0
            for (i in openOffset until code.length) {
                when (code[i]) {
                    open -> depth++
                    close -> if (--depth == 0) return i
                }
            }
            return -1
        }

        private fun blankCommentsAndStrings(text: String): String {
            val out = StringBuilder(text)
            fun blank(from: Int, to: Int) {
                for (i in from until minOf(to, text.length)) if (text[i] != '\n') out[i] = ' '
            }

            var i = 0
            while (i < text.length) {
                val c = text[i]
                when {
                    text.startsWith("//", i) -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                        blank(i, end); i = end
                    }

                    text.startsWith("/*", i) -> {
                        var depth = 0
                        var j = i
                        while (j < text.length) {
                            if (text.startsWith("/*", j)) { depth++; j += 2; continue }
                            if (text.startsWith("*/", j)) { depth--; j += 2; if (depth == 0) break; continue }
                            j++
                        }
                        blank(i, j); i = j
                    }

                    text.startsWith("\"\"\"", i) -> {
                        val close = text.indexOf("\"\"\"", i + 3).let { if (it < 0) text.length else it + 3 }
                        // A raw string may end with extra quotes ("""a"""") — swallow them.
                        var end = close
                        while (end < text.length && text[end] == '"') end++
                        blank(i, end); i = end
                    }

                    c == '"' || c == '\'' -> {
                        var j = i + 1
                        while (j < text.length && text[j] != c && text[j] != '\n') {
                            if (text[j] == '\\') j++
                            j++
                        }
                        blank(i, j + 1); i = j + 1
                    }

                    else -> i++
                }
            }
            return out.toString()
        }
    }
}
