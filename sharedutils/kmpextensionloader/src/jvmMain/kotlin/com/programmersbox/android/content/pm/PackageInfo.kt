package android.content.pm
import android.os.Bundle

class PackageInfo {
    var packageName: String = ""
    var versionName: String? = null
    var reqFeatures: Array<FeatureInfo>? = null
}

class FeatureInfo {
    var name: String? = null
}
