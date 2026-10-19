package com.programmersbox.anime.shared.videochoice

import com.programmersbox.kmpmodels.KmpStorage

/** Groups by [KmpStorage.server], keeping first-appearance order of servers and original order within each. */
fun List<KmpStorage>.groupedByServer(): List<Pair<String?, List<KmpStorage>>> =
    groupBy { it.server }.toList()

/**
 * Header label for the first item of each group; none when there is only one group. Items with no
 * server are labeled "Other" so they do not read as part of the group above them.
 */
fun List<Pair<String?, List<KmpStorage>>>.serverLabels(): Map<KmpStorage, String> =
    if (size > 1) associate { (server, list) -> list.first() to (server ?: "Other") } else emptyMap()
