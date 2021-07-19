package com.programmersbox.animeworldtv

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.programmersbox.gsonutils.sharedPrefObjectDelegate

var Context.currentService: String? by sharedPrefObjectDelegate(null)

@GlideModule
class AnimeWorldTvGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //super.applyOptions(context, builder)
        builder.setLogLevel(Log.ERROR)
    }
}
