package com.programmersbox.mangaworld

import android.content.Context
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate

var Context.showAdult by sharedPrefNotNullDelegate(false)