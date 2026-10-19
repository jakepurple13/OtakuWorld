package com.programmersbox.aniyomi

import eu.kanade.tachiyomi.animesource.model.HttpServer
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource

/** Local http servers that some extensions (e.g. AnimePahe) stream through. One per source, kept for the process. */
object AniyomiHttpServers {
    private val servers = mutableMapOf<Long, HttpServer>()

    @Synchronized
    fun portFor(source: AnimeHttpSource): Int {
        servers[source.id]?.takeIf { it.isRunning() }?.let { return it.listeningPort }
        val server = source.createHttpServer() ?: error("${source.name} uses a local http server but did not create one")
        server.start()
        check(server.isRunning()) { "${source.name} http server failed to start" }
        servers[source.id] = server
        return server.listeningPort
    }
}
