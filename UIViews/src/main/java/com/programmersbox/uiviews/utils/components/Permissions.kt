package com.programmersbox.uiviews.utils.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.programmersbox.uiviews.R

@ExperimentalPermissionsApi
@Composable
fun PermissionRequest(permissionsList: List<String>, content: @Composable () -> Unit) {
    val storagePermissions = rememberMultiplePermissionsState(permissionsList)
    val context = LocalContext.current
    SideEffect { storagePermissions.launchMultiplePermissionRequest() }
    if (storagePermissions.allPermissionsGranted) {
        content()
    } else {
        if (storagePermissions.permissions.fastAll { it.status.shouldShowRationale }) {
            NeedsPermissions { storagePermissions.launchMultiplePermissionRequest() }
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
                .padding(5.dp),
            shape = RoundedCornerShape(5.dp)
        ) {
            Column(modifier = Modifier) {
                Text(
                    text = stringResource(R.string.please_enable_permissions),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(R.string.need_permissions_to_work),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 4.dp)
                )

                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 5.dp)
                ) { Text(text = stringResource(R.string.enable)) }
            }
        }
    }
}