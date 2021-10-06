package com.programmersbox.sharedutils

import androidx.compose.ui.util.fastAny
import com.programmersbox.gsonutils.getJsonApi
import io.reactivex.subjects.BehaviorSubject

object AppUpdate {
    private const val url = "https://raw.githubusercontent.com/jakepurple13/OtakuWorld/master/update.json"
    fun getUpdate() = getJsonApi<AppUpdates>(url)
    data class AppUpdates(
        val update_version: Double?,
        val update_real_version: String?,
        val update_url: String?,
        val manga_file: String?,
        val anime_file: String?,
        val novel_file: String?,
        val animetv_file: String?,
        val otakumanager_file: String?
    ) {
        fun downloadUrl(url: AppUpdates.() -> String?) = "$update_url${url()}"
    }

    fun checkForUpdate(oldVersion: String, newVersion: String) = oldVersion.split(".").zip(newVersion.split("."))
        .fastAny {
            try {
                it.second.toInt() > it.first.toInt()
            } catch (e: NumberFormatException) {
                false
            }
        }
    //TODO: change to check
    // new major > old major
    // new major == old major && new minor > old minor
    // new major == old major && new minor == old minor && new patch > old patch
    // else false
}

val appUpdateCheck = BehaviorSubject.create<AppUpdate.AppUpdates>()