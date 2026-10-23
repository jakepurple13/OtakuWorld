package android.preference

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    @JvmStatic
    fun getDefaultSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
}
