package android.net

import java.net.URI
import java.net.URLDecoder

abstract class Uri {
    abstract val scheme: String?
    abstract val host: String?
    abstract val path: String?
    abstract val lastPathSegment: String?
    abstract fun getQueryParameter(key: String): String?
    abstract override fun toString(): String

    companion object {
        @JvmStatic
        fun parse(uriString: String): Uri = JavaUri(URI(uriString))
    }
}

private class JavaUri(private val uri: URI) : Uri() {
    override val scheme: String? get() = uri.scheme
    override val host: String? get() = uri.host
    override val path: String? get() = uri.path
    override val lastPathSegment: String?
        get() = uri.path?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }

    override fun getQueryParameter(key: String): String? =
        uri.rawQuery?.split("&")
            ?.map { it.substringBefore("=") to it.substringAfter("=", "") }
            ?.firstOrNull { URLDecoder.decode(it.first, "UTF-8") == key }
            ?.let { URLDecoder.decode(it.second, "UTF-8") }

    override fun toString(): String = uri.toString()
}
