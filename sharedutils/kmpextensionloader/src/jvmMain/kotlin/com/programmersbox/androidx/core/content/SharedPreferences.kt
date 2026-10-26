package androidx.core.content

import android.content.SharedPreferences

/** core-ktx's `edit { }` (same file name, so the same SharedPreferencesKt facade). */
inline fun SharedPreferences.edit(
    commit: Boolean = false,
    action: SharedPreferences.Editor.() -> Unit,
) {
    val editor = edit()
    action(editor)
    if (commit) editor.commit() else editor.apply()
}
