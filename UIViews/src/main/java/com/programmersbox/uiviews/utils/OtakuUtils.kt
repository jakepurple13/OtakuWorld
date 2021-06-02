package com.programmersbox.uiviews.utils

import com.programmersbox.gsonutils.getJsonApi

fun tryThis(block: () -> Unit) = try {
    block()
} catch (e: Exception) {
    e.printStackTrace()
}

object AppUpdate {
    private const val url = "https://raw.githubusercontent.com/jakepurple13/OtakuWorld/master/update.json"
    fun getUpdate() = getJsonApi<AppUpdates>(url)
    data class AppUpdates(
        val update_version: Double?,
        val update_url: String?,
        val manga_file: String?,
        val anime_file: String?,
        val novel_file: String?
    ) {
        fun downloadUrl(url: AppUpdates.() -> String?) = "$update_url${url()}"
    }
}

