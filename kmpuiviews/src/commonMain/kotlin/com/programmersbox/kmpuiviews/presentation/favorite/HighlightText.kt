package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private const val START = '\u0002'
private const val END = '\u0003'

/**
 * Strips FTS5 highlight markers (`\u0002` start, `\u0003` end) and returns the plain text plus
 * the inclusive ranges of the marked words in it.
 */
fun parseHighlight(marked: String): Pair<String, List<IntRange>> {
    val text = StringBuilder(marked.length)
    val ranges = mutableListOf<IntRange>()
    var start = -1
    marked.forEach { c ->
        when (c) {
            START -> start = text.length
            END -> if (start >= 0) {
                ranges += start until text.length
                start = -1
            }
            else -> text.append(c)
        }
    }
    return text.toString() to ranges
}

@Composable
fun highlightedText(marked: String): AnnotatedString {
    val (text, ranges) = parseHighlight(marked)
    val style = SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f))
    return buildAnnotatedString {
        append(text)
        ranges.forEach { addStyle(style, it.first, it.last + 1) }
    }
}
