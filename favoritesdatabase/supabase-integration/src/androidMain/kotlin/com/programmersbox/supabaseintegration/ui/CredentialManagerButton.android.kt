package com.programmersbox.supabaseintegration.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun CredentialManagerSignInButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { onClick(context) },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Text("Sign in with Credential Manager")
    }
}

@Composable
actual fun RegisterPasskeyButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { onClick(context) },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Key, contentDescription = null)
        Text("Register a Passkey")
    }
}

@Composable
actual fun rememberCredentialManagerContext(): Any? = LocalContext.current
