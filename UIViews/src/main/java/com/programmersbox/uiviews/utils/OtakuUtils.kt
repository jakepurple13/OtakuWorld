package com.programmersbox.uiviews.utils

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

fun tryThis(block: () -> Unit) = try {
    block()
} catch (e: Exception) {
    e.printStackTrace()
}

@GlideModule
class OtakuGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //super.applyOptions(context, builder)
        builder.setLogLevel(Log.ERROR)
    }
}