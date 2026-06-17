package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

// OkHttp engine — cross-platform, works on Desktop JVM without Android runtime.
// No CIO engine is present in the project's version catalog, so OkHttp is used here.
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)

/*
actual fun fetchRenderedHtml(url: String): String {
    // Start Playwright and launch a headless browser
    return Playwright.create().use { playwright ->
        val browser = playwright.chromium().launch()
        val page = browser.newPage()

        // Navigate to the URL
        page.navigate(url)

        // IMPORTANT: Wait for the JavaScript to finish doing its work.
        // You can wait for a specific element that JS generates to appear:
        //page.waitForSelector(".some-dynamic-element-class")
        // Alternatively, wait for network idle: page.waitForLoadState(LoadState.NETWORKIDLE)
        page.waitForLoadState(LoadState.NETWORKIDLE)
        // Extract the fully rendered HTML including JS modifications
        val loadedHtml = page.content()

        browser.close()

        loadedHtml
    }
}*/
