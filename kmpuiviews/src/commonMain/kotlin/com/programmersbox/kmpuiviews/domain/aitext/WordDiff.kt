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

    /** Above this many LCS table cells the changed middle becomes one deletion plus one insertion. */
    const val MAX_LCS_CELLS = 4_000_000L

    /**
     * LCS over whitespace-split tokens, after trimming the common prefix and suffix. In every
     * changed run the deletions come before the insertions.
     */
    fun diff(old: String, new: String): List<DiffSpan> {
        val a = token.findAll(old).map { it.value }.toList()
        val b = token.findAll(new).map { it.value }.toList()

        var prefix = 0
        while (prefix < a.size && prefix < b.size && a[prefix] == b[prefix]) prefix++
        var suffix = 0
        while (
            suffix < a.size - prefix && suffix < b.size - prefix &&
            a[a.size - 1 - suffix] == b[b.size - 1 - suffix]
        ) suffix++

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

        for (k in 0 until prefix) same.append(a[k])

        val midA = a.subList(prefix, a.size - suffix)
        val midB = b.subList(prefix, b.size - suffix)
        if ((midA.size + 1).toLong() * (midB.size + 1) > MAX_LCS_CELLS) {
            flushSame()
            midA.forEach { deleted.append(it) }
            midB.forEach { inserted.append(it) }
        } else {
            diffMiddle(midA, midB, same, deleted, inserted, ::flushSame, ::flushChange)
        }

        if (suffix > 0) {
            flushChange()
            for (k in a.size - suffix until a.size) same.append(a[k])
        }
        flushChange()
        flushSame()
        return spans
    }

    private fun diffMiddle(
        a: List<String>,
        b: List<String>,
        same: StringBuilder,
        deleted: StringBuilder,
        inserted: StringBuilder,
        flushSame: () -> Unit,
        flushChange: () -> Unit,
    ) {
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
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
    }
}
