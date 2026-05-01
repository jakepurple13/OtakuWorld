package android.content

import android.content.pm.PackageManager
import java.io.File

abstract class Context {
    abstract val packageName: String
    abstract val dataDir: File

    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        SharedPreferences(File(dataDir, "prefs/$name.properties"))

    open fun getFilesDir(): File = File(dataDir, "files").also { it.mkdirs() }
    open fun getCacheDir(): File = File(dataDir, "cache").also { it.mkdirs() }
    open fun getExternalFilesDir(type: String?): File? = getFilesDir()
    open fun getExternalCacheDir(): File? = getCacheDir()
    open fun getDir(name: String, mode: Int): File = File(dataDir, name).also { it.mkdirs() }
    open fun getDatabasePath(name: String): File = File(dataDir, "databases/$name")
    open fun getSystemService(name: String): Any? = null

    abstract fun getPackageManager(): PackageManager

    companion object {
        const val MODE_PRIVATE = 0
    }
}
