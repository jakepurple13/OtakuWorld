package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CredentialManagerSignInButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)

@Composable
expect fun RegisterPasskeyButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)
