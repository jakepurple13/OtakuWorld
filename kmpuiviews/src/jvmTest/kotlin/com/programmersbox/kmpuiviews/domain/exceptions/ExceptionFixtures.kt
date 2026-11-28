package com.programmersbox.kmpuiviews.domain.exceptions

/** Stack traces in the shape `Throwable.stackTraceToString()` produces, taken from the approved mockup. */
object ExceptionFixtures {
    val mangaDexTimeout = """
        java.net.SocketTimeoutException: timeout
        	at eu.kanade.tachiyomi.extension.en.mangadex.MangaDex.fetchChapterList(MangaDex.kt:212)
        	at com.programmersbox.mihon.MihonSourceAdapter.chapterInfo(MihonSourceAdapter.kt:88)
        	at okhttp3.internal.connection.RealCall.execute(RealCall.kt:154)
        	at okio.SocketAsyncTimeout.newTimeoutException(JvmOkio.kt:147)
        	at java.net.SocketInputStream.read(SocketInputStream.java:183)
    """.trimIndent()

    val asura403 = """
        org.jsoup.HttpStatusException: HTTP error fetching URL. Status=403, URL=[https://asuracomic.net/series/abc]
        	at eu.kanade.tachiyomi.extension.en.asurascans.AsuraScans.popularMangaParse(AsuraScans.kt:97)
        	at org.jsoup.helper.HttpConnection${'$'}Response.execute(HttpConnection.java:912)
        	at org.jsoup.helper.HttpConnection.execute(HttpConnection.java:373)
    """.trimIndent()

    val comickJson = """
        kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 0: Expected start of the object '{', but had '<' instead at path: ${'$'}
        JSON input: <!DOCTYPE html><html lang="en"><head><title>Just a moment...
        	at eu.kanade.tachiyomi.extension.all.comickfun.Comick.searchMangaParse(Comick.kt:301)
        	at kotlinx.serialization.json.internal.AbstractJsonLexer.fail(AbstractJsonLexer.kt:598)
        	at kotlinx.serialization.json.internal.StreamingJsonDecoder.beginStructure(StreamingJsonDecoder.kt:44)
    """.trimIndent()

    val weebCentral = """
        java.lang.IllegalStateException: Expected a chapter list but the response had 0 entries
        	at eu.kanade.tachiyomi.extension.en.weebcentral.WeebCentral.chapterListParse(WeebCentral.kt:155)
        	at com.programmersbox.mihon.MihonSourceAdapter.chapterInfo(MihonSourceAdapter.kt:91)
        	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
        Caused by: java.util.NoSuchElementException: Collection is empty.
        	at kotlin.collections.CollectionsKt.first(Collections.kt:200)
        	... 3 more
    """.trimIndent()

    val unknownHost = """
        java.net.UnknownHostException: Unable to resolve host "api.mangadex.org": No address associated with hostname
        	at java.net.Inet6AddressImpl.lookupAllHostAddr(Native Method)
        	at okhttp3.Dns${'$'}Companion${'$'}DnsSystem.lookup(Dns.kt:49)
        	at eu.kanade.tachiyomi.extension.en.mangadex.MangaDex.latestUpdatesRequest(MangaDex.kt:140)
    """.trimIndent()

    val backupFailure = """
        java.io.FileNotFoundException: /backup.zip: open failed: ENOENT (No such file or directory)
        	at com.programmersbox.kmpuiviews.utils.Zipper.unzip(Zipper.kt:80)
        	at com.programmersbox.kmpuiviews.utils.Backup.restore(Backup.kt:54)
    """.trimIndent()
}
