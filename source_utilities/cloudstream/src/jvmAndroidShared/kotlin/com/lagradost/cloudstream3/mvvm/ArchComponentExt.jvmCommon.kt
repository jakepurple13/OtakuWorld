// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: `actual` modifiers removed; the commonMain `expect` is not vendored.
package com.lagradost.cloudstream3.mvvm

import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.reflect.full.NoSuchPropertyException

fun <T> platformThrowAbleToResource(throwable: Throwable): Resource<T> {
    return when (throwable) {
        is NoSuchMethodException, is NoSuchFieldException, is NoSuchMethodError, is NoSuchFieldError, is NoSuchPropertyException -> {
            Resource.Failure(
                false,
                "App or extension is outdated, update the app or try pre-release.\n${throwable.message}" // todo add exact version?
            )
        }
        is SocketTimeoutException, is InterruptedIOException -> {
            Resource.Failure(
                true,
                "Connection Timeout\nPlease try again later."
            )
        }
        is UnknownHostException -> {
            Resource.Failure(
                true,
                "Cannot connect to server, try again later.\n${throwable.message}"
            )
        }
        is SSLHandshakeException -> {
            Resource.Failure(
                true,
                (throwable.message ?: "SSLHandshakeException") + "\nTry a VPN or DNS."
            )
        }
        else -> safeFail(throwable)
    }
}
