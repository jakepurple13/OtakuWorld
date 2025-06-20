package com.programmersbox.kmpuiviews.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionRequest(permissionsList: List<String>, content: @Composable () -> Unit) {
    var hasStoragePermission by remember { mutableStateOf(false) }
    val storage = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasStoragePermission = it.all { it.value } }
    val context = LocalContext.current
    SideEffect { storage.launch(permissionsList.toTypedArray()) }
    if (hasStoragePermission) {
        content()
    } else {
        NeedsPermissions {
            context.startActivity(
                Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsPermissions(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier) {
                Text(
                    text = "Please enable permissions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Permissions are required to work",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 4.dp)
                )

                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                ) { Text(text = "Enabled") }
            }
        }
    }
}