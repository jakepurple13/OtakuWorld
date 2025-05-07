package com.programmersbox.uiviews.utils

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class OtakuGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //super.applyOptions(context, builder)
        builder.setLogLevel(Log.ERROR)
    }
}

val otakuWorldGithubUrl get() = "https://github.com/jakepurple13/OtakuWorld/"

var currentDetailsUrl = otakuWorldGithubUrl
