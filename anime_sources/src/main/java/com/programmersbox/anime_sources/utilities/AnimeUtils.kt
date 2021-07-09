package com.programmersbox.anime_sources.utilities

import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun getApi(url: String, block: HttpUrl.Builder.() -> Unit): ApiResponse {
    val request = Request.Builder()
        .url(url.toHttpUrlOrNull()!!.newBuilder().apply(block).build())
        .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
        .build()
    val client = OkHttpClient().newCall(request).execute()
    return if (client.code == 200) ApiResponse.Success(client.body!!.string()) else ApiResponse.Failed(client.code)
}

sealed class ApiResponse {
    data class Success(val body: String) : ApiResponse()
    data class Failed(val code: Int) : ApiResponse()
}