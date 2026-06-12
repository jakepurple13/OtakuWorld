package com.programmersbox.koogintegration.embedding

import com.programmersbox.favoritesdatabase.DbModel

/**
 * Text representation sent to the embedding model.
 * imageUrl is intentionally excluded; favorites with no description are skipped (null).
 */
fun DbModel.toEmbeddingText(): String? {
    if (description.isBlank()) return null
    return buildString {
        appendLine("Title: $title")
        appendLine("Description: ${description.trim()}")
        appendLine("Source: $source")
        appendLine("Url: $url")
        appendLine("Chapters: $numChapters")
        append("Checks for updates: $shouldCheckForUpdate")
    }
}
