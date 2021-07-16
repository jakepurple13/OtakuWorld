package com.programmersbox.sharedutils

import com.programmersbox.gsonutils.getJsonApi
import io.reactivex.subjects.BehaviorSubject

object AppUpdate {
    private const val url = "https://raw.githubusercontent.com/jakepurple13/OtakuWorld/master/update.json"
    fun getUpdate() = getJsonApi<AppUpdates>(url)
    data class AppUpdates(
        val update_version: Double?,
        val update_url: String?,
        val manga_file: String?,
        val anime_file: String?,
        val novel_file: String?,
        val animetv_file: String?
    ) {
        fun downloadUrl(url: AppUpdates.() -> String?) = "$update_url${url()}"
    }
}

val appUpdateCheck = BehaviorSubject.create<AppUpdate.AppUpdates>()