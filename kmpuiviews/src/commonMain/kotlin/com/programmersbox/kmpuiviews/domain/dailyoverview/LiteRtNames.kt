package com.programmersbox.kmpuiviews.domain.dailyoverview

/** "gemma-4-E2B-it.litertlm" -> "Gemma 4 E2B"; "gemma3-1b-it-int4.litertlm" -> "Gemma3 1b It Int4". */
fun liteRtDisplayName(fileName: String): String = fileName
    .removeSuffix(".litertlm")
    .removeSuffix("-it")
    .split('-', '_')
    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
