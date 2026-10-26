package com.programmersbox.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.MainAPI
import com.programmersbox.kmpmodels.KmpConfigurableApiService

/** Android only: plugin settings open an Android settings UI. Desktop never exposes this type. */
class ConfigurableCloudstreamApiService(
    api: MainAPI,
    internalName: String,
    serviceName: String,
    val openSettings: (Context) -> Unit,
) : CloudstreamApiService(api, internalName, serviceName), KmpConfigurableApiService
