package com.programmersbox.uiviews.utils

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
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
        val novel_file: String?,
        val animetv_file: String?
    ) {
        fun downloadUrl(url: AppUpdates.() -> String?) = "$update_url${url()}"
    }
}

@GlideModule
class OtakuGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //super.applyOptions(context, builder)
        builder.setLogLevel(Log.ERROR)
    }
}