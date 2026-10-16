package com.programmersbox.cloudstream.plugins

import com.programmersbox.cloudstream.repo.CsPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.util.concurrent.ConcurrentHashMap

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
    private val nameLocks = ConcurrentHashMap<String, Mutex>()

    private fun nameLock(name: String) = nameLocks.getOrPut(name) { Mutex() }

    suspend fun install(repoUrl: String, plugin: CsPlugin): InstalledPlugin = withContext(Dispatchers.IO) {
        val name = plugin.internalName
        // A hostile plugins.json must not be able to write outside root.
        require(SAFE_NAME.matches(name) && !name.startsWith(".")) { "Unsafe internalName: $name" }
        nameLock(name).withLock { installLocked(repoUrl, plugin) }
    }

    private fun installLocked(repoUrl: String, plugin: CsPlugin): InstalledPlugin {
        val name = plugin.internalName
        val dir = File(root, hex(sha256(repoUrl.toByteArray())).take(16)).also { it.mkdirs() }
        val target = File(dir, "$name.cs3")
        val temp = File.createTempFile(name, ".tmp", dir)
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
        return record
    }

    fun uninstall(internalName: String) = runBlocking {
        nameLock(internalName).withLock {
            synchronized(lock) {
                val existing = read()
                existing.filter { it.internalName == internalName }.forEach { deleteFile(File(it.path)) }
                write(existing.filterNot { it.internalName == internalName })
            }
        }
    }

    fun installed(): List<InstalledPlugin> = synchronized(lock) { read() }

    private fun deleteFile(file: File) {
        file.setWritable(true)
        file.delete()
    }

    private fun read(): List<InstalledPlugin> {
        if (!indexFile.exists()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(InstalledPlugin.serializer()), indexFile.readText())
        } catch (e: Exception) {
            println("PluginStore: corrupt installed.json (${e.message}); rebuilding from disk")
            indexFile.renameTo(File(root, "installed.json.corrupt-${System.currentTimeMillis()}"))
            rebuild().also { write(it) }
        }
    }

    // Repo url is unknown and version 0, so the next repo refresh offers an update.
    private fun rebuild(): List<InstalledPlugin> =
        root.listFiles { f -> f.isDirectory }.orEmpty()
            .flatMap { dir -> dir.listFiles { f -> f.isFile && f.name.endsWith(".cs3") }.orEmpty().toList() }
            .distinctBy { it.name }
            .map { InstalledPlugin("", it.name.removeSuffix(".cs3"), 0, it.absolutePath) }

    private fun write(list: List<InstalledPlugin>) {
        root.mkdirs()
        val tmp = File(root, "installed.json.tmp")
        tmp.writeText(json.encodeToString(ListSerializer(InstalledPlugin.serializer()), list))
        Files.move(tmp.toPath(), indexFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private companion object {
        val SAFE_NAME = Regex("[A-Za-z0-9._-]+")
    }
}
