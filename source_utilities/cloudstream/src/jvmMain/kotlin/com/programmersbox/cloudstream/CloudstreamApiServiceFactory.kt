package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin

/** Desktop: plugin settings screens are Android UI, so every API is a plain [CloudstreamApiService]. */
@Suppress("UNUSED_PARAMETER")
internal fun cloudstreamApiService(
    instance: BasePlugin,
    api: MainAPI,
    internalName: String,
    serviceName: String,
): CloudstreamApiService = CloudstreamApiService(api, internalName, serviceName)
