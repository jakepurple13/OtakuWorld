package com.programmersbox.favoritesdatabase

// Letters, digits and combining marks, matching what unicode61 keeps inside a token
private val ftsToken = Regex("[\\p{L}\\p{N}\\p{M}]+")

/**
 * Turns raw user input into an FTS5 MATCH expression where every word is a quoted prefix
 * query and all words must match. Only letter/digit runs survive, so FTS5 syntax typed by
 * the user (quotes, `*`, `-`, `:`, parentheses) can never produce an invalid query.
 *
 * @param column restricts each phrase to that FTS column when set.
 * @return null when the input contains no searchable words.
 */
fun ftsMatch(raw: String, column: String? = null): String? {
    val tokens = ftsToken.findAll(raw).map { it.value }.toList()
    if (tokens.isEmpty()) return null
    val prefix = column?.let { "$it : " }.orEmpty()
    return tokens.joinToString(" ") { "$prefix\"$it\"*" }
}
