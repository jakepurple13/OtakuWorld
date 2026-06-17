package com.programmersbox.koogintegration.customscraper.model

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("The generic scraper result class.")
sealed class ScraperResult

@Serializable
@SerialName("ChapterData")
@LLMDescription("The class that contains all of the media urls scraped from the website.")
data class CustomScrapeKmpChapterModel(
    @property:LLMDescription("The list of media urls scraped from the website.")
    val urls: List<String>,
) : ScraperResult()

@Serializable
@SerialName("InfoData")
@LLMDescription("The class that contains all of the details of the piece of media scraped from the website.")
data class CustomScrapeKmpInfoModel(
    @property:LLMDescription("The title of the piece of media scraped from the website.")
    val title: String,
    @property:LLMDescription("The description of the piece of media scraped from the website.")
    val description: String,
    /*@property:LLMDescription("The url of the piece of media scraped from the website.")
    val url: String,*/
    @property:LLMDescription("The cover image url of the piece of media scraped from the website.")
    val imageUrl: String,
    @property:LLMDescription("The list of chapters scraped from the website.")
    val chapters: List<CustomScrapeChapter>,
    @property:LLMDescription("The list of genres scraped from the website. The default is an empty list")
    val genres: List<String> = emptyList(),
) : ScraperResult()

@Serializable
@LLMDescription("The class that contains a single chapter information scraped from the website.")
data class CustomScrapeChapter(
    @property:LLMDescription("The chapter url scraped from the website.")
    val url: String,
    @property:LLMDescription("The chapter name scraped from the website.")
    val name: String,
)