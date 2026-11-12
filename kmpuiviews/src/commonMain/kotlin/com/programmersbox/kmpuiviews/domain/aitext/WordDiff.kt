package com.programmersbox.kmpuiviews.domain.aitext

sealed interface DiffSpan {
    val text: String

    data class Same(override val text: String) : DiffSpan
    data class Inserted(override val text: String) : DiffSpan
    data class Deleted(override val text: String) : DiffSpan
}

object WordDiff {
    // A token is a word with the whitespace before it; trailing whitespace is a token of its own.
    private val token = Regex("""\s*\S+|\s+""")

    /** LCS over whitespace-split tokens. In every changed run the deletions come before the insertions. */
    fun diff(old: String, new: String): List<DiffSpan> {
        val a = token.findAll(old).map { it.value }.toList()
        val b = token.findAll(new).map { it.value }.toList()
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }

        val spans = mutableListOf<DiffSpan>()
        val same = StringBuilder()
        val deleted = StringBuilder()
        val inserted = StringBuilder()

        fun flushChange() {
            if (deleted.isNotEmpty()) spans += DiffSpan.Deleted(deleted.toString())
            if (inserted.isNotEmpty()) spans += DiffSpan.Inserted(inserted.toString())
            deleted.clear()
            inserted.clear()
        }

        fun flushSame() {
            if (same.isNotEmpty()) spans += DiffSpan.Same(same.toString())
            same.clear()
        }

        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            when {
                i < a.size && j < b.size && a[i] == b[j] -> {
                    flushChange()
                    same.append(a[i])
                    i++
                    j++
                }

                j == b.size || (i < a.size && lcs[i + 1][j] >= lcs[i][j + 1]) -> {
                    flushSame()
                    deleted.append(a[i++])
                }

                else -> {
                    flushSame()
                    inserted.append(b[j++])
                }
            }
        }
        flushChange()
        flushSame()
        return spans
    }
}
