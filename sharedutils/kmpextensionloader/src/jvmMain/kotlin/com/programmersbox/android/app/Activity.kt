package android.app

import android.view.ContextThemeWrapper

/**
 * SDK supertype chain (Activity -> ContextThemeWrapper -> ContextWrapper) so code typed against Activity
 * links. Desktop never creates one; abstract because the desktop Context stub is.
 */
abstract class Activity : ContextThemeWrapper()
