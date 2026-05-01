package android.content.pm

abstract class PackageManager {
    companion object {
        const val GET_META_DATA = 0x00000080
        const val GET_CONFIGURATIONS = 0x00004000
        const val GET_SIGNING_CERTIFICATES = 0x08000000
    }

    abstract fun getPackageInfo(packageName: String, flags: Int): PackageInfo
    abstract fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo
}
