package com.programmersbox.koogintegration

import kotlinx.coroutines.flow.Flow

class KoogDataStore(
    val getApiKey: suspend () -> String,
    val getModelCompany: suspend () -> String,
    val getModelName: suspend () -> String,
    val storeApiKey: suspend (String) -> Unit,
    val storeModelCompany: suspend (String) -> Unit,
    val storeModelName: suspend (String) -> Unit,
    val apiKeyFlow: Flow<String>,
    val modelCompanyFlow: Flow<String>,
    val modelNameFlow: Flow<String>,
)