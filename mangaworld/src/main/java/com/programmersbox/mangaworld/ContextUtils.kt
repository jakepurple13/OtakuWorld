package com.programmersbox.mangaworld

import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior

val DOWNLOAD_FILE_PATH
    get() = Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/MangaWorld/"

/*val SHOW_ADULT = booleanPreferencesKey("showAdultSources")
val Context.showAdultFlow get() = otakuDataStore.data.map { it[SHOW_ADULT] ?: false }

val FOLDER_LOCATION = stringPreferencesKey("folderLocation")
val Context.folderLocationFlow get() = otakuDataStore.data.map { it[FOLDER_LOCATION] ?: DOWNLOAD_FILE_PATH }

var Context.folderLocation: String by sharedPrefNotNullDelegate(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/MangaWorld/"
)*/

class CustomHideBottomViewOnScrollBehavior<T : View>(context: Context?, attrs: AttributeSet?) :
    HideBottomViewOnScrollBehavior<T>(context, attrs) {

    var isShowing = true
        private set

    override fun slideDown(child: T) {
        super.slideDown(child)
        isShowing = false
    }

    override fun slideUp(child: T) {
        super.slideUp(child)
        isShowing = true
    }

}