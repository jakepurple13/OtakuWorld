# Navigation keys keep their class names so analytics screen names stay readable in minified builds
-keepnames class * implements androidx.navigation3.runtime.NavKey
# Exception class names are sent as analytics error types (sync_failed, source_error)
-keepnames class * extends java.lang.Throwable
