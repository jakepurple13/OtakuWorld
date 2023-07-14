package com.programmersbox.extensionloader

import android.graphics.drawable.Drawable
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog

data class SourceInformation(
    val apiService: ApiService,
    val name: String,
    val icon: Drawable?,
    val packageName: String,
    val catalog: ApiServicesCatalog? = null
)