package android.content.pm

class PackageInfo {
    var packageName: String = ""
    var versionName: String? = null
    var versionCode: Int = 0
    var reqFeatures: Array<FeatureInfo>? = null
}

class FeatureInfo {
    var name: String? = null
    var flags: Int = 0
}
