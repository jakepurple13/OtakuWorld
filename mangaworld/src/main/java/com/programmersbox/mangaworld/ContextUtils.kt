package com.programmersbox.mangaworld

import android.content.Context
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate

var Context.batteryAlertPercent: Float by sharedPrefNotNullDelegate(20f)
