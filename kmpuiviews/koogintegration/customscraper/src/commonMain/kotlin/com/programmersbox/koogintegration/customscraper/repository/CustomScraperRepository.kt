package com.programmersbox.koogintegration.customscraper.repository

import com.programmersbox.koogintegration.customscraper.database.ScraperDao
import com.programmersbox.koogintegration.customscraper.database.ScraperSource
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpInfoModel
import com.programmersbox.koogintegration.customscraper.scraper.WebScraper

class CustomScraperRepository(
    private val scraperDao: ScraperDao,
    private val webScraper: WebScraper,
) {
    suspend fun scrapeDetails(url: String) = webScraper.scrapeDetails(url)
    suspend fun scrapeChapter(url: String) = webScraper.scrapeChapter(url)

    suspend fun saveScrapedDetails(
        url: String,
        info: CustomScrapeKmpInfoModel,
    ) {
        scraperDao.insertScraper(
            ScraperSource(
                url = url,
                name = info.title,
                description = info.description,
                coverImageUrl = info.imageUrl
            )
        )
    }

    fun getScrapedDetails(url: String) = scraperDao.getScraper(url)
    fun getAllScrapedDetails() = scraperDao.getAllScrapers()
    suspend fun updateScraper(scraperSource: ScraperSource) = scraperDao.updateScraper(scraperSource)
    fun getScraperCount() = scraperDao.getAllScrapersCount()
    fun searchScrapers(query: String) = scraperDao.searchScrapers(query)
    suspend fun getAllScrapersSync() = scraperDao.getAllScrapersSync()
    fun getAllScrapers() = scraperDao.getAllScrapers()
    suspend fun deleteScraper(scraperSource: ScraperSource) = scraperDao.deleteScraper(scraperSource)
}