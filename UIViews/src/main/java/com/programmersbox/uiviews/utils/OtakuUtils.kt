package com.programmersbox.uiviews.utils

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.utils.Cached
import kotlinx.coroutines.flow.flow

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

val otakuWorldGithubUrl get() = "https://github.com/jakepurple13/OtakuWorld/"

var currentDetailsUrl = otakuWorldGithubUrl

fun SourceRepository.loadItem(
    source: String,
    url: String,
) = toSourceByApiServiceName(source)
    ?.apiService
    ?.let { apiSource ->
        Cached.cache[url]?.let {
            flow {
                emit(
                    it
                        .toDbModel()
                        .toItemModel(apiSource)
                )
            }
        } ?: apiSource.getSourceByUrlFlow(url)
    }
    ?.dispatchIo()