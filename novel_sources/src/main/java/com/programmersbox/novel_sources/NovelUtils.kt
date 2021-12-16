package com.programmersbox.novel_sources

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.util.concurrent.TimeUnit

fun postApi(url: String, block: HttpUrl.Builder.() -> Unit = {}): ApiResponse = apiAccess(url, block) {
    post(object : RequestBody() {
        override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {}
    })
}

fun postApiMethod(
    url: String,
    block: HttpUrl.Builder.() -> Unit = {},
    method: Request.Builder.() -> Request.Builder
): ApiResponse = apiAccess(url, block) {
    post(object : RequestBody() {
        override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {}
    })
        .method()
}

fun postApi(url: String, built: Request.Builder.() -> Request.Builder = { this }, block: HttpUrl.Builder.() -> Unit = {}): ApiResponse =
    apiAccess(url, block) {
        post(object : RequestBody() {
            override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()
            override fun writeTo(sink: BufferedSink) {}
        })
            .built()
    }

fun postApi(url: String, requestBody: RequestBody): ApiResponse = apiAccess(url, {}) { post(requestBody) }

private fun apiAccess(url: String, block: HttpUrl.Builder.() -> Unit, method: Request.Builder.() -> Request.Builder): ApiResponse {
    val request = Request.Builder()
        .url(url.toHttpUrlOrNull()!!.newBuilder().apply(block).build())
        .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
        .method()
        .build()
    val client = OkHttpClient().newCall(request).execute()
    return if (client.code == 200) ApiResponse.Success(client.body!!.string()) else ApiResponse.Failed(client.code)
}

sealed class ApiResponse {
    data class Success(val body: String) : ApiResponse()
    data class Failed(val code: Int) : ApiResponse()
}