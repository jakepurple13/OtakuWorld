package android.content

import android.content.pm.PackageManager
import com.programmersbox.kmpextensionloader.FileSharedPreferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class Context {
    abstract val packageName: String
    abstract val dataDir: File

    // One instance per file even when extensions ask concurrently.
    private val prefsCache = ConcurrentHashMap<String, SharedPreferences>()

    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        prefsCache.computeIfAbsent(name) {
            FileSharedPreferences(File(dataDir, "prefs/$name.properties"))
        }

    open fun getFilesDir(): File = File(dataDir, "files").also { it.mkdirs() }
    open fun getCacheDir(): File = File(dataDir, "cache").also { it.mkdirs() }
    open fun getExternalFilesDir(type: String?): File? = getFilesDir()
    open fun getExternalCacheDir(): File? = getCacheDir()
    open fun getDir(name: String, mode: Int): File = File(dataDir, name).also { it.mkdirs() }
    open fun getDatabasePath(name: String): File =
        File(dataDir, "databases/$name").also { it.parentFile?.mkdirs() }
    open fun getSystemService(name: String): Any? = null

    /** Desktop has no activities; Android throws the same when nothing handles [intent]. */
    open fun startActivity(intent: Intent): Unit = throw ActivityNotFoundException("No activity on desktop for $intent")

    abstract fun getPackageManager(): PackageManager

    companion object {
        const val MODE_PRIVATE = 0
    }
}
