package com.programmersbox.novel_sources

import com.programmersbox.models.ApiService
import com.programmersbox.novel_sources.novels.WuxiaWorld
import org.jsoup.Jsoup

enum class Sources(
    val source: ApiService
) : ApiService by source {

    WUXIAWORLD(WuxiaWorld);

    override val serviceName: String get() = this.name

}

internal fun String.toJsoup() = Jsoup.connect(this).get()
