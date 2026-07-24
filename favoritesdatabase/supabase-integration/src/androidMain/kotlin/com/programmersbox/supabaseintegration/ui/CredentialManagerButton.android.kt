package com.programmersbox.supabaseintegration.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CredentialManagerSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Text("Sign in with Credential Manager")
    }
}

@Composable
actual fun RegisterPasskeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Key, contentDescription = null)
        Text("Register a Passkey")
    }
}
