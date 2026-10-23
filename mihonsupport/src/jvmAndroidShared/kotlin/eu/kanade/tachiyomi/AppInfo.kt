// OTAKU: hand-written implementation of the extensions-lib AppInfo surface.
package eu.kanade.tachiyomi

import android.app.Application
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Suppress("UNUSED")
object AppInfo {
    fun getVersionCode(): Int = packageInfo().longVersionCode.toInt()

    fun getVersionName(): String = packageInfo().versionName.orEmpty()

    /** @since extensions-lib 1.5 */
    fun getSupportedImageMimeTypes(): List<String> =
        listOf("image/jpeg", "image/png", "image/gif", "image/webp")

    // OTAKU: getter call instead of property syntax so the JVM stub Application (Kotlin fun) compiles too.
    private fun packageInfo() = Injekt.get<Application>().let { it.getPackageManager().getPackageInfo(it.packageName, 0) }
}
