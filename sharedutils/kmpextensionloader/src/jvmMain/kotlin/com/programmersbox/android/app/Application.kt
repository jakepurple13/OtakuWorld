package android.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import java.io.File

open class Application(
    override val packageName: String,
    baseDataDir: File,
    val apkPath: String = "",
) : Context() {

    override val dataDir: File = File(baseDataDir, packageName).also { it.mkdirs() }

    private inner class MockPackageManager : PackageManager() {
        override fun getPackageInfo(packageName: String, flags: Int): PackageInfo =
            PackageInfo().apply { this.packageName = packageName }

        override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo =
            ApplicationInfo().apply {
                this.packageName = packageName
                this.sourceDir = if (packageName == this@Application.packageName) apkPath else ""
            }
    }

    private val pm = MockPackageManager()
    override fun getPackageManager(): PackageManager = pm

    fun getApplicationInfo(): ApplicationInfo = ApplicationInfo().apply {
        this.packageName = this@Application.packageName
        this.sourceDir = apkPath
        this.metaData = Bundle()
    }
}
