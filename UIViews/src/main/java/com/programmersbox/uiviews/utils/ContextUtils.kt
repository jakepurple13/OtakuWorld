package com.programmersbox.uiviews.utils

import android.content.Context
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.models.ApiService

var Context.currentService: ApiService? by sharedPrefObjectDelegate(null)