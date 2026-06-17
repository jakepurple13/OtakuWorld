package com.programmersbox.koogintegration.customscraper.database

import kotlinx.coroutines.flow.Flow

class CustomScraperDataStore(
    val getModelName: suspend () -> String,
    val storeModelName: suspend (String) -> Unit,
    val modelNameFlow: Flow<String>,
)