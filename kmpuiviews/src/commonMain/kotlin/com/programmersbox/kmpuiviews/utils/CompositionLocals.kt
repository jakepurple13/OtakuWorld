package com.programmersbox.kmpuiviews.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavHostPadding = staticCompositionLocalOf<PaddingValues> { error("") }
val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController Found!") }
