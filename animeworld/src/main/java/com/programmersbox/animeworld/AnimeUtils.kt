package com.programmersbox.animeworld

import android.content.Context
import android.os.Environment
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate

var Context.folderLocation: String by sharedPrefNotNullDelegate(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/AnimeWorld/"
)