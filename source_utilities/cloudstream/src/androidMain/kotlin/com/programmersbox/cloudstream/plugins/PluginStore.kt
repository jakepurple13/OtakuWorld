package com.programmersbox.cloudstream.plugins

import com.programmersbox.cloudstream.repo.CsPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Downloads, verifies and records `.cs3` plugins under [root].
 * Layout: `root/<sha256(repoUrl) first 16 hex>/<internalName>.cs3`, plus `root/installed.json`.
 */
class PluginStore(
    private val root: File,
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val indexFile get() = File(root, "installed.json")
    private val lock = Any()

    suspend fun install(repoUrl: String, plugin: CsPlugin): InstalledPlugin = withContext(Dispatchers.IO) {
        val name = plugin.internalName
        // A hostile plugins.json must not be able to write outside root.
        require(SAFE_NAME.matches(name) && !name.startsWith(".")) { "Unsafe internalName: $name" }

        val dir = File(root, hex(sha256(repoUrl.toByteArray())).take(16)).also { it.mkdirs() }
        val target = File(dir, "$name.cs3")
        val temp = File(dir, "$name.cs3.tmp")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            client.newCall(Request.Builder().url(plugin.url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")
                temp.outputStream().use { out ->
                    response.body.byteStream().use { input ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            digest.update(buf, 0, n)
                            out.write(buf, 0, n)
                        }
                    }
                }
            }
            plugin.fileHash?.let { expected ->
                val want = expected.removePrefix("sha256-")
                if (!want.equals(hex(digest.digest()), ignoreCase = true)) {
                    throw IOException("Hash mismatch for $name")
                }
            }
            // An update replaces a read-only file.
            if (target.exists()) target.setWritable(true)
            Files.move(
                temp.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
            target.setReadOnly()
        } finally {
            temp.delete()
        }

        val record = InstalledPlugin(repoUrl, name, plugin.version, target.absolutePath)
        synchronized(lock) {
            val existing = read()
            // Same name from another repo lives in another directory; drop its file.
            existing.firstOrNull { it.internalName == name && it.path != record.path }?.let { deleteFile(File(it.path)) }
            write(existing.filterNot { it.internalName == name } + record)
        }
        record
    }

    fun uninstall(internalName: String) {
        synchronized(lock) {
            val existing = read()
            existing.filter { it.internalName == internalName }.forEach { deleteFile(File(it.path)) }
            write(existing.filterNot { it.internalName == internalName })
        }
    }

    fun installed(): List<InstalledPlugin> = synchronized(lock) { read() }

    private fun deleteFile(file: File) {
        file.setWritable(true)
        file.delete()
    }

    private fun read(): List<InstalledPlugin> = runCatching {
        json.decodeFromString(ListSerializer(InstalledPlugin.serializer()), indexFile.readText())
    }.getOrDefault(emptyList())

    private fun write(list: List<InstalledPlugin>) {
        root.mkdirs()
        indexFile.writeText(json.encodeToString(ListSerializer(InstalledPlugin.serializer()), list))
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private companion object {
        val SAFE_NAME = Regex("[A-Za-z0-9._-]+")
    }
}
