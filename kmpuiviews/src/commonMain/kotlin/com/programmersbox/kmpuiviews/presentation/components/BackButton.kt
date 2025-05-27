package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavController

@Composable
fun BackButton() {
    val navController = LocalNavController.current
    val navActions = LocalNavActions.current
    IconButton(onClick = { navActions.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
}