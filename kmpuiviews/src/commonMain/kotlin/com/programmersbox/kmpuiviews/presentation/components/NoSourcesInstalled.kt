package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExtensionOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoSourcesInstalled(modifier: Modifier = Modifier) {
    val navController = LocalNavActions.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ExtensionOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Button(
                onClick = { navController.extensionList() },
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Cannot find any extensions. Please install some!")
            }
        }
    }
}