# Kotlin serialization used in UIViews models
-keep,includedescriptorclasses class com.programmersbox.uiviews.**$$serializer { *; }
-keepclassmembers class com.programmersbox.uiviews.** {
    *** Companion;
}
-keepclasseswithmembers class com.programmersbox.uiviews.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.programmersbox.uiviews.** { *; }

# Keep Navigation3 screen key classes defined in UIViews
-keep class * implements androidx.navigation3.runtime.NavKey { *; }

# Keep Koin-registered ViewModels in UIViews
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep data binding generated classes
-keep class com.programmersbox.uiviews.databinding.** { *; }
