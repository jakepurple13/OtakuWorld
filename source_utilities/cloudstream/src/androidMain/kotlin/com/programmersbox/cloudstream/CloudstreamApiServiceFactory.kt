package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin

/** Android: a plugin with a settings screen is exposed as [ConfigurableCloudstreamApiService]. */
internal fun cloudstreamApiService(
    instance: BasePlugin,
    api: MainAPI,
    internalName: String,
    serviceName: String,
): CloudstreamApiService {
    val openSettings = (instance as? Plugin)?.openSettings
    return if (openSettings != null) {
        ConfigurableCloudstreamApiService(api, internalName, serviceName, openSettings)
    } else {
        CloudstreamApiService(api, internalName, serviceName)
    }
}
