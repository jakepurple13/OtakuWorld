package android.content.pm

import android.graphics.drawable.Drawable

abstract class PackageManager {
    abstract fun getPackageInfo(packageName: String, flags: Int): PackageInfo
    open fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo = ApplicationInfo()
    open fun getApplicationIcon(packageName: String): Drawable? = null
    open fun getApplicationIcon(info: ApplicationInfo): Drawable? = null
    open fun getInstalledPackages(flags: Int): List<PackageInfo> = emptyList()

    companion object {
        const val GET_META_DATA = 0x00000080
        const val GET_CONFIGURATIONS = 0x00004000
        const val GET_SIGNING_CERTIFICATES = 0x08000000
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
    }
}
