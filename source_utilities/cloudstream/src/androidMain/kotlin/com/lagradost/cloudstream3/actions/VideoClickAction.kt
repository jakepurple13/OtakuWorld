// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: shim so Plugin.registerVideoClickAction links. Never invoked. Members that need
// UiText, ResultEpisode or LinkLoadingResult (CloudStream UI classes) and VideoClickActionHolder
// are not included.
package com.lagradost.cloudstream3.actions

import com.lagradost.cloudstream3.utils.ExtractorLinkType

abstract class VideoClickAction {
    /** if true, the app will show dialog to select source - result.links[index] */
    open val oneSource : Boolean = false

    /** if true, this action could be selected as default player (one press action) in settings */
    open val isPlayer: Boolean = false

    /** Which type of sources this action can handle. */
    open val sourceTypes: Set<ExtractorLinkType> = ExtractorLinkType.entries.toSet()

    /** Determines which plugin a given provider is from. This is the full path to the plugin. */
    var sourcePlugin: String? = null

    fun uniqueId() = "$sourcePlugin:${this::class.qualifiedName}"
}
