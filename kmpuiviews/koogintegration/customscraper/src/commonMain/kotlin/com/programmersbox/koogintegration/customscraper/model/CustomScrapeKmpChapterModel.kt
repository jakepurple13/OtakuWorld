package com.programmersbox.koogintegration.customscraper.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomScrapeKmpChapterModel(
    val urls: List<String>,
)
