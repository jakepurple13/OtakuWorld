package com.programmersbox.cloudstream.plugins

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.utils.ExtractorApi

data class LoadedPlugin(
    val plugin: InstalledPlugin,
    val instance: BasePlugin,
    val apis: List<MainAPI>,
    val extractors: List<ExtractorApi>,
)
