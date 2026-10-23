// Vendored from mihonapp/mihon@446f6df8 (Apache-2.0). Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.network

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
