package com.programmersbox.anime.shared

import com.programmersbox.kmpmodels.KmpStorage

class StorageHolder {
    var storageModel: KmpStorage? = null

    /** Show title and poster of [storageModel]'s episode, for casting from the player. */
    var showTitle: String? = null
    var posterUrl: String? = null
}
