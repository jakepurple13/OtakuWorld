package com.programmersbox.anime.shared.videoplayer

import okhttp3.HttpUrl
import okhttp3.Request

/** A GET for [url] carrying [headers]: the request shape used for every upstream fetch of a source's stream. */
fun upstreamRequest(url: HttpUrl, headers: Map<String, String>): Request.Builder =
    Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }
