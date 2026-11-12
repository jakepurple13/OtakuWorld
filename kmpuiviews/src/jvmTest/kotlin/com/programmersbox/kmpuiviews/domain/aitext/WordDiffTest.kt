package com.programmersbox.kmpuiviews.domain.aitext

import kotlin.test.Test
import kotlin.test.assertEquals

class WordDiffTest {
    @Test
    fun `identical text is all Same`() {
        assertEquals(listOf(DiffSpan.Same("a b  c\n")), WordDiff.diff("a b  c\n", "a b  c\n"))
    }

    @Test
    fun `fixed typos produce deleted then inserted pairs`() {
        assertEquals(
            listOf(DiffSpan.Deleted("Stoped"), DiffSpan.Inserted("Stopped"), DiffSpan.Same(" at ch 45")),
            WordDiff.diff("Stoped at ch 45", "Stopped at ch 45"),
        )
    }

    @Test
    fun `round trip holds for random sentences`() {
        val pairs = listOf(
            "" to "",
            "" to "hello",
            "hello" to "",
            "a b c" to "a c",
            "a c" to "a b c",
            "the quick brown fox" to "the slow brown dog",
            "  leading space" to "leading space",
            "trailing space " to "trailing space",
            "line one\nline two" to "line one line two",
            "i has a apple" to "I have an apple",
            "teh cat sat" to "the cat sat down",
            "one two three four five" to "five four three two one",
            "same" to "same same",
            "a a a a" to "a a",
            "Dont forget" to "Don't forget!",
            "x" to "y",
            "ch 1 done\nch 2 todo" to "ch 1 done\nch 2 done\nch 3 todo",
            "tabs\tand  spaces" to "tabs and spaces",
            "alpha beta gamma delta" to "alpha gamma",
            "ok" to "not ok at all",
        )
        assertEquals(20, pairs.size)
        pairs.forEach { (old, new) ->
            val spans = WordDiff.diff(old, new)
            assertEquals(new, spans.filter { it !is DiffSpan.Deleted }.joinToString("") { it.text }, "new: $old -> $new")
            assertEquals(old, spans.filter { it !is DiffSpan.Inserted }.joinToString("") { it.text }, "old: $old -> $new")
        }
    }
}
