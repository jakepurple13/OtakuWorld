package android.net

import java.net.URLDecoder

abstract class Uri {
    abstract val scheme: String?
    abstract val host: String?
    abstract val path: String?
    abstract val lastPathSegment: String?
    abstract fun getQueryParameter(key: String): String?
    abstract override fun toString(): String

    companion object {
        /** Never throws, like Android: hand-splits instead of using java.net.URI, which rejects spaces, `|`, `_` hosts. */
        @JvmStatic
        fun parse(uriString: String): Uri = LenientUri(uriString)
    }
}

private class LenientUri(private val original: String) : Uri() {
    private val withoutFragment = original.substringBefore('#')
    private val schemeEnd = withoutFragment.indexOf(':').takeIf { idx ->
        idx > 0 && withoutFragment.substring(0, idx).all { it.isLetterOrDigit() || it in "+-." }
    } ?: -1
    private val rest = withoutFragment.substring(schemeEnd + 1)
    private val hasAuthority = rest.startsWith("//")
    private val afterAuthority = if (hasAuthority) rest.substring(2) else rest
    private val authorityEnd = if (hasAuthority) afterAuthority.indexOfFirst { it == '/' || it == '?' }.let { if (it < 0) afterAuthority.length else it } else 0
    private val authority = if (hasAuthority) afterAuthority.substring(0, authorityEnd) else null
    private val pathAndQuery = if (hasAuthority) afterAuthority.substring(authorityEnd) else rest
    private val rawPath = pathAndQuery.substringBefore('?')
    private val rawQuery = if ('?' in pathAndQuery) pathAndQuery.substringAfter('?') else null

    override val scheme: String? get() = if (schemeEnd > 0) withoutFragment.substring(0, schemeEnd) else null

    override val host: String?
        get() = authority
            ?.substringAfterLast('@')
            ?.let { if (it.startsWith("[")) it.substringBefore(']') + "]" else it.substringBefore(':') }
            ?.takeIf { it.isNotEmpty() }

    override val path: String? get() = decode(rawPath)

    override val lastPathSegment: String?
        get() = rawPath.trimEnd('/').substringAfterLast('/').takeIf { it.isNotEmpty() }?.let(::decode)

    override fun getQueryParameter(key: String): String? =
        rawQuery?.split("&")
            ?.map { it.substringBefore("=") to it.substringAfter("=", "") }
            ?.firstOrNull { decode(it.first, true) == key }
            ?.let { decode(it.second, true) }

    private fun decode(s: String, plusAsSpace: Boolean = false): String =
        runCatching { URLDecoder.decode(if (plusAsSpace) s else s.replace("+", "%2B"), "UTF-8") }.getOrDefault(s)

    override fun toString(): String = original
}
