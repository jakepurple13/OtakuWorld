// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.Prerelease
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter

object StringUtils {
    fun String.decodeUrl(): String {
        return this.decodeURLQueryComponent()
    }

    fun String.encodeUrl(): String {
        return this.encodeURLParameter()
    }

    @Deprecated(
        message = "Use Ktor 'Url' naming convention instead.",
        replaceWith = ReplaceWith("this.encodeUrl()"),
        level = DeprecationLevel.WARNING,
    )
    fun String.encodeUri(): String = encodeUrl()

    @Deprecated(
        message = "Use Ktor 'Url' naming convention instead.",
        replaceWith = ReplaceWith("this.decodeUrl()"),
        level = DeprecationLevel.WARNING,
    )
    fun String.decodeUri(): String = decodeUrl()
}

