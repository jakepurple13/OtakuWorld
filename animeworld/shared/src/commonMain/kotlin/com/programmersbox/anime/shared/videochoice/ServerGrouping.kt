package com.programmersbox.anime.shared.videochoice

import com.programmersbox.kmpmodels.KmpStorage

/** Groups by [KmpStorage.server], keeping first-appearance order of servers and original order within each. */
fun List<KmpStorage>.groupedByServer(): List<Pair<String?, List<KmpStorage>>> =
    groupBy { it.server }.toList()
