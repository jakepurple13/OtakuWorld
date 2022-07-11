package com.programmersbox.sharedutils

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
        val otakumanager_file: String?,
        val manga_no_firebase_file: String?,
        val anime_no_firebase_file: String?,
        val novel_no_firebase_file: String?,
        val animetv_no_firebase_file: String?,
    ) {
        fun downloadUrl(url: AppUpdates.() -> String?) = "$update_url${url()}"
    }

    fun checkForUpdate(oldVersion: String, newVersion: String): Boolean = try {
        val items = oldVersion.split(".").zip(newVersion.split("."))
        val major = items[0]
        val minor = items[1]
        val patch = items[2]
        /*
         new major > old major
         new major == old major && new minor > old minor
         new major == old major && new minor == old minor && new patch > old patch
         else false
         */
        when {
            major.second.toInt() > major.first.toInt() -> true
            major.second.toInt() == major.first.toInt() && minor.second.toInt() > minor.first.toInt() -> true
            major.second.toInt() == major.first.toInt()
                    && minor.second.toInt() == minor.first.toInt()
                    && patch.second.toInt() > patch.first.toInt() -> true
            else -> false
        }
    } catch (e: Exception) {
        false
    }
}

val appUpdateCheck = BehaviorSubject.create<AppUpdate.AppUpdates>()